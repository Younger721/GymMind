import apiClient from './client'
import type { ApiResponse } from '@/types/api'
export interface TrainingOverview { role:string; since:string; until:string; summary:{workoutCount:number;activeMembers:number;totalMinutes:number;averageMinutes:number;streakDays:number}; records:Array<{id:number;memberId:number;workoutDate:string;duration:number;calories:number;notes:string}>; members:Array<{memberId:number;name:string;workoutCount:number;totalMinutes:number;lastWorkout?:string}> }
export const trainingApi={overview(since?:string,until?:string,memberId?:number){return apiClient.get<any,ApiResponse<TrainingOverview>>('/v1/training/overview',{params:{since,until,memberId}})}}
