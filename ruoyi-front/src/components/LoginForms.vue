<template>
  <!-- 标签页 -->
  <div class="login-tabs">
    <div
      v-for="tab in tabs" :key="tab.key"
      :class="['tab-item', { active: modelValue === tab.key }]"
      @click="switchTab(tab.key)"
    >
      <span class="tab-text">{{ tab.label }}</span>
      <span v-if="modelValue === tab.key" class="tab-ink"></span>
      <div class="tab-brush" :class="{ 'active': modelValue === tab.key }"></div>
    </div>
  </div>

  <Transition name="form-fade" mode="out-in">
    <!-- 账号密码登录 -->
    <div v-if="modelValue === 'account'" key="account" class="login-form">
      <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules">
        <el-form-item prop="username">
          <div class="input-wrapper" :class="{ 'focused': focusedInput === 'username' }">
            <div class="input-ink-bg"></div>
            <el-input
              v-model="loginForm.username"
              placeholder="用户名" size="large"
              :prefix-icon="UserIcon" class="ink-input"
              @focus="focusedInput = 'username'"
              @blur="focusedInput = ''"
            />
            <span class="input-line"></span>
            <span class="input-ink-drop"></span>
          </div>
        </el-form-item>
        <el-form-item prop="password">
          <div class="input-wrapper" :class="{ 'focused': focusedInput === 'password' }">
            <div class="input-ink-bg"></div>
            <el-input
              v-model="loginForm.password"
              type="password" placeholder="密码" size="large"
              :prefix-icon="Lock" class="ink-input"
              @keyup.enter="$emit('login')"
              @focus="focusedInput = 'password'"
              @blur="focusedInput = ''"
            />
            <span class="input-line"></span>
            <span class="input-ink-drop"></span>
          </div>
        </el-form-item>
        <el-form-item prop="code" class="code-item">
          <div class="input-wrapper code-wrapper" :class="{ 'focused': focusedInput === 'code' }">
            <div class="input-ink-bg"></div>
            <el-input
              v-model="loginForm.code"
              placeholder="验证码" size="large"
              :prefix-icon="CircleCheck" class="ink-input"
              @keyup.enter="$emit('login')"
              @focus="focusedInput = 'code'"
              @blur="focusedInput = ''"
            />
            <span class="input-line"></span>
            <span class="input-ink-drop"></span>
          </div>
          <div class="code-img" :class="{ 'refreshing': codeRefreshing }" @click="$emit('get-code')">
            <img v-if="codeUrl" :src="codeUrl" alt="验证码">
            <div v-else class="code-loading">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
            </div>
            <div class="code-ink-overlay"></div>
          </div>
        </el-form-item>
        <el-form-item>
          <button
            type="button" class="login-btn"
            :class="{ 'loading': loading, 'success': loginSuccess }"
            :disabled="loading"
            @click="$emit('login')"
            @mouseenter="btnHover = true"
            @mouseleave="btnHover = false"
          >
            <span v-if="!loading && !loginSuccess" class="btn-text" :class="{ 'hover': btnHover }">登 录</span>
            <span v-else-if="loading" class="btn-loading">
              <span class="ink-ripple"></span>
              <span class="loading-text">登录中</span>
            </span>
            <span v-else class="btn-success">
              <span class="success-icon">✓</span>
              <span>成功</span>
            </span>
            <div class="btn-seal-mark"></div>
            <div class="btn-ink-effect" :class="{ 'active': btnHover }"></div>
          </button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 手机验证码登录 -->
    <div v-else-if="modelValue === 'phone'" key="phone" class="login-form">
      <el-form ref="phoneFormRef" :model="phoneForm" :rules="phoneRules">
        <el-form-item prop="phone">
          <div class="input-wrapper" :class="{ 'focused': focusedInput === 'phone' }">
            <div class="input-ink-bg"></div>
            <el-input
              v-model="phoneForm.phone"
              placeholder="手机号" size="large"
              :prefix-icon="Phone" class="ink-input"
              @focus="focusedInput = 'phone'"
              @blur="focusedInput = ''"
            />
            <span class="input-line"></span>
            <span class="input-ink-drop"></span>
          </div>
        </el-form-item>
        <el-form-item prop="code" class="code-item">
          <div class="input-wrapper code-wrapper" :class="{ 'focused': focusedInput === 'phoneCode' }">
            <div class="input-ink-bg"></div>
            <el-input
              v-model="phoneForm.code"
              placeholder="验证码" size="large"
              :prefix-icon="Message" class="ink-input"
              @keyup.enter="$emit('phone-login')"
              @focus="focusedInput = 'phoneCode'"
              @blur="focusedInput = ''"
            />
            <span class="input-line"></span>
            <span class="input-ink-drop"></span>
          </div>
          <button
            type="button" class="sms-btn"
            :class="{ 'counting': smsCountdown > 0 }"
            :disabled="smsCountdown > 0"
            @click="$emit('send-sms')"
          >
            <span v-if="smsCountdown > 0" class="countdown">{{ smsCountdown }}s</span>
            <span v-else class="sms-text">获取验证码</span>
            <div class="sms-ink"></div>
          </button>
        </el-form-item>
        <el-form-item>
          <button
            type="button" class="login-btn"
            :class="{ 'loading': loading, 'success': loginSuccess }"
            :disabled="loading"
            @click="$emit('phone-login')"
            @mouseenter="btnHover = true"
            @mouseleave="btnHover = false"
          >
            <span v-if="!loading && !loginSuccess" class="btn-text" :class="{ 'hover': btnHover }">登 录</span>
            <span v-else-if="loading" class="btn-loading">
              <span class="ink-ripple"></span>
              <span class="loading-text">登录中</span>
            </span>
            <span v-else class="btn-success">
              <span class="success-icon">✓</span>
              <span>成功</span>
            </span>
            <div class="btn-ink-effect" :class="{ 'active': btnHover }"></div>
          </button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 微信扫码登录 -->
    <div v-else key="wechat" class="login-form wechat-login">
    <div v-if="!qrCodeUrl" class="loading-qr">
      <div class="ink-loading">
        <div class="ink-brush-animate">
          <div class="brush-stroke stroke-1"></div>
          <div class="brush-stroke stroke-2"></div>
          <div class="brush-stroke stroke-3"></div>
        </div>
        <p class="loading-text">正在研墨</p>
        <div class="loading-dots">
          <span></span><span></span><span></span>
        </div>
      </div>
    </div>
    <div v-else class="qr-container">
      <div class="qr-frame" :class="{ 'scanned': qrScanned }">
        <div class="qr-ink-border"></div>
        <img :src="qrCodeUrl" alt="微信扫码" class="qr-code">
        <div class="qr-corner tl"></div>
        <div class="qr-corner tr"></div>
        <div class="qr-corner bl"></div>
        <div class="qr-corner br"></div>
        <div class="qr-scan-line"></div>
      </div>
      <p class="qr-tip">
        <span class="ink-text" :class="{ 'scanned': qrScanned }">
          {{ qrScanned ? '扫码成功，确认登录' : '请使用微信扫一扫登录' }}
        </span>
      </p>
    </div>
  </div>
</Transition>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { User as UserIcon, Lock, CircleCheck, Phone, Message } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: String, default: 'account' },
  loginForm: { type: Object, required: true },
  loginRules: { type: Object, required: true },
  phoneForm: { type: Object, required: true },
  phoneRules: { type: Object, required: true },
  codeUrl: { type: String, default: '' },
  smsCountdown: { type: Number, default: 0 },
  loading: { type: Boolean, default: false },
  loginSuccess: { type: Boolean, default: false },
  qrCodeUrl: { type: String, default: '' },
  qrScanned: { type: Boolean, default: false },
  codeRefreshing: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'login', 'phone-login', 'send-sms', 'get-code'])

const tabs = [
  { key: 'account', label: '账号登录' },
  { key: 'phone', label: '手机登录' }
]

const focusedInput = ref('')
const btnHover = ref(false)
const loginFormRef = ref(null)
const phoneFormRef = ref(null)

function switchTab(type) {
  if (props.modelValue === type) return
  emit('update:modelValue', type)
}

defineExpose({
  validateLogin: () => loginFormRef.value?.validate(),
  validatePhone: () => phoneFormRef.value?.validate(),
  validatePhoneField: (field) => phoneFormRef.value?.validateField(field)
})
</script>

<style scoped lang="scss">
$ink-black: #0a0a0a;
$ink-deep: #141414;
$ink-mid: #1f1f1f;
$ink-light: #2d2d2d;
$ink-pale: #404040;
$paper-cream: #f0ede8;
$accent-red: #8b1a1a;
$sans: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;

// 标签页
.login-tabs {
  display: flex;
  margin-bottom: 32px;
  border-bottom: 1px solid rgba(0,0,0,0.06);
  position: relative;
  z-index: 1;

  .tab-item {
    flex: 1;
    text-align: center;
    padding: 16px 0;
    font-size: 15px;
    font-weight: 300;
    color: $ink-pale;
    cursor: pointer;
    transition: all 0.3s ease;
    position: relative;
    font-family: $sans;
    letter-spacing: 1px;

    &:hover { color: $ink-mid; }

    &.active {
      color: $ink-black;
      font-weight: 400;

      .tab-ink {
        position: absolute;
        bottom: -1px;
        left: 20%;
        right: 20%;
        height: 2px;
        background: $ink-black;
        animation: tab-ink-slide 0.4s cubic-bezier(0.4, 0, 0.2, 1);
      }
    }

    .tab-brush {
      position: absolute;
      bottom: 0;
      left: 50%;
      width: 0;
      height: 2px;
      background: linear-gradient(90deg, transparent, rgba(0,0,0,0.08), transparent);
      transform: translateX(-50%);
      transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);

      &.active { width: 80%; }
    }
  }
}

@keyframes tab-ink-slide {
  from { transform: scaleX(0); opacity: 0; }
  to { transform: scaleX(1); opacity: 1; }
}

// 输入框
.input-wrapper {
  position: relative;

  .input-ink-bg {
    position: absolute;
    inset: 0;
    background: radial-gradient(ellipse at center, rgba(0,0,0,0.02) 0%, transparent 70%);
    opacity: 0;
    transition: opacity 0.4s ease;
    pointer-events: none;
  }

  .input-line {
    position: absolute;
    bottom: 0;
    left: 0;
    width: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, $ink-pale, transparent);
    transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .input-ink-drop {
    position: absolute;
    bottom: -3px;
    left: 0;
    width: 6px;
    height: 6px;
    background: $ink-black;
    border-radius: 50%;
    opacity: 0;
    transform: scale(0);
    filter: blur(1px);
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  }

  &.focused {
    .input-ink-bg { opacity: 1; }

    .input-line {
      width: 100%;
      height: 2px;
      background: linear-gradient(90deg, transparent, $ink-black, transparent);
    }

    .input-ink-drop {
      opacity: 0.9;
      transform: scale(1);
      left: calc(100% - 3px);
      background: $accent-red;
      box-shadow: 0 0 8px rgba(139, 26, 26, 0.4);
    }
  }

  :deep(.el-input__wrapper) {
    background: transparent !important;
    box-shadow: none !important;
    border-bottom: 1px solid rgba(0,0,0,0.1);
    border-radius: 0 !important;
    padding: 0 0 10px 0 !important;

    &.is-focus { box-shadow: none !important; }
  }

  :deep(.el-input__inner) {
    font-family: $sans;
    color: $ink-black;
    font-size: 15px;

    &::placeholder { color: $ink-pale; }
  }

  :deep(.el-input__icon) { color: $ink-light; }
}

// 验证码
.code-item {
  :deep(.el-form-item__content) {
    display: flex;
    gap: 16px;
  }

  .code-wrapper { flex: 1; }

  .code-img {
    width: 110px;
    height: 44px;
    border: 1px solid rgba(0,0,0,0.1);
    border-radius: 4px;
    overflow: hidden;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    background: $paper-cream;
    position: relative;
    transition: all 0.3s ease;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.3s ease;
      filter: grayscale(100%) contrast(1.2) brightness(0.9);
    }

    .code-ink-overlay {
      position: absolute;
      inset: 0;
      background: linear-gradient(45deg, transparent 30%, rgba(255,255,255,0.2) 50%, transparent 70%);
      opacity: 0;
      transition: opacity 0.3s ease;
    }

    &:hover {
      border-color: $ink-light;
      img { transform: scale(1.05); }
      .code-ink-overlay { opacity: 1; }
    }

    &.refreshing img { animation: code-refresh 0.3s ease; }

    .code-loading {
      display: flex;
      gap: 4px;

      .dot {
        width: 6px;
        height: 6px;
        background: $ink-light;
        border-radius: 50%;
        animation: dot-bounce 1.4s infinite ease-in-out;
        &:nth-child(2) { animation-delay: 0.2s; }
        &:nth-child(3) { animation-delay: 0.4s; }
      }
    }
  }
}

@keyframes code-refresh {
  0% { transform: scale(1); filter: blur(0); }
  50% { transform: scale(0.9); filter: blur(2px); }
  100% { transform: scale(1); filter: blur(0); }
}

@keyframes dot-bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

// 短信按钮
.sms-btn {
  width: 110px;
  height: 44px;
  border: 1px solid $ink-mid;
  background: transparent;
  color: $ink-black;
  border-radius: 4px;
  cursor: pointer;
  font-family: $sans;
  font-size: 13px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;

  .sms-ink {
    position: absolute;
    bottom: 0; left: 0;
    width: 100%; height: 0;
    background: $ink-mid;
    transition: height 0.3s ease;
    z-index: 0;
  }

  .sms-text, .countdown { position: relative; z-index: 1; }

  &:hover:not(:disabled) {
    color: white;
    .sms-ink { height: 100%; }
  }

  &:disabled { opacity: 0.5; cursor: not-allowed; border-color: $ink-pale; }
  &.counting { border-color: $ink-pale; color: $ink-pale; }
}

// 登录按钮
.login-btn {
  width: 100%;
  height: 50px;
  border: none;
  background: $ink-black;
  color: white;
  border-radius: 8px;
  font-size: 16px;
  font-family: $sans;
  letter-spacing: 6px;
  cursor: pointer;
  position: relative;
  overflow: visible;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  margin-top: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);

  .btn-text {
    display: inline-block;
    transition: all 0.3s ease;
    &.hover { transform: scale(1.02); letter-spacing: 8px; }
  }

  .btn-seal-mark {
    position: absolute;
    top: 50%; left: 50%;
    transform: translate(-50%, -50%) scale(0);
    width: 100px; height: 100px;
    background: radial-gradient(circle, rgba(139,26,26,0.3) 0%, rgba(139,26,26,0.1) 40%, transparent 70%);
    border-radius: 50%;
    opacity: 0;
    pointer-events: none;
    transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .btn-ink-effect {
    position: absolute;
    inset: 0;
    background: linear-gradient(90deg, transparent, rgba(255,255,255,0.1), transparent);
    transform: translateX(-100%);
    transition: transform 0.6s ease;
    border-radius: 8px;
    &.active { animation: ink-sweep 0.8s ease; }
  }

  // 右上角朱红印章
  &::after {
    content: '印';
    position: absolute;
    top: -4px;
    right: -4px;
    width: 28px;
    height: 28px;
    background: $accent-red;
    color: #fff;
    font-size: 12px;
    font-family: 'Noto Serif SC', serif;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    opacity: 0;
    transform: scale(0) rotate(-15deg);
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 0 2px 8px rgba(139,26,26,0.4);
    pointer-events: none;
  }

  &:hover:not(:disabled) {
    background: $ink-deep;
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(0,0,0,0.2);

    &::after {
      opacity: 0.9;
      transform: scale(1) rotate(0deg);
    }
  }

  &:active:not(:disabled) {
    transform: translateY(1px);
    box-shadow: 0 2px 8px rgba(0,0,0,0.15);
    .btn-seal-mark {
      transform: translate(-50%, -50%) scale(2);
      opacity: 1;
    }
  }

  &:disabled { cursor: not-allowed; opacity: 0.6; }

  &.loading {
    .btn-loading {
      display: flex; align-items: center; justify-content: center; gap: 12px;
      .ink-ripple {
        width: 22px; height: 22px;
        border: 2px solid rgba(255,255,255,0.2);
        border-top-color: white;
        border-radius: 50%;
        animation: spin 0.8s linear infinite;
      }
      .loading-text { animation: loading-pulse 1.5s ease-in-out infinite; }
    }
  }

  &.success {
    background: $accent-red;
    box-shadow: 0 4px 16px rgba(139,26,26,0.3);
    .btn-success {
      display: flex; align-items: center; justify-content: center; gap: 8px;
      .success-icon { animation: success-pop 0.4s cubic-bezier(0.4, 0, 0.2, 1); }
    }
  }
}

@keyframes ink-sweep { to { transform: translateX(100%); } }
@keyframes spin { to { transform: rotate(360deg); } }
@keyframes loading-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.6; } }
@keyframes success-pop { 0% { transform: scale(0); } 50% { transform: scale(1.3); } 100% { transform: scale(1); } }

// 微信登录
.wechat-login {
  .loading-qr {
    padding: 50px 0;
    text-align: center;

    .ink-loading {
      .ink-brush-animate {
        width: 60px; height: 60px;
        margin: 0 auto 20px;
        position: relative;

        .brush-stroke {
          position: absolute;
          background: $ink-mid;
          border-radius: 2px;
          animation: brush-paint 1.5s ease-in-out infinite;

          &.stroke-1 { width: 40px; height: 4px; top: 20px; left: 10px; animation-delay: 0s; }
          &.stroke-2 { width: 30px; height: 4px; top: 30px; left: 15px; animation-delay: 0.2s; }
          &.stroke-3 { width: 35px; height: 4px; top: 40px; left: 12px; animation-delay: 0.4s; }
        }
      }

      .loading-text {
        color: $ink-pale;
        font-family: $sans;
        font-size: 14px;
      }

      .loading-dots {
        display: flex; justify-content: center; gap: 6px; margin-top: 12px;
        span {
          width: 6px; height: 6px;
          background: $ink-light;
          border-radius: 50%;
          animation: loading-dot 1.4s infinite ease-in-out;
          &:nth-child(2) { animation-delay: 0.2s; }
          &:nth-child(3) { animation-delay: 0.4s; }
        }
      }
    }
  }

  .qr-container {
    text-align: center;
    padding: 24px 0;

    .qr-frame {
      position: relative;
      display: inline-block;
      padding: 20px;
      background: white;
      box-shadow: 0 8px 30px rgba(0,0,0,0.12);
      transition: all 0.3s ease;

      .qr-ink-border {
        position: absolute;
        inset: 12px;
        border: 1px solid rgba(0,0,0,0.08);
        pointer-events: none;
      }

      .qr-code { width: 180px; height: 180px; display: block; }

      .qr-corner {
        position: absolute;
        width: 24px; height: 24px;
        border: 3px solid $ink-black;
        transition: all 0.3s ease;

        &.tl { top: 12px; left: 12px; border-right: none; border-bottom: none; }
        &.tr { top: 12px; right: 12px; border-left: none; border-bottom: none; }
        &.bl { bottom: 12px; left: 12px; border-right: none; border-top: none; }
        &.br { bottom: 12px; right: 12px; border-left: none; border-top: none; }
      }

      .qr-scan-line {
        position: absolute;
        left: 20px; right: 20px; height: 2px;
        background: linear-gradient(90deg, transparent, $accent-red, transparent);
        top: 20px;
        animation: scan-move 2s ease-in-out infinite;
        opacity: 0.6;
      }

      &:hover {
        box-shadow: 0 12px 40px rgba(0,0,0,0.18);
        .qr-corner { width: 28px; height: 28px; }
      }

      &.scanned {
        .qr-scan-line {
          animation: none;
          top: 50%;
          background: linear-gradient(90deg, transparent, #2d5a3d, transparent);
        }
      }
    }

    .qr-tip {
      margin-top: 24px;
      font-family: $sans;
      color: $ink-light;

      .ink-text {
        position: relative;
        padding: 0 8px;

        &::after {
          content: '';
          position: absolute;
          bottom: -6px; left: 0;
          width: 100%; height: 1px;
          background: linear-gradient(90deg, transparent, $ink-pale, transparent);
          transform: scaleX(0.8);
          transition: all 0.3s ease;
        }

        &.scanned {
          color: #2d5a3d;
          &::after {
            background: linear-gradient(90deg, transparent, #2d5a3d, transparent);
            transform: scaleX(1);
          }
        }
      }
    }
  }
}

@keyframes brush-paint {
  0%, 100% { transform: scaleX(0); opacity: 0; }
  50% { transform: scaleX(1); opacity: 1; }
}

@keyframes loading-dot {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

@keyframes scan-move {
  0%, 100% { top: 20px; opacity: 0; }
  10% { opacity: 0.6; }
  90% { opacity: 0.6; }
  50% { top: calc(100% - 20px); }
}

// 表单切换过渡
.form-fade-enter-active,
.form-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.form-fade-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.form-fade-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}
</style>
