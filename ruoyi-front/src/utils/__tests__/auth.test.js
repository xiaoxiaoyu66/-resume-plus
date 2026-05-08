import { describe, it, expect, beforeEach, vi } from 'vitest'
import { getToken, setToken, removeToken } from '../auth'

vi.mock('js-cookie', () => ({
  default: { get: vi.fn(), set: vi.fn(), remove: vi.fn() },
}))

describe('auth utils', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('getToken should call Cookies.get with Admin-Token', async () => {
    const Cookies = await import('js-cookie')
    Cookies.default.get.mockReturnValue('my-token')
    expect(getToken()).toBe('my-token')
    expect(Cookies.default.get).toHaveBeenCalledWith('Admin-Token')
  })

  it('setToken should call Cookies.set with Admin-Token', async () => {
    const Cookies = await import('js-cookie')
    setToken('new-token')
    expect(Cookies.default.set).toHaveBeenCalledWith('Admin-Token', 'new-token')
  })

  it('removeToken should call Cookies.remove with Admin-Token', async () => {
    const Cookies = await import('js-cookie')
    removeToken()
    expect(Cookies.default.remove).toHaveBeenCalledWith('Admin-Token')
  })
})
