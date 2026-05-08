import { describe, it, expect, beforeAll, beforeEach, vi } from 'vitest'

const { mockAxiosInstance, mockAxiosRequest } = vi.hoisted(() => {
  const req = vi.fn()
  const inst = {
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    request: req,
  }
  return { mockAxiosInstance: inst, mockAxiosRequest: req }
})

vi.mock('axios', () => ({
  default: { create: vi.fn(() => mockAxiosInstance) },
}))

const routerPush = vi.fn()
const routerCurrentRoute = { value: { path: '/chat' } }

vi.mock('@/router', () => ({
  default: { currentRoute: routerCurrentRoute, push: routerPush },
}))

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn() },
}))

vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(),
}))

let requestHandler, responseHandler, responseErrorHandler

describe('request utils', () => {
  beforeAll(async () => {
    await import('../request')
    requestHandler = mockAxiosInstance.interceptors.request.use.mock.calls[0][0]
    responseHandler = mockAxiosInstance.interceptors.response.use.mock.calls[0][0]
    responseErrorHandler = mockAxiosInstance.interceptors.response.use.mock.calls[0][1]
  })

  beforeEach(() => {
    vi.resetAllMocks()
    routerCurrentRoute.value = { path: '/chat' }
  })

  describe('request interceptor', () => {
    it('should add Authorization header when token exists', async () => {
      const { getToken } = await import('@/utils/auth')
      getToken.mockReturnValue('my-jwt-token')

      const config = await requestHandler({ headers: {} })

      expect(config.headers['Authorization']).toBe('Bearer my-jwt-token')
    })

    it('should not add Authorization when isToken is false', async () => {
      const { getToken } = await import('@/utils/auth')
      getToken.mockReturnValue('my-jwt-token')

      const config = await requestHandler({ headers: { isToken: false } })

      expect(config.headers['Authorization']).toBeUndefined()
    })

    it('should not add Authorization when no token', async () => {
      const config = await requestHandler({ headers: {} })

      expect(config.headers['Authorization']).toBeUndefined()
    })
  })

  describe('response interceptor — success', () => {
    it('should return data when code is 200', async () => {
      const res = {
        config: { responseType: 'json' },
        data: { code: 200, data: { id: 1 }, msg: 'success' },
      }

      const result = await responseHandler(res)

      expect(result).toEqual({ code: 200, data: { id: 1 }, msg: 'success' })
    })

    it('should return blob data for blob response with PDF content-type', async () => {
      const blobData = new Blob(['pdf-content'], { type: 'application/pdf' })
      const res = {
        config: { responseType: 'blob' },
        status: 200,
        headers: { 'content-type': 'application/pdf' },
        data: blobData,
      }

      const result = await responseHandler(res)

      expect(result).toBe(blobData)
    })

    it('should reject when blob response status is not 200', async () => {
      const res = {
        config: { responseType: 'blob' },
        status: 500,
        headers: { 'content-type': 'application/pdf' },
        data: new Blob(['Error text'], { type: 'text/plain' }),
      }

      await expect(responseHandler(res)).rejects.toThrow('Error text')
    })

    it('should reject when blob response content-type is not pdf', async () => {
      const res = {
        config: { responseType: 'blob' },
        status: 200,
        headers: { 'content-type': 'application/json' },
        data: new Blob(['invalid response'], { type: 'text/plain' }),
      }

      await expect(responseHandler(res)).rejects.toThrow('invalid response')
    })

    it('should reject with fallback message when blob response is not pdf and body is empty', async () => {
      const res = {
        config: { responseType: 'blob' },
        status: 200,
        headers: { 'content-type': 'application/json' },
        data: new Blob([''], { type: 'text/plain' }),
      }

      await expect(responseHandler(res)).rejects.toThrow('响应格式异常')
    })

    it('should show error and reject when code is not 200', async () => {
      const { ElMessage } = await import('element-plus')
      const res = {
        config: { responseType: 'json' },
        data: { code: 500, msg: '服务器错误' },
      }

      await expect(responseHandler(res)).rejects.toThrow('服务器错误')
      expect(ElMessage.error).toHaveBeenCalledWith('服务器错误')
    })

    it('should use default message when msg is empty', async () => {
      const { ElMessage } = await import('element-plus')
      const res = {
        config: { responseType: 'json' },
        data: { code: 500, msg: '' },
      }

      await expect(responseHandler(res)).rejects.toThrow('请求失败')
      expect(ElMessage.error).toHaveBeenCalledWith('请求失败')
    })

    describe('401 handling', () => {
      beforeEach(() => {
        localStorage.setItem('token', 'expired-token')
        localStorage.setItem('userInfo', '{"name":"test"}')
      })

      it('should redirect to login and clear storage on 401', async () => {
        const { ElMessage } = await import('element-plus')
        const res = {
          config: { responseType: 'json' },
          data: { code: 401, msg: '登录状态已过期' },
        }

        await expect(responseHandler(res)).rejects.toThrow('登录状态已过期')
        expect(ElMessage.error).toHaveBeenCalledWith('登录状态已过期，请重新登录')
        expect(localStorage.getItem('token')).toBeNull()
        expect(localStorage.getItem('userInfo')).toBeNull()
        expect(routerPush).toHaveBeenCalledWith('/login')
      })

      it('should not redirect when already on login page', async () => {
        routerCurrentRoute.value = { path: '/login' }
        const res = {
          config: { responseType: 'json' },
          data: { code: 401, msg: '登录状态已过期' },
        }

        await expect(responseHandler(res)).rejects.toThrow('登录状态已过期')
        expect(routerPush).not.toHaveBeenCalled()
      })
    })
  })

  describe('response interceptor — error', () => {
    it('should handle 500 error with appropriate message', async () => {
      const { ElMessage } = await import('element-plus')
      const error = {
        message: 'Request failed',
        response: { status: 500, data: {} },
      }

      await expect(responseErrorHandler(error)).rejects.toThrow('Request failed')
      expect(ElMessage.error).toHaveBeenCalledWith('服务器内部错误')
    })

    it('should handle 401 error and redirect to login', async () => {
      const { ElMessage } = await import('element-plus')
      localStorage.setItem('token', 't')
      const error = { message: 'Unauthorized', response: { status: 401 } }

      await expect(responseErrorHandler(error)).rejects.toThrow('Unauthorized')
      expect(ElMessage.error).toHaveBeenCalledWith('未授权，请登录')
      expect(localStorage.getItem('token')).toBeNull()
      expect(routerPush).toHaveBeenCalledWith('/login')
    })

    it('should not redirect on 401 when already on login page', async () => {
      routerCurrentRoute.value = { path: '/login' }
      const error = { message: 'Unauthorized', response: { status: 401, data: {} } }

      await expect(responseErrorHandler(error)).rejects.toThrow('Unauthorized')
      expect(routerPush).not.toHaveBeenCalled()
    })

    it('should handle 403 error', async () => {
      const { ElMessage } = await import('element-plus')
      const error = { message: 'Forbidden', response: { status: 403, data: {} } }

      await expect(responseErrorHandler(error)).rejects.toThrow('Forbidden')
      expect(ElMessage.error).toHaveBeenCalledWith('拒绝访问')
    })

    it('should handle 404 error', async () => {
      const { ElMessage } = await import('element-plus')
      const error = { message: 'Not Found', response: { status: 404, data: {} } }

      await expect(responseErrorHandler(error)).rejects.toThrow('Not Found')
      expect(ElMessage.error).toHaveBeenCalledWith('请求地址不存在')
    })

    it('should handle 500 error', async () => {
      const { ElMessage } = await import('element-plus')
      const error = { message: 'Server Error', response: { status: 500, data: {} } }

      await expect(responseErrorHandler(error)).rejects.toThrow('Server Error')
      expect(ElMessage.error).toHaveBeenCalledWith('服务器内部错误')
    })

    it('should use custom error message from response data', async () => {
      const { ElMessage } = await import('element-plus')
      const error = {
        message: 'Bad Request',
        response: { status: 400, data: { msg: '参数错误' } },
      }

      await expect(responseErrorHandler(error)).rejects.toThrow('Bad Request')
      expect(ElMessage.error).toHaveBeenCalledWith('参数错误')
    })

    it('should handle network error with no response', async () => {
      const { ElMessage } = await import('element-plus')
      const error = { message: 'Network Error' }

      await expect(responseErrorHandler(error)).rejects.toThrow('Network Error')
      expect(ElMessage.error).toHaveBeenCalledWith('Network Error')
    })
  })
})
