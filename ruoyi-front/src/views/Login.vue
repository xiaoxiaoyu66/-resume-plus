<template>
  <div class="login-page">
    <InkLandscapeBg />

    <div
      class="login-container"
      :class="{ shake: shakeAnimation, 'ink-spread': inkSpread }"
      ref="loginContainerRef"
    >
      <InkRippleLayer ref="rippleRef" />

      <BrushCalligraphy :skip-animation="visitedBefore" />

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

      <LoginFooter />

      <WechatCorner @open="openWechatLogin" />
    </div>

    <InkDiffusionOverlay :active="inkSpread" />

    <WechatQrModal
      :visible="showQrModal"
      :qr-code-url="qrCodeUrl"
      @close="closeWechatLogin"
    />

    <SealStamp
      :visible="loginSuccess"
      text="简"
      @done="() => {}"
    />

    <InkCursorTrail />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import InkLandscapeBg from '@/components/InkLandscapeBg.vue'
import LoginForms from '@/components/LoginForms.vue'
import { useLoginForm } from '@/composables/useLoginForm'
import { useLoginAuth } from '@/composables/useLoginAuth'

// 拆出的小组件
import BrushCalligraphy from '@/components/login/BrushCalligraphy.vue'
import LoginFooter from '@/components/login/LoginFooter.vue'
import InkRippleLayer from '@/components/login/InkRippleLayer.vue'
import InkDiffusionOverlay from '@/components/login/InkDiffusionOverlay.vue'
import WechatCorner from '@/components/login/WechatCorner.vue'
import WechatQrModal from '@/components/login/WechatQrModal.vue'
import SealStamp from '@/components/login/SealStamp.vue'
import InkCursorTrail from '@/components/login/InkCursorTrail.vue'

const router = useRouter()
const userStore = useUserStore()
const loginContainerRef = ref<HTMLElement | null>(null)
const rippleRef = ref<InstanceType<typeof InkRippleLayer> | null>(null)

// 表单状态
const {
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
} = useLoginForm()

// 登录逻辑
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
  stopPolling,
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
  triggerShake,
})

// 跳过首次访问动画
const visitedBefore = ref(false)

// 微信标签切换
watch(loginType, (type) => {
  if (type === 'wechat' && !qrCodeUrl.value) getWechatCode()
})

onMounted(() => {
  // 记录访问（跳过动画用）
  const visited = sessionStorage.getItem('resume_visited')
  if (visited) visitedBefore.value = true
  sessionStorage.setItem('resume_visited', 'true')

  getCode()
  if (loginType.value === 'wechat') getWechatCode()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped lang="scss">
$ink-black: #0a0a0a;
$ink-deep: #141414;
$ink-mid: #1f1f1f;
$ink-pale: #404040;

.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, $ink-deep 0%, $ink-black 50%, #0d0d0d 100%);
  position: relative;
  overflow: hidden;
}

.login-container {
  width: 100%;
  max-width: 480px;
  background: rgba(252, 252, 250, 0.98);
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.5);
  padding: 56px 48px;
  position: relative;
  z-index: 10;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)' opacity='0.03'/%3E%3C/svg%3E");
    pointer-events: none;
    z-index: 0;
  }

  &::after {
    content: '';
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 2px;
    background: linear-gradient(90deg, transparent 0%, rgba(0,0,0,0.1) 20%, rgba(0,0,0,0.1) 80%, transparent 100%);
    opacity: 0.6;
  }

  &.shake { animation: ink-shake 0.5s ease; }
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
</style>
