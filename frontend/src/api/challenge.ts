import request from '@/utils/request'

export interface CreateChallengeRequest {
  name: string
  description: string
  goalType: 'WORKOUT_COUNT' | 'TOTAL_TIME' | 'TOTAL_CALORIES' | 'DISTANCE'
  goalValue: number
  startDate: string
  endDate: string
  imageUrl?: string
  difficulty?: 'EASY' | 'MEDIUM' | 'HARD'
}

export interface ChallengeResponse {
  id: number
  creatorId: number
  name: string
  description: string
  goalType: string
  goalValue: number
  startDate: string
  endDate: string
  status: string
  participantCount: number
  imageUrl?: string
  difficulty?: string
  isParticipating: boolean
  userProgress?: number
  createdAt: string
}

export interface ParticipantResponse {
  userId: number
  username: string
  progress: number
  status: string
  rank: number
  joinedAt: string
}

// 创建挑战
export const createChallenge = (data: CreateChallengeRequest) => {
  return request<ChallengeResponse>({
    url: '/api/challenges',
    method: 'post',
    data
  })
}

// 获取活跃挑战
export const getActiveChallenges = () => {
  return request<ChallengeResponse[]>({
    url: '/api/challenges/active',
    method: 'get'
  })
}

// 获取用户参与的挑战
export const getUserChallenges = () => {
  return request<ChallengeResponse[]>({
    url: '/api/challenges/user',
    method: 'get'
  })
}

// 加入挑战
export const joinChallenge = (challengeId: number) => {
  return request({
    url: `/api/challenges/${challengeId}/join`,
    method: 'post'
  })
}

// 更新进度
export const updateProgress = (challengeId: number, progress: number) => {
  return request({
    url: `/api/challenges/${challengeId}/progress`,
    method: 'put',
    params: { progress }
  })
}

// 获取排行榜
export const getLeaderboard = (challengeId: number) => {
  return request<ParticipantResponse[]>({
    url: `/api/challenges/${challengeId}/leaderboard`,
    method: 'get'
  })
}
