import axios from 'axios'
import type { InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken } from './auth'
import router from '@/router'

export interface ApiResponse<T = unknown> {
  code: number
  msg: string
  data: T
  [key: string]: unknown
}

// 创建 axios 实例
const service = axios.create({
  baseURL: '/api',
  timeout: 120000
})

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const headers = config.headers as Record<string, unknown>
    const isToken = headers.isToken === false
    if (getToken() && !isToken) {
      headers['Authorization'] = 'Bearer ' + getToken()
    }
    return config
  },
  error => {
    console.log(error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    // 二进制流下载（PDF / Word / PNG 等），直接返回 blob
    if (response.config.responseType === 'blob') {
      if (response.status !== 200) {
        return response.data.text().then(text => {
          return Promise.reject(new Error(text || '导出失败'))
        })
      }
      // 防止 auth 拦截返回 HTML/JSON 被误当文件下载
      const contentType = (response.headers['content-type'] as string) || ''
      if (contentType.includes('text/html') || contentType.includes('application/json')) {
        return response.data.text().then(text => {
          return Promise.reject(new Error(text || '响应格式异常'))
        })
      }
      return response.data
    }

    const res = response.data

    // 如果返回的状态码不是 200，说明出错
    if (res.code !== 200) {
      // 401: 未登录或 token 过期
      if (res.code === 401) {
        // 如果已经在登录页，不重复跳转
        if (router.currentRoute.value.path === '/login') {
          return Promise.reject(new Error(res.msg || '登录状态已过期'))
        }
        ElMessage.error('登录状态已过期，请重新登录')
        // 清除token
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        router.push('/login')
        return Promise.reject(new Error(res.msg || '登录状态已过期'))
      }
      
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }

    return res
  },
  (error: { message?: string; response?: { status: number; data?: any; config?: any } }) => {
    console.log('err' + error)
    let message = error.message || '网络错误'

    if (error.response) {
      // blob 响应（如 PDF 导出）— 读取实际错误文本
      if (error.response.config?.responseType === 'blob' && error.response.data instanceof Blob) {
        return error.response.data.text().then(text => {
          const errMsg = text || message
          ElMessage.error(errMsg)
          return Promise.reject(new Error(errMsg))
        }).catch(() => {
          ElMessage.error(message)
          return Promise.reject(error)
        })
      }

      switch (error.response.status) {
        case 401:
          message = '未授权，请登录'
          // 如果已经在登录页，不重复跳转
          if (router.currentRoute.value.path === '/login') {
            break
          }
          // 清除token
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          router.push('/login')
          break
        case 403:
          message = '拒绝访问'
          break
        case 404:
          message = '请求地址不存在'
          break
        case 500:
          message = '服务器内部错误'
          break
        default:
          message = error.response.data?.msg || '网络错误'
      }
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

// Create a typed wrapper so callers get Promise<ApiResponse> instead of Promise<AxiosResponse>
function request<T = unknown>(config: Record<string, unknown>): Promise<ApiResponse<T>> {
  return service.request(config) as any
}

export default request
