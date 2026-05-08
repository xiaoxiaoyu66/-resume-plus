<template>
  <div class="login-page">
    <!-- 动态水墨山水背景 -->
    <InkLandscapeBg />

    <!-- 毛笔装饰 -->

    <div class="login-container" :class="{ 'shake': shakeAnimation, 'ink-spread': inkSpread }" ref="loginContainer">
      <!-- 水墨涟漪效果 -->
      <div class="ripple-container" ref="rippleContainer"></div>

      <LoginHeader />

      <LoginForms
        ref="loginFormsRef"
        v-model="loginType"
        :login-form="loginForm"
        :login-rules="loginRules"
        :phone-form="phoneForm"
        :phone-rules="phoneRules"
        :code-url="codeUrl"
        :sms-countdown="smsCountdown"
        :loading="loading"
        :login-success="loginSuccess"
        :qr-code-url="qrCodeUrl"
        :qr-scanned="qrScanned"
        :code-refreshing="codeRefreshing"
        @login="handleLogin"
        @phone-login="handlePhoneLogin"
        @send-sms="sendSms"
        @get-code="getCode"
      />

      <div class="login-footer">
        <div class="divider">
          <span class="line line-left"></span>
          <span class="text">墨韵</span>
          <span class="line line-right"></span>
        </div>
        <p class="footer-text">
          <span v-for="(char, index) in footerChars" :key="index" class="footer-char" :style="{ animationDelay: index * 0.1 + 's' }">{{ char }}</span>
        </p>
      </div>

      <!-- 微信扫码角标 - 登录框右下角 -->
      <div class="wechat-corner" @click="openWechatLogin" @mouseenter="wechatCornerHover = true" @mouseleave="wechatCornerHover = false">
        <div class="corner-bg" :class="{ 'hover': wechatCornerHover }">
          <svg class="qr-icon" viewBox="0 0 24 24" fill="currentColor">
            <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.047c.134 0 .24-.111.24-.247 0-.06-.023-.12-.038-.177l-.327-1.233a.582.582 0 0 1-.023-.156.49.49 0 0 1 .201-.398C23.024 18.48 24 16.82 24 14.98c0-3.21-2.931-5.837-6.656-6.088V8.89c-.135-.01-.27-.027-.407-.03zm-2.53 3.274c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.97-.982zm4.844 0c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.969-.982z"/>
          </svg>
          <div class="corner-tooltip" :class="{ 'show': wechatCornerHover }">微信扫码登录</div>
        </div>
      </div>
    </div>

    <!-- 水墨扩散层 -->
    <div class="ink-diffusion" :class="{ 'active': inkSpread }">
      <div class="diffusion-ring ring-1"></div>
      <div class="diffusion-ring ring-2"></div>
      <div class="diffusion-ring ring-3"></div>
    </div>

    <!-- 二维码弹出模态框 -->
    <div v-if="showQrModal" class="qr-modal" @click="closeWechatLogin">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>微信扫码登录</h3>
          <button class="close-btn" @click="closeWechatLogin">
            <el-icon><Close /></el-icon>
          </button>
        </div>
        <div class="modal-body">
          <div class="qr-large-frame">
            <div class="qr-ink-border"></div>
            <div v-if="!qrCodeUrl" class="qr-loading">
              <div class="ink-loading">
                <div class="brush-stroke"></div>
                <div class="brush-stroke"></div>
                <div class="brush-stroke"></div>
              </div>
              <p>正在研墨...</p>
            </div>
            <img v-else :src="qrCodeUrl" alt="微信扫码" class="qr-large">
            <div class="qr-corner tl"></div>
            <div class="qr-corner tr"></div>
            <div class="qr-corner bl"></div>
            <div class="qr-corner br"></div>
          </div>
          <p class="qr-modal-tip">请使用微信扫一扫登录</p>
        </div>
        <div class="modal-footer">
          <div class="seal-mark">简历+</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Close } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import InkLandscapeBg from '@/components/InkLandscapeBg.vue'
import LoginHeader from '@/components/LoginHeader.vue'
import LoginForms from '@/components/LoginForms.vue'
import { useLoginAuth } from '@/composables/useLoginAuth'

const router = useRouter()
const userStore = useUserStore()

// Canvas引用
const loginContainer = ref(null)
const rippleContainer = ref(null)

const codeRefreshing = ref(false)
const inkSpread = ref(false)
const loginSuccess = ref(false)
const wechatCornerHover = ref(false)
const showQrModal = ref(false)

// 登录类型
const loginType = ref('account')
const loginFormsRef = ref(null)

// 抖动动画
const shakeAnimation = ref(false)
function triggerShake() {
  shakeAnimation.value = true
  setTimeout(() => {
    shakeAnimation.value = false
  }, 500)
}

// 创建水墨涟漪
function createRipple(e) {
  const ripple = document.createElement('div')
  ripple.className = 'ink-ripple-effect'
  const rect = loginContainer.value.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  ripple.style.left = x + 'px'
  ripple.style.top = y + 'px'
  rippleContainer.value.appendChild(ripple)
  setTimeout(() => ripple.remove(), 1000)
}

// 底部文字动画
const footerText = '笔墨纸砚 · 传承千年'
const footerChars = computed(() => footerText.split(''))

const loading = ref(false)

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

const phoneForm = reactive({
  phone: '',
  code: ''
})

const phoneRules = {
  phone: [
    { required: true, trigger: 'blur', message: '请输入手机号' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
  ],
  code: [{ required: true, trigger: 'blur', message: '请输入验证码' }]
}

const {
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
} = useLoginAuth({
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
})


// 监听标签切换
watch(loginType, (newType) => {
  if (newType === 'wechat' && !qrCodeUrl.value) {
    getWechatCode()
  }
})

onMounted(() => {
  getCode()
  if (loginType.value === 'wechat') {
    getWechatCode()
  }
  // 添加点击涟漪效果
  if (loginContainer.value) {
    loginContainer.value.addEventListener('click', createRipple)
  }
})

onUnmounted(() => {
  stopPolling()
  if (loginContainer.value) {
    loginContainer.value.removeEventListener('click', createRipple)
  }
})
</script>

<style scoped lang="scss">
// 水墨黑主题色
$ink-black: #0a0a0a;
$ink-deep: #141414;
$ink-mid: #1f1f1f;
$ink-light: #2d2d2d;
$ink-pale: #404040;
$paper-white: #f8f8f6;
$paper-cream: #f0ede8;
$accent-red: #8b1a1a;
$gold: #c9a961;

.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, $ink-deep 0%, $ink-black 50%, #0d0d0d 100%);
  position: relative;
  overflow: hidden;
}




// 登录容器
.login-container {
  width: 100%;
  max-width: 480px;
  background: rgba(252, 252, 250, 0.98);
  border-radius: 24px;
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.3),
    0 0 0 1px rgba(255, 255, 255, 0.5);
  padding: 56px 48px;
  position: relative;
  z-index: 10;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;

  // 宣纸纹理
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background-image: 
      url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)' opacity='0.03'/%3E%3C/svg%3E");
    pointer-events: none;
    z-index: 0;
  }

  // 顶部装饰线
  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 2px;
    background: linear-gradient(90deg, transparent 0%, rgba(0,0,0,0.1) 20%, rgba(0,0,0,0.1) 80%, transparent 100%);
    opacity: 0.6;
  }

  &.shake {
    animation: ink-shake 0.5s ease;
  }

  &.ink-spread {
    transform: scale(0.96) translateY(-12px);
    opacity: 0.85;
    transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  }
}

@keyframes ink-shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-6px); }
  20%, 40%, 60%, 80% { transform: translateX(6px); }
}

// 涟漪容器
.ripple-container {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  border-radius: 4px;
  z-index: 0;
}

:deep(.ink-ripple-effect) {
  position: absolute;
  width: 20px;
  height: 20px;
  background: radial-gradient(circle, rgba(0,0,0,0.1) 0%, transparent 70%);
  border-radius: 50%;
  transform: translate(-50%, -50%) scale(0);
  animation: ripple-spread 1s ease-out forwards;
  pointer-events: none;
}

@keyframes ripple-spread {
  to {
    transform: translate(-50%, -50%) scale(20);
    opacity: 0;
  }
}



// 底部
.login-footer {
  margin-top: 36px;
  text-align: center;
  position: relative;
  z-index: 1;

  .divider {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;

    .line {
      flex: 1;
      height: 1px;
      background: linear-gradient(90deg, transparent, rgba(0,0,0,0.15), transparent);
      position: relative;
      overflow: hidden;

      &::after {
        content: '';
        position: absolute;
        top: 0;
        left: -100%;
        width: 100%;
        height: 100%;
        background: linear-gradient(90deg, transparent, rgba(0,0,0,0.3), transparent);
        animation: line-shimmer 3s infinite;
      }

      &.line-right::after {
        animation-delay: 1.5s;
      }
    }

    .text {
      font-size: 12px;
      color: $ink-pale;
      font-family: 'Noto Serif SC', 'STSong', serif;
      letter-spacing: 6px;
    }
  }

  .footer-text {
    font-size: 11px;
    color: $ink-pale;
    font-family: 'Noto Serif SC', 'STSong', serif;
    letter-spacing: 3px;
    opacity: 0.7;

    .footer-char {
      display: inline-block;
      opacity: 0;
      animation: footer-char-in 0.5s ease forwards;
    }
  }
}

@keyframes line-shimmer {
  to { left: 100%; }
}

@keyframes footer-char-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 水墨扩散效果
.ink-diffusion {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;

  &.active {
    opacity: 1;

    .diffusion-ring {
      animation: diffuse-out 1s cubic-bezier(0.4, 0, 0.2, 1) forwards;
    }
  }

  .diffusion-ring {
    position: absolute;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(10,10,10,0.9) 0%, rgba(20,20,20,0.8) 40%, transparent 70%);
    transform: scale(0);

    &.ring-1 {
      width: 200vmax;
      height: 200vmax;
      animation-delay: 0s;
    }

    &.ring-2 {
      width: 200vmax;
      height: 200vmax;
      animation-delay: 0.1s;
      opacity: 0.7;
    }

    &.ring-3 {
      width: 200vmax;
      height: 200vmax;
      animation-delay: 0.2s;
      opacity: 0.5;
    }
  }
}

@keyframes diffuse-out {
  to {
    transform: scale(1);
  }
}

@keyframes brush-paint {
  0%, 100% { transform: scaleX(0); opacity: 0; }
  50% { transform: scaleX(1); opacity: 1; }
}

// 微信扫码角标 - 登录框右下角（水墨色）
.wechat-corner {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 60px;
  height: 60px;
  z-index: 30;
  cursor: pointer;
  overflow: hidden;

  .corner-bg {
    position: absolute;
    right: 0;
    bottom: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(315deg, $ink-mid 50%, transparent 50%);
    transition: all 0.3s ease;

    &.hover {
      width: 110%;
      height: 110%;
      background: linear-gradient(315deg, $ink-black 50%, transparent 50%);
    }

    .qr-icon {
      position: absolute;
      right: 10px;
      bottom: 10px;
      width: 22px;
      height: 22px;
      color: white;
      filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3));
      transition: all 0.3s ease;
    }

    &:hover .qr-icon {
      transform: scale(1.1);
    }

    .corner-tooltip {
      position: absolute;
      right: 8px;
      bottom: 45px;
      font-size: 11px;
      color: $ink-black;
      background: rgba(248, 248, 246, 0.95);
      border: 1px solid rgba(0, 0, 0, 0.1);
      padding: 4px 8px;
      border-radius: 4px;
      white-space: nowrap;
      opacity: 0;
      transform: translateY(10px);
      transition: all 0.3s ease;
      pointer-events: none;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);

      &.show {
        opacity: 1;
        transform: translateY(0);
      }

      &::before {
        content: '';
        position: absolute;
        right: 10px;
        bottom: -4px;
        border-left: 4px solid transparent;
        border-right: 4px solid transparent;
        border-top: 4px solid rgba(248, 248, 246, 0.95);
      }
    }
  }
}

// 二维码弹出模态框
.qr-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: modal-fade-in 0.3s ease;

  .modal-content {
    background: rgba(252, 252, 250, 0.98);
    border-radius: 20px;
    padding: 32px;
    width: 90%;
    max-width: 400px;
    box-shadow: 
      0 25px 80px rgba(0, 0, 0, 0.3),
      0 0 0 1px rgba(255, 255, 255, 0.5);
    position: relative;
    animation: modal-scale-in 0.4s cubic-bezier(0.4, 0, 0.2, 1);

    &::before {
      content: '';
      position: absolute;
      inset: 0;
      background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)' opacity='0.03'/%3E%3C/svg%3E");
      border-radius: 20px;
      pointer-events: none;
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      position: relative;
      z-index: 2;

      h3 {
        font-size: 20px;
        font-weight: 600;
        color: $ink-black;
        font-family: 'Noto Serif SC', 'STSong', serif;
      }

      .close-btn {
        width: 36px;
        height: 36px;
        border: none;
        background: transparent;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        color: $ink-pale;
        transition: all 0.3s ease;

        &:hover {
          background: rgba(0, 0, 0, 0.05);
          color: $ink-black;
        }

        .el-icon {
          font-size: 20px;
        }
      }
    }

    .modal-body {
      display: flex;
      flex-direction: column;
      align-items: center;
      position: relative;
      z-index: 2;

      .qr-large-frame {
        position: relative;
        padding: 20px;
        background: white;
        border-radius: 12px;
        box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
        margin-bottom: 20px;

        .qr-ink-border {
          position: absolute;
          inset: 12px;
          border: 1px solid rgba(0, 0, 0, 0.06);
          border-radius: 8px;
          pointer-events: none;
        }

        .qr-loading {
          width: 200px;
          height: 200px;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;

          .ink-loading {
            width: 60px;
            height: 60px;
            position: relative;
            margin-bottom: 16px;

            .brush-stroke {
              position: absolute;
              background: $ink-mid;
              border-radius: 2px;
              animation: brush-paint 1.5s ease-in-out infinite;

              &:nth-child(1) {
                width: 40px;
                height: 4px;
                top: 20px;
                left: 10px;
                animation-delay: 0s;
              }

              &:nth-child(2) {
                width: 30px;
                height: 4px;
                top: 30px;
                left: 15px;
                animation-delay: 0.2s;
              }

              &:nth-child(3) {
                width: 35px;
                height: 4px;
                top: 40px;
                left: 12px;
                animation-delay: 0.4s;
              }
            }
          }

          p {
            color: $ink-pale;
            font-family: 'Noto Serif SC', 'STSong', serif;
            font-size: 14px;
          }
        }

        .qr-large {
          width: 200px;
          height: 200px;
          display: block;
        }

        .qr-corner {
          position: absolute;
          width: 20px;
          height: 20px;
          border: 3px solid $ink-black;
          transition: all 0.3s ease;

          &.tl { top: 12px; left: 12px; border-right: none; border-bottom: none; }
          &.tr { top: 12px; right: 12px; border-left: none; border-bottom: none; }
          &.bl { bottom: 12px; left: 12px; border-right: none; border-top: none; }
          &.br { bottom: 12px; right: 12px; border-left: none; border-top: none; }
        }
      }

      .qr-modal-tip {
        font-size: 14px;
        color: $ink-mid;
        font-family: 'Noto Serif SC', 'STSong', serif;
      }
    }

    .modal-footer {
      margin-top: 24px;
      display: flex;
      justify-content: center;
      position: relative;
      z-index: 2;

      .seal-mark {
        width: 48px;
        height: 48px;
        border: 2px solid $accent-red;
        color: $accent-red;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 14px;
        font-weight: bold;
        font-family: 'Noto Serif SC', 'STSong', serif;
        transform: rotate(-8deg);
        opacity: 0.8;
        border-radius: 4px;
      }
    }
  }
}

@keyframes modal-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes modal-scale-in {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

// 平板端适配
@media (max-width: 768px) {
  .login-container {
    padding: 44px 36px;
  }
}

// 移动端适配
@media (max-width: 480px) {
  .login-container {
    max-width: 100%;
    margin: 0 12px;
    padding: 28px 20px;
    border-radius: 16px;
  }

  .wechat-corner { width: 52px; height: 52px; }

  .wechat-corner .corner-bg .qr-icon {
    right: 10px;
    bottom: 10px;
    width: 20px;
    height: 20px;
  }
}
</style>
