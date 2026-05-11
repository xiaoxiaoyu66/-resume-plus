<template>
  <div class="ink-landscape-bg">
    <canvas ref="landscapeCanvas" class="landscape-canvas"></canvas>
    <div class="gradient-overlay"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const landscapeCanvas = ref(null)
let animationId = null
let ctx = null
let canvas = null

// 动画状态
const state = {
  time: 0,
  birds: [],
  petals: [],
  poem: { opacity: 0, phase: 0 },
  moonOffset: 0
}

function initCanvas() {
  canvas = landscapeCanvas.value
  if (!canvas) return

  ctx = canvas.getContext('2d')
  resizeCanvas()

  initBirds()
  initPlumBlossoms()

  animate()

  window.addEventListener('resize', resizeCanvas)
}

function resizeCanvas() {
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
  // 重置飞鸟和花瓣位置
  if (state.birds.length) state.birds.forEach(b => b.x = -50)
  if (state.petals.length) state.petals.forEach(p => { p.x = Math.random() * canvas.width; p.y = -20 })
}

// ── 飞鸟（减少到3只） ──
function initBirds() {
  state.birds = []
  for (let i = 0; i < 3; i++) {
    state.birds.push({
      x: -50 + Math.random() * 100,
      y: canvas.height * 0.12 + Math.random() * 40,
      speed: 0.25 + Math.random() * 0.2,
      offset: Math.random() * Math.PI * 2,
      wingPhase: Math.random() * Math.PI * 2
    })
  }
}

// ── 梅花瓣 ──
function initPlumBlossoms() {
  state.petals = []
  for (let i = 0; i < 4; i++) {
    state.petals.push({
      x: Math.random() * (canvas?.width || 800),
      y: -20 - Math.random() * 100,
      size: 6 + Math.random() * 6,
      speedY: 0.15 + Math.random() * 0.15,
      speedX: (Math.random() - 0.5) * 0.1,
      rotation: Math.random() * Math.PI * 2,
      rotSpeed: (Math.random() - 0.5) * 0.005,
      swayPhase: Math.random() * Math.PI * 2,
      opacity: 0.15 + Math.random() * 0.15,
      color: Math.random() > 0.5
        ? [180, 120, 130]   // 淡粉
        : [160, 100, 110]   // 暗红
    })
  }
}

// ── 山脉（2层泼墨） ──
function drawMountains() {
  const h = canvas.height

  // 远山 — 极淡墨色，宽笔触
  drawMountainLayer(h * 0.35, h * 0.55, 'rgba(0, 0, 0, 0.04)', 0.0015, 100, 20)
  // 近山 — 淡墨
  drawMountainLayer(h * 0.5, h * 0.75, 'rgba(0, 0, 0, 0.08)', 0.0025, 70, 18)
}

function drawMountainLayer(baseY, peakY, color, frequency, amplitude, step) {
  ctx.fillStyle = color
  ctx.beginPath()

  const pts = []
  for (let x = -50; x <= canvas.width + 50; x += step) {
    const n = Math.sin(x * frequency) * amplitude
            + Math.sin(x * frequency * 2.3) * amplitude * 0.4
            + Math.sin(x * frequency * 4.7) * amplitude * 0.15
    pts.push({ x, y: baseY + n })
  }

  ctx.moveTo(pts[0].x, canvas.height)
  ctx.lineTo(pts[0].x, pts[0].y)
  for (let i = 0; i < pts.length - 1; i++) {
    const mx = (pts[i].x + pts[i + 1].x) / 2
    const my = (pts[i].y + pts[i + 1].y) / 2
    ctx.quadraticCurveTo(pts[i].x, pts[i].y, mx, my)
  }
  ctx.lineTo(pts[pts.length - 1].x, canvas.height)
  ctx.closePath()
  ctx.fill()
}

// ── 水面 ──
function drawWater() {
  const h = canvas.height
  const waterY = h * 0.75

  const grad = ctx.createLinearGradient(0, waterY, 0, h)
  grad.addColorStop(0, 'rgba(200, 200, 200, 0.06)')
  grad.addColorStop(1, 'rgba(220, 220, 220, 0.1)')
  ctx.fillStyle = grad
  ctx.fillRect(0, waterY, canvas.width, h - waterY)

  // 极淡波纹
  ctx.strokeStyle = 'rgba(180, 180, 180, 0.06)'
  ctx.lineWidth = 0.5
  for (let i = 0; i < 4; i++) {
    const y = waterY + 15 + i * 30
    const offset = state.time * 0.3 + i * 60
    ctx.beginPath()
    for (let x = 0; x <= canvas.width; x += 15) {
      const wy = y + Math.sin((x + offset) * 0.008) * 1.5
      x === 0 ? ctx.moveTo(x, wy) : ctx.lineTo(x, wy)
    }
    ctx.stroke()
  }
}

// ── 雾气 ──
function drawMist() {
  const w = canvas.width
  const h = canvas.height

  for (let i = 0; i < 2; i++) {
    const grad = ctx.createLinearGradient(0, h * (0.5 + i * 0.15), 0, h * (0.7 + i * 0.1))
    grad.addColorStop(0, 'rgba(255, 255, 255, 0)')
    grad.addColorStop(0.5, `rgba(255, 255, 255, ${0.04 - i * 0.01})`)
    grad.addColorStop(1, 'rgba(255, 255, 255, 0)')
    ctx.fillStyle = grad
    ctx.beginPath()
    const offset = state.time * 0.15 + i * 80
    for (let x = 0; x <= w; x += 12) {
      const y = h * (0.55 + i * 0.1) + Math.sin((x + offset) * 0.003) * 20 + Math.sin((x + offset * 0.5) * 0.008) * 10
      x === 0 ? ctx.moveTo(x, y) : ctx.lineTo(x, y)
    }
    ctx.lineTo(w, h)
    ctx.lineTo(0, h)
    ctx.closePath()
    ctx.fill()
  }
}

// ── 月亮（冷灰色，几乎静止） ──
function drawMoon() {
  const w = canvas.width
  const h = canvas.height
  const r = 28

  // 月亮位置 — 右上区域，极慢漂移
  state.moonOffset += 0.0003
  const mx = w * 0.78 + Math.sin(state.moonOffset * 0.5) * 15
  const my = h * 0.18 + Math.sin(state.moonOffset * 0.3) * 10

  // 光晕
  const glow = ctx.createRadialGradient(mx, my, 0, mx, my, r * 4)
  glow.addColorStop(0, 'rgba(220, 220, 230, 0.06)')
  glow.addColorStop(0.4, 'rgba(210, 210, 220, 0.03)')
  glow.addColorStop(1, 'rgba(200, 200, 210, 0)')
  ctx.fillStyle = glow
  ctx.beginPath()
  ctx.arc(mx, my, r * 4, 0, Math.PI * 2)
  ctx.fill()

  // 月轮
  const moonGrad = ctx.createRadialGradient(mx, my, 0, mx, my, r)
  moonGrad.addColorStop(0, 'rgba(230, 230, 240, 0.2)')
  moonGrad.addColorStop(0.6, 'rgba(215, 215, 225, 0.12)')
  moonGrad.addColorStop(1, 'rgba(200, 200, 210, 0.05)')
  ctx.fillStyle = moonGrad
  ctx.beginPath()
  ctx.arc(mx, my, r, 0, Math.PI * 2)
  ctx.fill()
}

// ── 飞鸟 ──
function drawBirds() {
  ctx.strokeStyle = 'rgba(40, 40, 40, 0.35)'
  ctx.lineWidth = 1

  state.birds.forEach(bird => {
    bird.x += bird.speed
    bird.y += Math.sin(state.time * 0.015 + bird.offset) * 0.2
    bird.wingPhase += 0.12

    if (bird.x > canvas.width + 50) {
      bird.x = -50
      bird.y = canvas.height * 0.12 + Math.random() * 40
    }

    const wingY = Math.sin(bird.wingPhase) * 3
    ctx.beginPath()
    ctx.moveTo(bird.x - 5, bird.y + wingY)
    ctx.quadraticCurveTo(bird.x, bird.y - 1.5, bird.x + 5, bird.y + wingY)
    ctx.stroke()
  })
}

// ── 梅花瓣 ──
function drawPlumBlossoms() {
  state.petals.forEach(p => {
    p.y += p.speedY
    p.x += p.speedX + Math.sin(state.time * 0.01 + p.swayPhase) * 0.15
    p.rotation += p.rotSpeed

    // 循环
    if (p.y > canvas.height + 30) {
      p.y = -20
      p.x = Math.random() * canvas.width
    }
    if (p.x < -30) p.x = canvas.width + 10
    if (p.x > canvas.width + 30) p.x = -10

    ctx.save()
    ctx.translate(p.x, p.y)
    ctx.rotate(p.rotation)
    ctx.globalAlpha = p.opacity

    // 五瓣梅花 — 简化为三笔弧形
    const s = p.size
    const [r, g, b] = p.color

    ctx.fillStyle = `rgba(${r}, ${g}, ${b}, 0.6)`
    ctx.strokeStyle = `rgba(${r}, ${g}, ${b}, 0.4)`
    ctx.lineWidth = 0.5

    for (let i = 0; i < 5; i++) {
      const angle = (i / 5) * Math.PI * 2 - Math.PI / 2
      const px = Math.cos(angle) * s * 0.5
      const py = Math.sin(angle) * s * 0.5
      ctx.beginPath()
      ctx.ellipse(px, py, s * 0.35, s * 0.2, angle, 0, Math.PI * 2)
      ctx.fill()
    }

    // 花蕊 — 淡黄色点
    ctx.fillStyle = 'rgba(200, 180, 150, 0.3)'
    ctx.beginPath()
    ctx.arc(0, 0, s * 0.08, 0, Math.PI * 2)
    ctx.fill()

    ctx.restore()
  })
}

// ── 诗句残影 ──
function drawPoemText() {
  const w = canvas.width
  const h = canvas.height

  // 30秒周期：淡入 → 保持 → 淡出
  state.poem.phase += 0.002
  const cycle = Math.sin(state.poem.phase * 0.2)
  const opacity = Math.max(0, cycle) * 0.035 // 最高 0.035

  if (opacity < 0.001) return

  ctx.save()
  ctx.globalAlpha = opacity
  ctx.fillStyle = 'rgba(0, 0, 0, 0.8)'
  ctx.font = '14px "Noto Serif SC", "STSong", "SimSun", serif'
  ctx.textAlign = 'right'

  // 右下角两句诗
  const lines = ['江畔何人初见月', '江月何年初照人']
  const lh = 24
  const sx = w - 40
  const sy = h - 60

  lines.forEach((line, i) => {
    ctx.fillText(line, sx, sy + i * lh)
  })

  ctx.restore()
}

// ── 氛围覆盖（只留极淡暗角） ──
function drawVignette() {
  const grad = ctx.createRadialGradient(
    canvas.width / 2, canvas.height / 2, 0,
    canvas.width / 2, canvas.height / 2, canvas.width * 0.7
  )
  grad.addColorStop(0, 'rgba(0, 0, 0, 0)')
  grad.addColorStop(1, 'rgba(0, 0, 0, 0.06)')
  ctx.fillStyle = grad
  ctx.fillRect(0, 0, canvas.width, canvas.height)
}

// ── 动画循环 ──
function animate() {
  if (!ctx || !canvas) return
  state.time++

  // 天空渐变 — 宣纸色
  const sky = ctx.createLinearGradient(0, 0, 0, canvas.height)
  sky.addColorStop(0, '#f7f5f0')
  sky.addColorStop(0.5, '#f5f3ee')
  sky.addColorStop(1, '#f0eee8')
  ctx.fillStyle = sky
  ctx.fillRect(0, 0, canvas.width, canvas.height)

  drawMoon()
  drawMountains()
  drawWater()
  drawMist()
  drawPlumBlossoms()
  drawBirds()
  drawPoemText()
  drawVignette()

  animationId = requestAnimationFrame(animate)
}

onMounted(() => {
  initCanvas()
})

onUnmounted(() => {
  if (animationId) cancelAnimationFrame(animationId)
  window.removeEventListener('resize', resizeCanvas)
})
</script>

<style scoped lang="scss">
.ink-landscape-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  background: #f7f5f0;

  .landscape-canvas {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
  }

  .gradient-overlay {
    position: absolute;
    inset: 0;
    background: radial-gradient(ellipse at center, transparent 0%, rgba(0,0,0,0.06) 100%);
    pointer-events: none;
  }
}
</style>
