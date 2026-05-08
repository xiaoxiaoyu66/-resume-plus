import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '../user'

vi.mock('@/api/login', () => ({
  login: vi.fn(),
  getInfo: vi.fn(),
  logout: vi.fn(),
}))

vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(() => ''),
  setToken: vi.fn(),
  removeToken: vi.fn(),
}))

describe('user store', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useUserStore()
  })

  describe('isLoggedIn getter', () => {
    it('should return false when token is empty', () => {
      expect(store.isLoggedIn).toBe(false)
    })

    it('should return true when token exists', () => {
      store.token = 'valid-token'
      expect(store.isLoggedIn).toBe(true)
    })
  })

  describe('login', () => {
    it('should set token on successful login', async () => {
      const { login } = await import('@/api/login')
      const { setToken } = await import('@/utils/auth')
      login.mockResolvedValue({ token: 'my-token' })

      await store.login({ username: 'admin', password: 'admin123', code: '', uuid: '' })

      expect(store.token).toBe('my-token')
      expect(setToken).toHaveBeenCalledWith('my-token')
    })

    it('should handle nested data response', async () => {
      const { login } = await import('@/api/login')
      login.mockResolvedValue({ data: { token: 'nested-token' } })

      await store.login({ username: 'admin', password: 'admin123', code: '', uuid: '' })

      expect(store.token).toBe('nested-token')
    })

    it('should reject on API failure', async () => {
      const { login } = await import('@/api/login')
      login.mockRejectedValue(new Error('Invalid credentials'))

      await expect(store.login({ username: 'bad', password: 'wrong', code: '', uuid: '' }))
        .rejects.toThrow('Invalid credentials')
      expect(store.token).toBe('')
    })
  })

  describe('getInfo', () => {
    const mockUser = {
      user: {
        userName: 'TestUser',
        avatar: 'avatar.png',
        phonenumber: '13800138000',
        email: 'test@test.com',
        createTime: '2026-01-01',
      },
      roles: ['admin'],
      permissions: ['*:*:*'],
    }

    it('should populate user info on success', async () => {
      const { getInfo } = await import('@/api/login')
      getInfo.mockResolvedValue({ user: mockUser.user, roles: mockUser.roles, permissions: mockUser.permissions })

      await store.getInfo()

      expect(store.name).toBe('TestUser')
      expect(store.avatar).toBe('avatar.png')
      expect(store.phonenumber).toBe('13800138000')
      expect(store.email).toBe('test@test.com')
      expect(store.createTime).toBe('2026-01-01')
      expect(store.roles).toEqual(['admin'])
      expect(store.permissions).toEqual(['*:*:*'])
    })

    it('should handle nested data wrapper', async () => {
      const { getInfo } = await import('@/api/login')
      getInfo.mockResolvedValue({
        data: {
          user: mockUser.user,
          roles: mockUser.roles,
          permissions: mockUser.permissions,
        }
      })

      await store.getInfo()
      expect(store.name).toBe('TestUser')
    })

    it('should reject on API failure', async () => {
      const { getInfo } = await import('@/api/login')
      getInfo.mockRejectedValue(new Error('Network error'))

      await expect(store.getInfo()).rejects.toThrow('Network error')
    })

    it('should handle missing avatar gracefully', async () => {
      const { getInfo } = await import('@/api/login')
      getInfo.mockResolvedValue({
        user: { userName: 'NoAvatar', avatar: null },
        roles: [], permissions: []
      })

      await store.getInfo()
      expect(store.avatar).toBe('')
    })
  })

  describe('logout', () => {
    it('should call logout API and clear state', async () => {
      const { logout } = await import('@/api/login')
      const { removeToken } = await import('@/utils/auth')
      logout.mockResolvedValue({})

      // setup some state
      store.token = 'old-token'
      store.name = 'User'

      await store.logout()

      expect(store.token).toBe('')
      expect(store.name).toBe('')
      expect(store.avatar).toBe('')
      expect(store.roles).toEqual([])
      expect(store.permissions).toEqual([])
      expect(removeToken).toHaveBeenCalled()
    })

    it('should clear state even if API fails', async () => {
      const { logout } = await import('@/api/login')
      const { removeToken } = await import('@/utils/auth')
      logout.mockRejectedValue(new Error('Network error'))

      store.token = 'old-token'
      store.name = 'User'

      await expect(store.logout()).rejects.toThrow('Network error')

      // state should still be cleared
      expect(store.token).toBe('')
      expect(store.name).toBe('')
      expect(removeToken).toHaveBeenCalled()
    })
  })

  describe('resetToken', () => {
    it('should clear token and call removeToken', async () => {
      const { removeToken } = await import('@/utils/auth')
      store.token = 'some-token'

      store.resetToken()

      expect(store.token).toBe('')
      expect(removeToken).toHaveBeenCalled()
    })
  })
})
