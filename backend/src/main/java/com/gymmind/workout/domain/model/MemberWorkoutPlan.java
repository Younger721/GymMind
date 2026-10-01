package com.gymmind.workout.domain.model;
import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*;
@Entity @Table(name="member_workout_plan")
public class MemberWorkoutPlan extends TenantScopedEntity {
 @Column(name="member_id",nullable=false) private Long memberId; @Column(name="template_id",nullable=false) private Long templateId; @Column(nullable=false,length=128) private String name; @Column(nullable=false) private int templateVersion;
 protected MemberWorkoutPlan(){} public MemberWorkoutPlan(Long tenantId,Long memberId,Long templateId,String name,int version){super(tenantId);this.memberId=memberId;this.templateId=templateId;this.name=name;this.templateVersion=version;}
 public Long getMemberId(){return memberId;} public Long getTemplateId(){return templateId;} public String getName(){return name;} public int getTemplateVersion(){return templateVersion;}
}
