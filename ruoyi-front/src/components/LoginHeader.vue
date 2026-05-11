<template>
  <div class="login-header">
    <h1 class="title" @mouseenter="titleHover = true" @mouseleave="titleHover = false">
      <span
        v-for="(char, index) in titleChars" :key="index"
        class="char"
        :class="{ 'hover': titleHover, 'skip-anim': !firstVisit }"
        :style="{ animationDelay: firstVisit ? index * 0.15 + 's' : '0s', '--char-index': index }"
      >{{ char }}</span>
    </h1>
    <p class="subtitle">
      <span class="typing-text">{{ subtitleText }}</span>
      <span class="cursor" :class="{ 'blink': cursorBlink }">|</span>
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

const titleText = '简历+'
const titleChars = computed(() => titleText.split(''))
const subtitleText = ref('')
const fullSubtitle = '一份好简历，是一篇好文章'
const titleHover = ref(false)
const cursorBlink = ref(true)
const firstVisit = ref(true)

let typingIndex = 0
let cursorInterval = null

function typeSubtitle() {
  if (typingIndex < fullSubtitle.length) {
    subtitleText.value += fullSubtitle[typingIndex]
    typingIndex++
    setTimeout(typeSubtitle, 80)
  } else {
    cursorInterval = setInterval(() => {
      cursorBlink.value = !cursorBlink.value
    }, 1000)
  }
}

onMounted(() => {
  const visited = sessionStorage.getItem('resume_visited')
  if (visited) {
    firstVisit.value = false
    subtitleText.value = fullSubtitle
    cursorBlink.value = false
    setTimeout(() => { cursorBlink.value = true }, 300)
  } else {
    sessionStorage.setItem('resume_visited', 'true')
  }
  setTimeout(typeSubtitle, 800)
})

onUnmounted(() => {
  if (cursorInterval) clearInterval(cursorInterval)
})
</script>

<style scoped lang="scss">
// 登录头部
$ink-black: #0a0a0a;
$ink-mid: #1f1f1f;
$ink-light: #2d2d2d;
$ink-pale: #404040;

.login-header {
  text-align: center;
  margin-bottom: 36px;
  position: relative;
  z-index: 1;

  .title {
    font-size: 42px;
    font-weight: 600;
    color: $ink-black;
    font-family: 'Noto Serif SC', 'STSong', 'Source Han Serif SC', 'SimSun', serif;
    margin-bottom: 16px;
    display: flex;
    justify-content: center;
    gap: 8px;
    cursor: default;
    letter-spacing: 4px;

    .char {
      display: inline-block;
      opacity: 0;
      animation: char-ink-in 0.8s cubic-bezier(0.4, 0, 0.2, 1) forwards;
      transition: all 0.3s ease;
      position: relative;
      font-weight: 700;

      &::after {
        content: '';
        position: absolute;
        bottom: -4px;
        left: 50%;
        width: 0;
        height: 2px;
        background: linear-gradient(90deg, transparent, $ink-mid, transparent);
        transition: all 0.3s ease;
        transform: translateX(-50%);
      }

      &.hover {
        transform: translateY(-3px);
        color: $ink-mid;

        &::after {
          width: 80%;
        }
      }

      &.skip-anim {
        opacity: 1;
        animation: none;
        transform: none;
        filter: none;
      }
    }
  }

  .subtitle {
    font-size: 13px;
    color: $ink-pale;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 2px;
    letter-spacing: 2px;

    .typing-text {
      min-height: 20px;
    }

    .cursor {
      color: $ink-light;
      opacity: 1;

      &.blink {
        animation: cursor-blink 1s infinite;
      }
    }
  }
}

@keyframes char-ink-in {
  0% {
    opacity: 0;
    transform: translateY(-30px) scale(1.2);
    filter: blur(4px);
  }
  50% {
    filter: blur(2px);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
    filter: blur(0);
  }
}

@keyframes cursor-blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}
</style>
