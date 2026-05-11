<template>
  <div
    class="ink-avatar"
    :class="[state]"
    :style="{ width: size + 'px', height: size + 'px' }"
    @mouseenter="hover = true"
    @mouseleave="hover = false"
  >
    <!-- SVG 墨韵 "简" 字 -->
    <svg class="avatar-svg" :viewBox="`0 0 ${svgSize} ${svgSize}`" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <filter :id="`av-ink-${uid}`" x="-20%" y="-20%" width="140%" height="140%">
          <feTurbulence type="fractalNoise" baseFrequency="0.06" numOctaves="3" result="noise" />
          <feDisplacementMap in="SourceGraphic" in2="noise" scale="1.5" xChannelSelector="R" yChannelSelector="G" />
        </filter>
      </defs>
      <text
        x="50%"
        y="56%"
        dominant-baseline="central"
        text-anchor="middle"
        class="avatar-char"
        :class="{ 'fade-out': state === 'thinking' }"
        fill="#D4A574"
        font-family="'Noto Serif SC','STSong',serif"
        font-weight="700"
        :font-size="charSize"
        :filter="`url(#av-ink-${uid})`"
      >简</text>
    </svg>

    <!-- 思考中 → 墨滴涟漪 -->
    <div v-if="state === 'thinking'" class="ink-rings">
      <span class="ring r1"></span>
      <span class="ring r2"></span>
      <span class="ring r3"></span>
    </div>

    <!-- 错误 / 超时 → 朱红闪烁 -->
    <div v-if="state === 'error'" class="error-overlay"></div>

    <!-- 提示 -->
    <Transition name="tip">
      <div v-if="hover" class="avatar-tip">简历+ AI助手</div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const props = withDefaults(defineProps<{
  state?: 'idle' | 'thinking' | 'error'
  size?: number
}>(), {
  state: 'idle',
  size: 40,
})

const hover = ref(false)

// 唯一 ID 防 SVG filter 冲突
const uid = computed(() => Math.random().toString(36).slice(2, 8))

const svgSize = 80
const charSize = 52
</script>

<style scoped lang="scss">
$gold: #D4A574;
$ink-dark: #2D1B13;
$ink-deeper: #1A0F0A;
$red-error: #dc2626;

.ink-avatar {
  position: relative;
  border-radius: 50%;
  background: linear-gradient(135deg, $ink-dark 0%, $ink-deeper 100%);
  border: 1.5px solid rgba($gold, 0.3);
  box-shadow: 0 2px 10px rgba(45, 27, 19, 0.35);
  flex-shrink: 0;
  overflow: visible;
  cursor: default;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  animation: avatar-in 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);

  /* 内圈装饰线 */
  &::after {
    content: '';
    position: absolute;
    inset: 4px;
    border-radius: 50%;
    border: 1px solid rgba($gold, 0.12);
    pointer-events: none;
  }

  &.thinking {
    animation: avatar-think-pulse 2s ease-in-out infinite;
  }

  &.error {
    animation: avatar-shake 0.5s ease;
    border-color: rgba($red-error, 0.5);
    box-shadow: 0 2px 10px rgba($red-error, 0.3);
  }
}

/* SVG 文字 */
.avatar-svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.avatar-char {
  transition: opacity 0.4s ease;
  &.fade-out {
    opacity: 0.25;
  }
}

/* 思绪涟漪 */
.ink-rings {
  position: absolute;
  inset: -4px;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;

  .ring {
    position: absolute;
    border-radius: 50%;
    border: 1.5px solid rgba($gold, 0.25);
    animation: ring-expand 1.8s ease-out infinite;

    &.r1 { animation-delay: 0s; }
    &.r2 { animation-delay: 0.6s; }
    &.r3 { animation-delay: 1.2s; }
  }
}

/* 错误闪烁 */
.error-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba($red-error, 0.15);
  animation: error-flash 0.5s ease;
}

/* 提示 tooltip */
.avatar-tip {
  position: absolute;
  left: 50%;
  bottom: calc(100% + 8px);
  transform: translateX(-50%);
  padding: 4px 10px;
  background: rgba(0, 0, 0, 0.8);
  color: #fff;
  font-size: 11px;
  border-radius: 4px;
  white-space: nowrap;
  pointer-events: none;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;

  &::after {
    content: '';
    position: absolute;
    top: 100%;
    left: 50%;
    transform: translateX(-50%);
    border: 4px solid transparent;
    border-top-color: rgba(0, 0, 0, 0.8);
  }
}

/* 动画 */
@keyframes avatar-in {
  0% { opacity: 0; transform: scale(0.8); }
  100% { opacity: 1; transform: scale(1); }
}

@keyframes avatar-think-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.04); }
}

@keyframes ring-expand {
  0% { width: 0; height: 0; opacity: 0.6; }
  100% { width: 100%; height: 100%; opacity: 0; }
}

@keyframes avatar-shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-3px); }
  20%, 40%, 60%, 80% { transform: translateX(3px); }
}

@keyframes error-flash {
  0% { opacity: 0; }
  20% { opacity: 1; }
  100% { opacity: 0; }
}

.tip-enter-active,
.tip-leave-active {
  transition: all 0.2s ease;
}
.tip-enter-from,
.tip-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(4px);
}
</style>
