<template>
  <div class="ripple-container" ref="containerRef" @click="createRipple"></div>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'

const containerRef = ref<HTMLElement | null>(null)

function createRipple(e: MouseEvent) {
  const el = containerRef.value
  if (!el) return
  const ripple = document.createElement('div')
  ripple.className = 'ink-ripple-effect'
  const rect = el.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  ripple.style.left = x + 'px'
  ripple.style.top = y + 'px'
  el.appendChild(ripple)
  setTimeout(() => ripple.remove(), 1000)
}

// 如果父组件需要手动触发涟漪
defineExpose({ createRipple })
</script>

<style scoped lang="scss">
.ripple-container {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
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
  to { transform: translate(-50%, -50%) scale(20); opacity: 0; }
}
</style>
