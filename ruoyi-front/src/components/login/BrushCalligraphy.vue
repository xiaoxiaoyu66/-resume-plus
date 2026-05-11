<template>
  <div class="brush-calligraphy" :class="{ 'skip-anim': !animate }">
    <!-- 主文字 -->
    <div class="title-row">
      <svg class="ink-svg" viewBox="0 0 320 100" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <!-- 水墨纹理滤镜：纸纹 + 墨迹边缘羽化 -->
          <filter id="ink-bleed" x="-10%" y="-10%" width="120%" height="120%">
            <feTurbulence type="fractalNoise" baseFrequency="0.04" numOctaves="4" result="noise" />
            <feDisplacementMap in="SourceGraphic" in2="noise" scale="3" xChannelSelector="R" yChannelSelector="G" result="displaced" />
            <feGaussianBlur in="displaced" stdDeviation="0.5" result="blurred" />
            <feMerge>
              <feMergeNode in="blurred" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>

          <!-- 刷墨遮罩 — 从左到右扫过 -->
          <linearGradient id="brush-sweep" x1="0" y1="0" x2="1" y2="0.05">
            <stop offset="0%" stop-color="#0a0a0a" />
            <stop offset="20%" stop-color="#0a0a0a" />
            <stop offset="35%" stop-color="#0a0a0a" />
            <stop offset="50%" stop-color="#0a0a0a" />
            <stop offset="65%" stop-color="#0a0a0a" />
            <stop offset="80%" stop-color="#0a0a0a" />
            <stop offset="95%" stop-color="#0a0a0a" />
            <stop offset="100%" stop-color="#0a0a0a" />
          </linearGradient>

          <!-- 每个字的揭示遮罩 -->
          <linearGradient id="reveal-mask" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0%" stop-color="white" />
            <stop offset="100%" stop-color="white" />
          </linearGradient>

          <mask id="ink-mask">
            <rect x="0" y="0" width="320" height="100" fill="url(#reveal-mask)" />
          </mask>

          <!-- 墨迹斑点滤镜 -->
          <filter id="ink-splatter">
            <feTurbulence type="fractalNoise" baseFrequency="0.6" numOctaves="3" />
            <feColorMatrix type="matrix" values="0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 0 0.3 0" />
          </filter>
        </defs>

        <!-- 墨迹底纹（极淡） -->
        <rect x="0" y="0" width="320" height="100" fill="none" />

        <!-- 画笔扫过动画层 — 用渐变遮罩从左到右揭示文字 -->
        <g mask="url(#ink-mask)">
          <!-- 简 -->
          <text x="10" y="78" class="char-svg" fill="#0a0a0a" font-family="'Noto Serif SC','STSong',serif" font-weight="700" font-size="76" filter="url(#ink-bleed)">
            简
          </text>
          <!-- 历 -->
          <text x="120" y="78" class="char-svg char-delay-1" fill="#0a0a0a" font-family="'Noto Serif SC','STSong',serif" font-weight="700" font-size="76" filter="url(#ink-bleed)">
            历
          </text>
          <!-- + -->
          <text x="260" y="72" class="char-svg char-delay-2" fill="#8b1a1a" font-family="'Noto Serif SC','STSong',serif" font-weight="700" font-size="52" filter="url(#ink-bleed)">
            +
          </text>
        </g>

        <!-- 墨迹飞溅（装饰粒子） -->
        <g class="splatter-group">
          <circle cx="38" cy="30" r="1.5" fill="#0a0a0a" opacity="0" class="splatter s1" />
          <circle cx="75" cy="22" r="1" fill="#0a0a0a" opacity="0" class="splatter s2" />
          <circle cx="160" cy="25" r="1.2" fill="#0a0a0a" opacity="0" class="splatter s3" />
          <circle cx="195" cy="35" r="0.8" fill="#0a0a0a" opacity="0" class="splatter s4" />
          <circle cx="58" cy="85" r="1" fill="#0a0a0a" opacity="0" class="splatter s5" />
          <circle cx="230" cy="20" r="1.3" fill="#8b1a1a" opacity="0" class="splatter s6" />
        </g>
      </svg>
    </div>

    <!-- 副标题 -->
    <p class="subtitle" :class="{ 'show': showSubtitle }">
      <span class="ink-divider"></span>
      <span class="subtitle-text">{{ subtitle }}</span>
      <span class="ink-divider"></span>
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

const props = defineProps<{
  skipAnimation?: boolean
}>()

const subtitle = '一份好简历，是一篇好文章'
const animate = ref(false)
const showSubtitle = ref(false)

onMounted(() => {
  // 首次访问才播放完整动画
  if (props.skipAnimation) {
    animate.value = false
    showSubtitle.value = true
    return
  }
  // 触发 CSS 动画
  animate.value = true
  // 副标题延迟出现
  setTimeout(() => { showSubtitle.value = true }, 1200)
})
</script>

<style scoped lang="scss">
$ink-black: #0a0a0a;
$ink-pale: #404040;
$accent-red: #8b1a1a;

.brush-calligraphy {
  text-align: center;
  margin-bottom: 36px;
  position: relative;
  z-index: 1;
}

.title-row {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100px;

  .ink-svg {
    width: 100%;
    max-width: 320px;
    height: 100px;
  }
}

/* 每个字的笔触揭示动画 */
.char-svg {
  opacity: 0;
  transform-origin: left center;
  animation: char-brush-in 0.6s cubic-bezier(0.2, 0.9, 0.3, 1) forwards;
}

.char-delay-1 {
  animation-delay: 0.5s;
}

.char-delay-2 {
  animation-delay: 1.0s;
}

@keyframes char-brush-in {
  0% {
    opacity: 0;
    clip-path: inset(0 100% 0 0);
  }
  50% {
    opacity: 1;
  }
  100% {
    opacity: 1;
    clip-path: inset(0 0 0 0);
  }
}

/* 跳过动画 */
.skip-anim {
  .char-svg {
    opacity: 1;
    animation: none;
  }
}

/* 墨迹飞溅 */
.splatter {
  animation: splatter-in 0.3s ease forwards;
}

.s1 { animation-delay: 0.6s; }
.s2 { animation-delay: 0.7s; }
.s3 { animation-delay: 0.9s; }
.s4 { animation-delay: 1.0s; }
.s5 { animation-delay: 1.1s; }
.s6 { animation-delay: 1.3s; }

@keyframes splatter-in {
  0% { opacity: 0; transform: scale(0); }
  60% { opacity: 0.8; transform: scale(1.3); }
  100% { opacity: 0.5; transform: scale(1); }
}

/* 副标题 */
.subtitle {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
  opacity: 0;
  transform: translateY(8px);
  transition: all 0.6s cubic-bezier(0.4, 0, 0.2, 1);

  &.show { opacity: 1; transform: translateY(0); }

  .ink-divider {
    width: 24px;
    height: 1px;
    background: linear-gradient(90deg, transparent, $ink-pale, transparent);
  }

  .subtitle-text {
    font-size: 13px;
    color: $ink-pale;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
    letter-spacing: 2px;
  }
}
</style>
