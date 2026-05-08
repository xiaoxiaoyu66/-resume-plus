import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { ref } from 'vue'
import { useLoginAuth } from '../useLoginAuth'

vi.mock('@/api/login', () => ({
  getCodeImg: vi.fn(),
  phoneLogin: vi.fn(),
  sendSmsCode: vi.fn(),
  getWechatQrCode: vi.fn(),
  pollWechatStatus: vi.fn(),
}))

vi.mock('@/utils/auth', () => ({
  setToken: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
  },
}))

vi.mock('qrcode', () => ({
  default: { toDataURL: vi.fn() },
}))

describe('useLoginAuth', () => {
  let router, userStore, loginForm, loginFormsRef, phoneForm, loading,
      loginSuccess, inkSpread, codeRefreshing, showQrModal, triggerShake
  let auth

  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()

    router = { push: vi.fn() }
    userStore = { login: vi.fn(), getInfo: vi.fn(), token: '' }
    loginForm = { username: '', password: '', code: '', uuid: '' }
    loginFormsRef = { value: null }
    phoneForm = { phone: '', code: '' }
    loading = ref(false)
    loginSuccess = ref(false)
    inkSpread = ref(false)
    codeRefreshing = ref(false)
    showQrModal = ref(false)
    triggerShake = vi.fn()

    auth = useLoginAuth({
      router, userStore, loginForm, loginFormsRef, phoneForm,
      loading, loginSuccess, inkSpread, codeRefreshing, showQrModal, triggerShake,
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('getCode', () => {
    it('should fetch captcha and set codeUrl from direct response', async () => {
      const { getCodeImg } = await import('@/api/login')
      getCodeImg.mockResolvedValue({ img: 'base64data', uuid: 'uuid-123' })

      await auth.getCode()

      expect(codeRefreshing.value).toBe(true)
      expect(auth.codeUrl.value).toBe('data:image/gif;base64,base64data')
      expect(loginForm.uuid).toBe('uuid-123')
    })

    it('should handle nested data response', async () => {
      const { getCodeImg } = await import('@/api/login')
      getCodeImg.mockResolvedValue({ data: { img: 'nested', uuid: 'uuid-nested' } })

      await auth.getCode()

      expect(auth.codeUrl.value).toBe('data:image/gif;base64,nested')
      expect(loginForm.uuid).toBe('uuid-nested')
    })

    it('should handle API error gracefully', async () => {
      const { getCodeImg } = await import('@/api/login')
      getCodeImg.mockRejectedValue(new Error('Network error'))

      await expect(auth.getCode()).resolves.toBeUndefined()
      expect(codeRefreshing.value).toBe(true)
    })

    it('should reset codeRefreshing after timeout', async () => {
      vi.useFakeTimers()
      const { getCodeImg } = await import('@/api/login')
      getCodeImg.mockResolvedValue({ img: 'data', uuid: 'uuid' })

      auth.getCode()
      await vi.advanceTimersByTimeAsync(10)
      await vi.advanceTimersByTimeAsync(300)

      expect(codeRefreshing.value).toBe(false)
    })
  })

  describe('handleLogin', () => {
    it('should successfully login and redirect', async () => {
      vi.useFakeTimers()
      loginFormsRef.value = { validateLogin: vi.fn().mockResolvedValue(true) }
      userStore.login.mockResolvedValue({ token: 'my-token' })
      userStore.getInfo.mockResolvedValue()

      await auth.handleLogin()

      expect(userStore.login).toHaveBeenCalledWith(loginForm)
      expect(userStore.getInfo).toHaveBeenCalled()
      expect(loginSuccess.value).toBe(true)
      expect(inkSpread.value).toBe(true)
      expect(loading.value).toBe(false)

      await vi.advanceTimersByTimeAsync(800)

      const { ElMessage } = await import('element-plus')
      expect(ElMessage.success).toHaveBeenCalledWith('登录成功')
      expect(router.push).toHaveBeenCalledWith('/chat')
    })

    it('should shake on validation failure', async () => {
      loginFormsRef.value = { validateLogin: vi.fn().mockResolvedValue(false) }

      await auth.handleLogin()

      expect(triggerShake).toHaveBeenCalled()
      expect(userStore.login).not.toHaveBeenCalled()
    })

    it('should shake and refresh captcha on login failure', async () => {
      vi.useFakeTimers()
      const { getCodeImg } = await import('@/api/login')
      getCodeImg.mockResolvedValue({ img: 'new-captcha', uuid: 'new-uuid' })
      loginFormsRef.value = { validateLogin: vi.fn().mockResolvedValue(true) }
      userStore.login.mockRejectedValue(new Error('Invalid credentials'))

      await auth.handleLogin()

      expect(triggerShake).toHaveBeenCalled()
      expect(getCodeImg).toHaveBeenCalled()
      expect(loading.value).toBe(false)
    })

    it('should handle validateLogin rejection', async () => {
      loginFormsRef.value = {
        validateLogin: vi.fn().mockRejectedValue(new Error('Validation error')),
      }

      await auth.handleLogin()

      expect(triggerShake).toHaveBeenCalled()
      expect(userStore.login).not.toHaveBeenCalled()
    })
  })

  describe('sendSms', () => {
    it('should send SMS and start countdown', async () => {
      vi.useFakeTimers()
      const { sendSmsCode } = await import('@/api/login')
      sendSmsCode.mockResolvedValue({})
      loginFormsRef.value = { validatePhoneField: vi.fn().mockResolvedValue(true) }
      phoneForm.phone = '13800138000'

      await auth.sendSms()

      expect(sendSmsCode).toHaveBeenCalledWith('13800138000')
      const { ElMessage } = await import('element-plus')
      expect(ElMessage.success).toHaveBeenCalledWith('验证码已发送')
      expect(auth.smsCountdown.value).toBe(60)

      await vi.advanceTimersByTimeAsync(5000)
      expect(auth.smsCountdown.value).toBe(55)

      await vi.advanceTimersByTimeAsync(55000)
      expect(auth.smsCountdown.value).toBe(0)
    })

    it('should do nothing on validation failure', async () => {
      const { sendSmsCode } = await import('@/api/login')
      loginFormsRef.value = { validatePhoneField: vi.fn().mockResolvedValue(false) }

      await auth.sendSms()

      expect(sendSmsCode).not.toHaveBeenCalled()
    })

    it('should handle validatePhoneField rejection', async () => {
      const { sendSmsCode } = await import('@/api/login')
      loginFormsRef.value = {
        validatePhoneField: vi.fn().mockRejectedValue(new Error('Invalid')),
      }

      await auth.sendSms()

      expect(sendSmsCode).not.toHaveBeenCalled()
    })
  })

  describe('handlePhoneLogin', () => {
    beforeEach(() => {
      loginFormsRef.value = { validatePhone: vi.fn().mockResolvedValue(true) }
      phoneForm.phone = '13800138000'
      phoneForm.code = '123456'
    })

    it('should login with token in direct response', async () => {
      const { phoneLogin } = await import('@/api/login')
      const { setToken } = await import('@/utils/auth')
      phoneLogin.mockResolvedValue({ token: 'phone-token' })
      userStore.getInfo.mockResolvedValue()

      await auth.handlePhoneLogin()

      expect(phoneLogin).toHaveBeenCalledWith('13800138000', '123456')
      expect(setToken).toHaveBeenCalledWith('phone-token')
      expect(userStore.token).toBe('phone-token')
      expect(localStorage.getItem('token')).toBe('phone-token')
      expect(userStore.getInfo).toHaveBeenCalled()
      expect(loginSuccess.value).toBe(true)
      expect(inkSpread.value).toBe(true)
      const { ElMessage } = await import('element-plus')
      expect(ElMessage.success).toHaveBeenCalledWith('登录成功')
      expect(router.push).toHaveBeenCalledWith('/chat')
      expect(loading.value).toBe(false)
    })

    it('should extract token from nested data response', async () => {
      const { phoneLogin } = await import('@/api/login')
      const { setToken } = await import('@/utils/auth')
      phoneLogin.mockResolvedValue({ data: { token: 'nested-token' } })
      userStore.getInfo.mockResolvedValue()

      await auth.handlePhoneLogin()

      expect(setToken).toHaveBeenCalledWith('nested-token')
      expect(userStore.token).toBe('nested-token')
    })

    it('should show error when no token in response', async () => {
      const { phoneLogin } = await import('@/api/login')
      const { ElMessage } = await import('element-plus')
      phoneLogin.mockResolvedValue({})

      await auth.handlePhoneLogin()

      expect(ElMessage.error).toHaveBeenCalledWith('登录响应格式错误')
      expect(router.push).not.toHaveBeenCalled()
    })

    it('should shake on validation failure', async () => {
      loginFormsRef.value.validatePhone = vi.fn().mockResolvedValue(false)

      await auth.handlePhoneLogin()

      expect(triggerShake).toHaveBeenCalled()
      expect(router.push).not.toHaveBeenCalled()
      expect(loading.value).toBe(false)
    })

    it('should handle API error', async () => {
      const { phoneLogin } = await import('@/api/login')
      const { ElMessage } = await import('element-plus')
      phoneLogin.mockRejectedValue(new Error('Phone login failed'))

      await auth.handlePhoneLogin()

      expect(triggerShake).toHaveBeenCalled()
      expect(ElMessage.error).toHaveBeenCalledWith('Phone login failed')
      expect(loading.value).toBe(false)
    })

    it('should continue even if getInfo fails', async () => {
      const { phoneLogin } = await import('@/api/login')
      phoneLogin.mockResolvedValue({ token: 'phone-token' })
      userStore.getInfo.mockRejectedValue(new Error('Info fetch failed'))

      await auth.handlePhoneLogin()

      expect(loginSuccess.value).toBe(true)
      expect(router.push).toHaveBeenCalledWith('/chat')
    })
  })

  describe('wechat login', () => {
    it('should set modal state when opening', () => {
      auth.openWechatLogin()

      expect(showQrModal.value).toBe(true)
      expect(auth.qrCodeUrl.value).toBe('')
      expect(auth.qrScanned.value).toBe(false)
    })

    it('should fetch QR code', async () => {
      const { getWechatQrCode } = await import('@/api/login')
      const QRCode = await import('qrcode')
      getWechatQrCode.mockResolvedValue({ sessionId: 'sess-1', qrUrl: 'https://wechat.com/qr' })
      QRCode.default.toDataURL.mockResolvedValue('data:image/png;base64,qr-data')

      await auth.getWechatCode()

      expect(getWechatQrCode).toHaveBeenCalled()
      expect(auth.qrCodeUrl.value).toBe('data:image/png;base64,qr-data')
    })

    it('should close wechat modal and stop polling', () => {
      auth.closeWechatLogin()
      expect(showQrModal.value).toBe(false)
    })
  })

  describe('polling', () => {
    beforeEach(() => { vi.useFakeTimers() })

    it('should poll wechat status and detect scanned', async () => {
      const { getWechatQrCode, pollWechatStatus } = await import('@/api/login')
      const QRCode = await import('qrcode')
      getWechatQrCode.mockResolvedValue({ sessionId: 'sess-1', qrUrl: 'https://wechat.com/qr' })
      QRCode.default.toDataURL.mockResolvedValue('data:image/png;base64,qr')
      pollWechatStatus.mockResolvedValue({ status: 'pending' })

      auth.openWechatLogin()
      await vi.advanceTimersByTimeAsync(10)

      pollWechatStatus.mockResolvedValue({ status: 'scanned' })
      await vi.advanceTimersByTimeAsync(2000)

      expect(pollWechatStatus).toHaveBeenCalledWith('sess-1')
      expect(auth.qrScanned.value).toBe(true)
    })

    it('should complete login on success status', async () => {
      const { getWechatQrCode, pollWechatStatus } = await import('@/api/login')
      const QRCode = await import('qrcode')
      getWechatQrCode.mockResolvedValue({ sessionId: 'sess-2', qrUrl: 'https://wechat.com/qr' })
      QRCode.default.toDataURL.mockResolvedValue('data:image/png;base64,qr')
      pollWechatStatus.mockResolvedValue({ status: 'pending' })

      auth.openWechatLogin()
      await vi.advanceTimersByTimeAsync(10)

      pollWechatStatus.mockResolvedValue({ status: 'success', token: 'wechat-token' })
      userStore.getInfo.mockResolvedValue()
      await vi.advanceTimersByTimeAsync(2000)

      expect(userStore.token).toBe('wechat-token')
      expect(localStorage.getItem('token')).toBe('wechat-token')
      expect(userStore.getInfo).toHaveBeenCalled()
      expect(loginSuccess.value).toBe(true)
      expect(inkSpread.value).toBe(true)

      await vi.advanceTimersByTimeAsync(800)
      const { ElMessage } = await import('element-plus')
      expect(ElMessage.success).toHaveBeenCalledWith('登录成功')
      expect(router.push).toHaveBeenCalledWith('/chat')
    })

    it('should handle nested poll response', async () => {
      const { getWechatQrCode, pollWechatStatus } = await import('@/api/login')
      const QRCode = await import('qrcode')
      getWechatQrCode.mockResolvedValue({ sessionId: 'sess-3', qrUrl: 'https://wechat.com/qr' })
      QRCode.default.toDataURL.mockResolvedValue('data:image/png;base64,qr')
      pollWechatStatus.mockResolvedValue({ data: { status: 'scanned' } })

      auth.openWechatLogin()
      await vi.advanceTimersByTimeAsync(10)
      await vi.advanceTimersByTimeAsync(2000)

      expect(auth.qrScanned.value).toBe(true)
    })
  })

  describe('stopPolling', () => {
    it('should not throw when no active timer', () => {
      expect(() => auth.stopPolling()).not.toThrow()
    })
  })
})
