<template>
  <transition name="gold-fade">
    <div v-if="visible" class="gold-overlay" @click.self="$emit('done')">
      <!-- 上升的金色粒子 -->
      <div class="gold-particles">
        <span v-for="i in 20" :key="i" class="particle" :style="particleStyle(i)" />
      </div>

      <!-- 金色祝福文字 -->
      <div class="gold-text">{{ text }}</div>
    </div>
  </transition>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  text: { type: String, default: '小何在此，祝您offer拿到手软' }
})

const emit = defineEmits(['update:modelValue', 'done'])

const visible = ref(false)
let timer = null

watch(() => props.modelValue, (val) => {
  if (val) {
    visible.value = true
    timer = setTimeout(() => {
      visible.value = false
      setTimeout(() => {
        emit('update:modelValue', false)
        emit('done')
      }, 400)
    }, 2500)
  } else {
    visible.value = false
  }
})

function particleStyle(i) {
  const left = 10 + Math.random() * 80
  const delay = Math.random() * 0.8
  const duration = 1.5 + Math.random() * 1
  const size = 3 + Math.random() * 5
  return {
    left: `${left}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`
  }
}

onBeforeUnmount(() => {
  clearTimeout(timer)
})
</script>

<style scoped>
.gold-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
  animation: goldBgIn 0.3s ease-out;
}

/* 金色粒子 */
.gold-particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.particle {
  position: absolute;
  bottom: -10px;
  border-radius: 50%;
  background: radial-gradient(circle, #fcf6ba, #bf953f);
  box-shadow: 0 0 6px #fcf6ba;
  animation: particleUp ease-out infinite;
}

@keyframes particleUp {
  0% {
    transform: translateY(0) scale(1);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 0.8;
  }
  100% {
    transform: translateY(-100vh) scale(0.3);
    opacity: 0;
  }
}

/* 金色文字 */
.gold-text {
  position: relative;
  z-index: 1;
  font-family: 'STKaiti', 'KaiTi', '楷体', 'Noto Serif SC', serif;
  font-size: 36px;
  font-weight: 700;
  line-height: 1.6;
  text-align: center;
  padding: 40px 60px;
  animation: goldTextIn 0.8s ease-out 0.3s both;

  /* 镀金渐变 */
  background: linear-gradient(
    135deg,
    #bf953f 0%,
    #fcf6ba 20%,
    #b38728 40%,
    #fbf5b7 55%,
    #aa771c 70%,
    #fcf6ba 85%,
    #bf953f 100%
  );
  background-size: 200% 100%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;

  /* 发光效果 */
  filter: drop-shadow(0 0 20px rgba(191, 149, 63, 0.4))
          drop-shadow(0 0 40px rgba(191, 149, 63, 0.2));
  animation:
    goldTextIn 0.8s ease-out 0.3s both,
    shimmer 2.5s ease-in-out 0.3s;
}

@keyframes goldTextIn {
  from { opacity: 0; transform: scale(0.8) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

/* 扫光效果 */
@keyframes shimmer {
  0% { background-position: 200% center; }
  40% { background-position: -200% center; }
  100% { background-position: -200% center; }
}

@keyframes goldBgIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.gold-fade-enter-active { transition: opacity 0.3s; }
.gold-fade-leave-active { transition: opacity 0.4s; }
.gold-fade-enter-from,
.gold-fade-leave-to { opacity: 0; }
</style>
