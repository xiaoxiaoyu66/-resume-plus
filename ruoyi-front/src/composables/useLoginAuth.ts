import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { phoneLogin, getCodeImg, sendSmsCode, getWechatQrCode, pollWechatStatus } from '@/api/login'
import QRCode from 'qrcode'

export function useLoginAuth(options) {
  const {
    router,
    userStore,
    loginForm,
    loginFormsRef,
    phoneForm,
    loading,
    loginSuccess,
    inkSpread,
    codeRefreshing,
    showQrModal,
    triggerShake
  } = options

  const codeUrl = ref('')
  const smsCountdown = ref(0)
  const qrCodeUrl = ref('')
  const qrScanned = ref(false)
  const wechatSessionId = ref('')
  let pollTimer = null

  async function getCode() {
    codeRefreshing.value = true
    try {
      const res = await getCodeImg() as any
      if (res.img) {
        codeUrl.value = 'data:image/gif;base64,' + res.img
        loginForm.uuid = res.uuid
      } else if (res.data && res.data.img) {
        codeUrl.value = 'data:image/gif;base64,' + res.data.img
        loginForm.uuid = res.data.uuid
      }
    } catch (e) {
      console.error('获取验证码失败', e)
    } finally {
      setTimeout(() => {
        codeRefreshing.value = false
      }, 300)
    }
  }

  async function handleLogin() {
    const valid = await loginFormsRef.value?.validateLogin()?.catch(() => false)
    if (!valid) {
      triggerShake()
      return
    }

    loading.value = true
    try {
      await userStore.login(loginForm)
      await userStore.getInfo()
      loginSuccess.value = true
      inkSpread.value = true
      setTimeout(() => {
        ElMessage.success('登录成功')
        router.push('/chat')
      }, 800)
    } catch (e) {
      triggerShake()
      getCode()
    } finally {
      loading.value = false
    }
  }

  async function sendSms() {
    const valid = await loginFormsRef.value?.validatePhoneField?.('phone')?.catch(() => false)
    if (!valid) return

    try {
      await sendSmsCode(phoneForm.phone)
      ElMessage.success('验证码已发送')
      smsCountdown.value = 60
      const timer = setInterval(() => {
        smsCountdown.value--
        if (smsCountdown.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    } catch (e) {
      console.error('发送验证码失败', e)
    }
  }

  async function handlePhoneLogin() {
    const valid = await loginFormsRef.value?.validatePhone()?.catch(() => false)
    if (!valid) {
      triggerShake()
      return
    }

    loading.value = true
    try {
      const res = await phoneLogin(phoneForm.phone, phoneForm.code) as any
      console.log('登录响应:', res)

      let token
      if (res.token) {
        token = res.token
      } else if (res.data && res.data.token) {
        token = res.data.token
      } else {
        console.error('无法获取token，响应数据:', res)
        ElMessage.error('登录响应格式错误')
        return
      }

      console.log('获取到token:', token)
      const { setToken } = await import('@/utils/auth')
      setToken(token)
      userStore.token = token
      localStorage.setItem('token', token)

      try {
        await userStore.getInfo()
        console.log('获取用户信息成功')
      } catch (infoError) {
        console.error('获取用户信息失败:', infoError)
      }

      console.log('准备跳转...')
      loginSuccess.value = true
      inkSpread.value = true
      ElMessage.success('登录成功')
      console.log('跳转到/chat')
      router.push('/chat')
    } catch (e) {
      triggerShake()
      console.error('登录失败', e)
      ElMessage.error(e.message || '登录失败')
    } finally {
      loading.value = false
    }
  }

  async function getWechatCode() {
    try {
      const res = await getWechatQrCode() as any
      const data = res.sessionId ? res : res.data
      wechatSessionId.value = data.sessionId
      qrCodeUrl.value = await QRCode.toDataURL(data.qrUrl)
      startPolling()
    } catch (e) {
      console.error('获取微信二维码失败', e)
    }
  }

  function startPolling() {
    pollTimer = setInterval(async () => {
      try {
        const res = await pollWechatStatus(wechatSessionId.value) as any
        const data = res.status ? res : res.data
        if (data.status === 'scanned') {
          qrScanned.value = true
        }
        if (data.status === 'success') {
          clearInterval(pollTimer)
          const { token } = data
          userStore.token = token
          localStorage.setItem('token', token)
          await userStore.getInfo()
          loginSuccess.value = true
          inkSpread.value = true
          setTimeout(() => {
            ElMessage.success('登录成功')
            router.push('/chat')
          }, 800)
        }
      } catch (e) {
        console.error('轮询失败', e)
      }
    }, 2000)
  }

  function openWechatLogin() {
    showQrModal.value = true
    qrCodeUrl.value = ''
    qrScanned.value = false
    getWechatCode()
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  function closeWechatLogin() {
    showQrModal.value = false
    stopPolling()
  }

  return {
    codeUrl,
    smsCountdown,
    qrCodeUrl,
    qrScanned,
    getCode,
    handleLogin,
    sendSms,
    handlePhoneLogin,
    getWechatCode,
    openWechatLogin,
    closeWechatLogin,
    stopPolling
  }
}

