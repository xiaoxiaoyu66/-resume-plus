import request from '@/utils/request'

// 登录方法（账号密码）
export function login(username: string, password: string, code: string, uuid: string) {
  const data = {
    username,
    password,
    code,
    uuid
  }
  return request({
    url: '/login',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: data
  })
}

// 手机验证码登录
export function phoneLogin(phone: string, code: string) {
  return request({
    url: '/login/phone',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: { phone, code }
  })
}

// 微信扫码登录 - 获取二维码
export function getWechatQrCode() {
  return request({
    url: '/login/wechat/qrcode',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'get'
  })
}

// 微信扫码登录 - 轮询状态
export function pollWechatStatus(sessionId: string) {
  return request({
    url: '/login/wechat/poll',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'get',
    params: { sessionId }
  })
}

// 发送短信验证码
export function sendSmsCode(phone: string) {
  return request({
    url: '/login/sendSms',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: { phone }
  })
}

// 注册方法
export function register(data) {
  return request({
    url: '/register',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 获取用户详细信息
export function getInfo() {
  return request({
    url: '/getInfo',
    method: 'get'
  })
}

// 退出方法
export function logout() {
  return request({
    url: '/logout',
    method: 'post'
  })
}

// 获取验证码
export function getCodeImg() {
  return request({
    url: '/captchaImage',
    headers: {
      isToken: false
    },
    method: 'get',
    timeout: 20000
  })
}
