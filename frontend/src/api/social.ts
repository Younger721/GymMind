import request from '@/utils/request'

export interface CreatePostRequest {
  content: string
  imageUrls?: string[]
  postType: 'WORKOUT' | 'PROGRESS' | 'MEAL' | 'GENERAL'
  metadata?: Record<string, any>
}

export interface PostResponse {
  id: number
  userId: number
  userName: string
  userAvatar?: string
  content: string
  imageUrls: string[]
  postType: string
  metadata?: Record<string, any>
  likes: number
  comments: number
  isLiked: boolean
  createdAt: string
}

export interface CommentResponse {
  id: number
  postId: number
  userId: number
  userName: string
  userAvatar?: string
  content: string
  createdAt: string
}

export interface CreateCommentRequest {
  content: string
}

// 创建动态
export const createPost = (data: CreatePostRequest) => {
  return request<PostResponse>({
    url: '/api/social/posts',
    method: 'post',
    data
  })
}

// 获取动态流（时间线）
export const getPostFeed = (page: number = 0, size: number = 10) => {
  return request<{ content: PostResponse[], totalElements: number, totalPages: number }>({
    url: '/api/social/posts/feed',
    method: 'get',
    params: { page, size }
  })
}

// 获取热门动态
export const getTrendingPosts = (hours: number = 24, page: number = 0, size: number = 10) => {
  return request<{ content: PostResponse[], totalElements: number, totalPages: number }>({
    url: '/api/social/posts/trending',
    method: 'get',
    params: { hours, page, size }
  })
}

// 获取用户动态
export const getUserPosts = (userId: number, page: number = 0, size: number = 10) => {
  return request<{ content: PostResponse[], totalElements: number, totalPages: number }>({
    url: `/api/social/posts/user/${userId}`,
    method: 'get',
    params: { page, size }
  })
}

// 点赞动态
export const likePost = (postId: number) => {
  return request({
    url: `/api/social/posts/${postId}/like`,
    method: 'post'
  })
}

// 取消点赞
export const unlikePost = (postId: number) => {
  return request({
    url: `/api/social/posts/${postId}/like`,
    method: 'delete'
  })
}

// 获取评论列表
export const getComments = (postId: number) => {
  return request<CommentResponse[]>({
    url: `/api/social/posts/${postId}/comments`,
    method: 'get'
  })
}

// 添加评论
export const addComment = (postId: number, data: CreateCommentRequest) => {
  return request<CommentResponse>({
    url: `/api/social/posts/${postId}/comments`,
    method: 'post',
    data
  })
}

// 删除动态
export const deletePost = (postId: number) => {
  return request({
    url: `/api/social/posts/${postId}`,
    method: 'delete'
  })
}
