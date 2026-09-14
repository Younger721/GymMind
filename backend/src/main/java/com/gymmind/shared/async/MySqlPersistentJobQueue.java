package com.gymmind.shared.async;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** MySQL-backed queue. Claim uses SKIP LOCKED so workers never wait on each other. */
@Component
@ConditionalOnProperty(name = "gymmind.job-queue", havingValue = "mysql")
public class MySqlPersistentJobQueue implements PersistentJobQueue {
    private final JdbcTemplate jdbc;
    public MySqlPersistentJobQueue(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override @Transactional
    public Long enqueue(String type, Long tenantId) {
        validate(type, tenantId);
        jdbc.update("insert into async_job(type,tenant_id,status,attempts,next_attempt_at) values(?,?,?,0,?)",
                type.trim(), tenantId, JobStatus.PENDING.name(), Timestamp.from(Instant.EPOCH));
        return jdbc.queryForObject("select last_insert_id()", Long.class);
    }

    @Override @Transactional
    public Optional<PersistentJob> claimNext(Instant now, Duration lease) {
        if (now == null || lease == null || lease.isNegative() || lease.isZero()) throw new IllegalArgumentException("invalid lease");
        List<PersistentJob> rows = jdbc.query("select id,type,tenant_id,status,attempts,next_attempt_at,lease_until,last_error from async_job where status=? and next_attempt_at<=? order by id limit 1 for update skip locked",
                (rs, n) -> row(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4), rs.getInt(5), rs.getTimestamp(6), rs.getTimestamp(7), rs.getString(8)),
                JobStatus.PENDING.name(), Timestamp.from(now));
        if (rows.isEmpty()) return Optional.empty();
        PersistentJob job = rows.get(0);
        jdbc.update("update async_job set status=?,lease_until=? where id=?", JobStatus.RUNNING.name(), Timestamp.from(now.plus(lease)), job.id());
        return Optional.of(new PersistentJob(job.id(), job.type(), job.tenantId(), JobStatus.RUNNING, job.attempts(), job.nextAttemptAt(), now.plus(lease), job.lastError()));
    }

    public void advance(Long id) { updateStatus(id, JobStatus.RUNNING); }
    public void retry(Long id, String error, Instant now) { PersistentJob j = find(id); int attempts=j.attempts()+1; JobStatus s=attempts>=5?JobStatus.FAILED:JobStatus.PENDING; jdbc.update("update async_job set status=?,attempts=?,next_attempt_at=?,lease_until=null,last_error=? where id=?",s.name(),attempts,Timestamp.from(now.plusSeconds(30L << (attempts-1))),error,id); }
    public void complete(Long id) { jdbc.update("update async_job set status=?,lease_until=null where id=?", JobStatus.COMPLETED.name(), id); }
    public void recoverExpired(Instant now) { jdbc.update("update async_job set status=?,lease_until=null where status=? and lease_until<=?", JobStatus.PENDING.name(), JobStatus.RUNNING.name(), Timestamp.from(now)); }
    public PersistentJob find(Long id) { return jdbc.queryForObject("select id,type,tenant_id,status,attempts,next_attempt_at,lease_until,last_error from async_job where id=?", (rs,n)->row(rs.getLong(1),rs.getString(2),rs.getLong(3),rs.getString(4),rs.getInt(5),rs.getTimestamp(6),rs.getTimestamp(7),rs.getString(8)), id); }
    private void updateStatus(Long id, JobStatus status) { if (jdbc.update("update async_job set status=? where id=?",status.name(),id)==0) throw new NoSuchElementException("job"); }
    private static PersistentJob row(long id,String type,long tenant,String status,int attempts,Timestamp next,Timestamp lease,String error){return new PersistentJob(id,type,tenant,JobStatus.valueOf(status),attempts,next==null?null:next.toInstant(),lease==null?null:lease.toInstant(),error);}
    private static void validate(String type,Long tenant){if(type==null||type.isBlank()||tenant==null||tenant<=0)throw new IllegalArgumentException("invalid job");}
}
