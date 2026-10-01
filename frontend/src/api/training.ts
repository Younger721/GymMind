import apiClient from './client'
import type { ApiResponse } from '@/types/api'
export interface TrainingOverview { role:string; since:string; until:string; summary:{workoutCount:number;activeMembers:number;totalMinutes:number;averageMinutes:number;streakDays:number}; records:Array<{id:number;memberId:number;workoutDate:string;duration:number;calories:number;notes:string}>; members:Array<{memberId:number;name:string;workoutCount:number;totalMinutes:number;lastWorkout?:string}> }
export interface TrainingTemplateItem { id:number; exerciseId:number; phase:string; sortOrder:number; sets:number; reps:number; restSeconds:number; weight:number; notes?:string }
export interface TrainingTemplate { id:number; name:string; category:string; goal:string; description:string; coachId?:number; versionNumber:number; status:string; items?:TrainingTemplateItem[] }
export interface MemberPlan { id:number; name:string; templateId:number; templateVersion:number }
export interface TrainingSchedule { id:number; memberPlanId:number; memberId:number; scheduledDate:string }
export const trainingApi={
 overview(since?:string,until?:string,memberId?:number){return apiClient.get<any,ApiResponse<TrainingOverview>>('/v1/training/overview',{params:{since,until,memberId}})},
 templates(){return apiClient.get<any,ApiResponse<TrainingTemplate[]>>('/v1/training/plans')},
 template(id:number){return apiClient.get<any,ApiResponse<TrainingTemplate>>(`/v1/training/plans/${id}`)},
 select(templateId:number){return apiClient.post<any,ApiResponse<MemberPlan>>('/v1/training/member-plans',null,{params:{templateId}})},
 memberPlans(){return apiClient.get<any,ApiResponse<MemberPlan[]>>('/v1/training/member-plans')},
 calendar(from:string,to:string){return apiClient.get<any,ApiResponse<TrainingSchedule[]>>('/v1/training/calendar',{params:{from,to}})},
 schedule(memberPlanId:number,date:string){return apiClient.post<any,ApiResponse<TrainingSchedule>>('/v1/training/calendar/schedules',null,{params:{memberPlanId,date}})},
 publish(id:number){return apiClient.post<any,ApiResponse<TrainingTemplate>>(`/v1/training/plans/${id}/publish`)}
}
