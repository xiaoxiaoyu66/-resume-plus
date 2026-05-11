<template>
  <canvas
    ref="canvasRef"
    class="ink-trail-canvas"
    v-show="enabled"
  />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const canvasRef = ref<HTMLCanvasElement | null>(null)

// 只在精细指针设备（鼠标）启用
const enabled = ref(false)
let ctx: CanvasRenderingContext2D | null = null
let animId: number | null = null
let drops: InkDrop[] = []
let pointerX = -100
let pointerY = -100
let lastMove = 0

interface InkDrop {
  x: number
  y: number
  size: number
  alpha: number
  life: number
  maxLife: number
  vx: number
  vy: number
}

function init() {
  enabled.value = window.matchMedia('(pointer: fine)').matches
  if (!enabled.value) return

  const canvas = canvasRef.value
  if (!canvas) return

  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
  ctx = canvas.getContext('2d')

  window.addEventListener('mousemove', onPointerMove)
  window.addEventListener('resize', onResize)
  animId = requestAnimationFrame(tick)
}

function onPointerMove(e: MouseEvent) {
  pointerX = e.clientX
  pointerY = e.clientY
  lastMove = performance.now()

  // 每帧最多产生一个墨点
  if (Math.random() > 0.5) {
    drops.push({
      x: pointerX + (Math.random() - 0.5) * 8,
      y: pointerY + (Math.random() - 0.5) * 8,
      size: 3 + Math.random() * 6,
      alpha: 0.12 + Math.random() * 0.08,
      life: 0,
      maxLife: 60 + Math.random() * 40, // frames
      vx: (Math.random() - 0.5) * 0.3,
      vy: (Math.random() - 0.5) * 0.3 + 0.1, // slight downward drift
    })
  }
}

function onResize() {
  const canvas = canvasRef.value
  if (!canvas) return
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
}

function tick() {
  if (!ctx || !canvasRef.value) {
    animId = requestAnimationFrame(tick)
    return
  }

  const canvas = canvasRef.value
  ctx.clearRect(0, 0, canvas.width, canvas.height)

  // 空闲时不再产生新墨点
  const idle = performance.now() - lastMove > 300

  // 更新并绘制
  drops = drops.filter(d => d.life < d.maxLife)
  if (!idle) {
    // 鼠标快速移动时产生更多墨点
    if (Math.random() > 0.7) {
      drops.push({
        x: pointerX + (Math.random() - 0.5) * 12,
        y: pointerY + (Math.random() - 0.5) * 12,
        size: 2 + Math.random() * 4,
        alpha: 0.06 + Math.random() * 0.06,
        life: 0,
        maxLife: 40 + Math.random() * 30,
        vx: (Math.random() - 0.5) * 0.5,
        vy: (Math.random() - 0.5) * 0.5,
      })
    }
  }

  for (const d of drops) {
    d.life++
    d.x += d.vx
    d.y += d.vy
    const progress = d.life / d.maxLife
    const currentAlpha = d.alpha * (1 - progress)

    ctx.beginPath()
    ctx.arc(d.x, d.y, d.size * (1 - progress * 0.5), 0, Math.PI * 2)
    ctx.fillStyle = `rgba(10, 10, 10, ${currentAlpha})`
    ctx.fill()
  }

  // 限制墨点数量
  if (drops.length > 200) {
    drops.splice(0, drops.length - 200)
  }

  animId = requestAnimationFrame(tick)
}

onMounted(init)

onUnmounted(() => {
  if (animId) cancelAnimationFrame(animId)
  window.removeEventListener('mousemove', onPointerMove)
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped lang="scss">
.ink-trail-canvas {
  position: fixed;
  inset: 0;
  z-index: 9999;
  pointer-events: none;
}
</style>
