/**
 * 登录表单状态与校验规则
 */
import { reactive, ref } from 'vue'

export function useLoginForm() {
  const loginType = ref('account')
  const loading = ref(false)
  const inkSpread = ref(false)
  const loginSuccess = ref(false)
  const codeRefreshing = ref(false)
  const shakeAnimation = ref(false)
  const wechatCornerHover = ref(false)
  const showQrModal = ref(false)
  const loginFormsRef = ref<InstanceType<any> | null>(null)

  function triggerShake() {
    shakeAnimation.value = true
    setTimeout(() => { shakeAnimation.value = false }, 500)
  }

  const loginForm = reactive({
    username: 'admin',
    password: 'admin123',
    code: '',
    uuid: ''
  })

  const loginRules = {
    username: [{ required: true, trigger: 'blur', message: '请输入用户名' }],
    password: [{ required: true, trigger: 'blur', message: '请输入密码' }],
    code: [{ required: true, trigger: 'blur', message: '请输入验证码' }]
  }

  const phoneForm = reactive({ phone: '', code: '' })

  const phoneRules = {
    phone: [
      { required: true, trigger: 'blur', message: '请输入手机号' },
      { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
    ],
    code: [{ required: true, trigger: 'blur', message: '请输入验证码' }]
  }

  return {
    loginType,
    loading,
    inkSpread,
    loginSuccess,
    codeRefreshing,
    shakeAnimation,
    wechatCornerHover,
    showQrModal,
    loginFormsRef,
    loginForm,
    loginRules,
    phoneForm,
    phoneRules,
    triggerShake,
  }
}
