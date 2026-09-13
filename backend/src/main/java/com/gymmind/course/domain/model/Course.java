package com.gymmind.course.domain.model;
import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="course_schedule")
public class Course extends TenantScopedEntity {
    @Column(name="coach_id", nullable=false) private Long coachId;
    @Column(nullable=false, length=128) private String title;
    @Column(name="course_type", nullable=false, length=32) private String type;
    @Column(name="starts_at", nullable=false) private Instant startsAt;
    @Column(name="ends_at", nullable=false) private Instant endsAt;
    @Column(nullable=false) private int capacity;
    @Column(name="reserved_count", nullable=false) private int reservedCount;
    @Column(nullable=false, length=128) private String location;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=16) private CourseStatus status=CourseStatus.SCHEDULED;
    protected Course() {}
    private Course(Long tenantId, Long coachId, String title, String type, Instant startsAt, Instant endsAt, int capacity, String location) {
        super(tenantId); if (coachId==null||coachId<=0||title==null||title.isBlank()||type==null||type.isBlank()||startsAt==null||endsAt==null||!endsAt.isAfter(startsAt)||capacity<=0||location==null||location.isBlank()) throw new IllegalArgumentException("Invalid course");
        this.coachId=coachId; this.title=title.trim(); this.type=type.trim(); this.startsAt=startsAt; this.endsAt=endsAt; this.capacity=capacity; this.location=location.trim();
    }
    public static Course create(Long tenantId, Long coachId, String title, String type, Instant startsAt, Instant endsAt, int capacity, String location) { return new Course(tenantId,coachId,title,type,startsAt,endsAt,capacity,location); }
    public void update(String title,String type,Instant startsAt,Instant endsAt,int capacity,String location) { if (Instant.now().isAfter(this.startsAt)) throw new IllegalStateException("Started course cannot change"); if(capacity<reservedCount||capacity<=0||endsAt==null||startsAt==null||!endsAt.isAfter(startsAt)) throw new IllegalArgumentException("Invalid course update"); this.title=title;this.type=type;this.startsAt=startsAt;this.endsAt=endsAt;this.capacity=capacity;this.location=location; }
    public void cancel(){ if(status==CourseStatus.COMPLETED) throw new IllegalStateException("Completed course cannot cancel"); status=CourseStatus.CANCELLED; }
    public void reserve(){ if(reservedCount>=capacity) throw new IllegalStateException("Course is full"); reservedCount++; }
    public void release(){ if(reservedCount>0) reservedCount--; }
    public Long getCoachId(){return coachId;} public String getTitle(){return title;} public String getType(){return type;} public Instant getStartsAt(){return startsAt;} public Instant getEndsAt(){return endsAt;} public int getCapacity(){return capacity;} public int getReservedCount(){return reservedCount;} public String getLocation(){return location;} public CourseStatus getStatus(){return status;}
}
