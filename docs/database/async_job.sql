create table if not exists async_job (
  id bigint primary key auto_increment,
  type varchar(64) not null,
  tenant_id bigint not null,
  status varchar(16) not null,
  attempts int not null default 0,
  next_attempt_at timestamp(6) not null,
  lease_until timestamp(6) null,
  last_error varchar(1000) null,
  created_at timestamp(6) not null default current_timestamp(6),
  updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
  index idx_async_job_claim(status,next_attempt_at,id),
  index idx_async_job_tenant(tenant_id)
);
