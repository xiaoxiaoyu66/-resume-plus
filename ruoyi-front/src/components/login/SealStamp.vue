<template>
  <Transition name="seal">
    <div v-if="visible" class="seal-overlay" @click="$emit('done')">
      <div class="seal-container" :style="sealStyle">
        <!-- 印章本体 -->
        <div class="seal-body" ref="sealRef">
          <div class="seal-border"></div>
          <div class="seal-text">{{ text }}</div>
          <!-- 印泥晕染 -->
          <div class="seal-ink-bleed"></div>
        </div>
        <!-- 墨点散射 -->
        <div class="seal-particles">
          <span v-for="n in 6" :key="n" class="particle" :style="particleStyle(n)"></span>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  visible: boolean
  text?: string
  size?: number
}>()

defineEmits<{ done: [] }>()

const text = computed(() => props.text || '简')
const size = computed(() => props.size || 120)

const sealStyle = computed(() => ({
  width: size.value + 'px',
  height: size.value + 'px',
}))

function particleStyle(n: number) {
  const angle = (n / 6) * Math.PI * 2
  const dist = 40 + Math.random() * 30
  const x = Math.cos(angle) * dist
  const y = Math.sin(angle) * dist
  const size = 3 + Math.random() * 4
  return {
    '--x': x + 'px',
    '--y': y + 'px',
    '--size': size + 'px',
    animationDelay: 0.3 + n * 0.05 + 's',
  }
}
</script>

<style scoped lang="scss">
$accent-red: #8b1a1a;

.seal-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
  pointer-events: auto;
  cursor: pointer;
}

.seal-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.seal-body {
  position: relative;
  width: 100%;
  height: 100%;
  border: 3px solid $accent-red;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: rgba(139, 26, 26, 0.05);
  animation: seal-stamp 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
  transform-origin: center;
  opacity: 0;
  transform: scale(0) rotate(-15deg);

  .seal-border {
    position: absolute;
    inset: 4px;
    border: 1px solid rgba(139, 26, 26, 0.3);
    border-radius: 4px;
    pointer-events: none;
  }

  .seal-text {
    position: relative;
    z-index: 2;
    font-family: 'Noto Serif SC', 'STSong', 'SimSun', serif;
    font-size: 48px;
    font-weight: 700;
    color: $accent-red;
    text-shadow: 0 0 6px rgba(139, 26, 26, 0.3);
    letter-spacing: 2px;
  }

  .seal-ink-bleed {
    position: absolute;
    inset: -20px;
    background: radial-gradient(ellipse, rgba(139, 26, 26, 0.08) 0%, transparent 70%);
    opacity: 0;
    animation: ink-bleed-out 0.8s ease 0.3s forwards;
    pointer-events: none;
  }
}

@keyframes seal-stamp {
  0% {
    opacity: 0;
    transform: scale(0) rotate(-15deg);
  }
  50% {
    opacity: 1;
    transform: scale(1.15) rotate(-3deg);
  }
  70% {
    transform: scale(0.95) rotate(-5deg);
  }
  100% {
    opacity: 1;
    transform: scale(1) rotate(-4deg);
  }
}

@keyframes ink-bleed-out {
  to { opacity: 1; }
}

/* 散射墨点 */
.particle {
  position: absolute;
  top: 50%;
  left: 50%;
  width: var(--size);
  height: var(--size);
  background: $accent-red;
  border-radius: 50%;
  opacity: 0;
  animation: particle-fly 0.6s cubic-bezier(0.4, 0, 0.2, 1) forwards;
}

@keyframes particle-fly {
  0% {
    opacity: 0.8;
    transform: translate(0, 0) scale(1);
  }
  60% {
    opacity: 0.5;
    transform: translate(var(--x), var(--y)) scale(1.2);
  }
  100% {
    opacity: 0;
    transform: translate(calc(var(--x) * 1.5), calc(var(--y) * 1.5)) scale(0);
  }
}

/* 过渡动画 */
.seal-enter-active { transition: opacity 0.2s ease; }
.seal-leave-active { transition: opacity 0.5s ease; }
.seal-enter-from, .seal-leave-to { opacity: 0; }
</style>
