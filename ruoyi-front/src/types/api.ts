export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
  token?: string
  [key: string]: unknown
}

export interface ApiResult<T = unknown> {
  data?: T
  [key: string]: unknown
}
