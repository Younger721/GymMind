/** 与后端 ApiResponse 对齐 */
export interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
  traceId?: string
}
