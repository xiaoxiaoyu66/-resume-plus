<template>
  <div class="login-footer">
    <div class="divider">
      <span class="line line-left"></span>
      <span class="text">墨韵</span>
      <span class="line line-right"></span>
    </div>
    <p class="footer-text">
      <span v-for="(char, index) in chars" :key="index" class="footer-char" :style="{ animationDelay: index * 0.1 + 's' }">{{ char }}</span>
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const text = '笔墨纸砚 · 传承千年'
const chars = computed(() => text.split(''))
</script>

<style scoped lang="scss">
$ink-pale: #404040;

.login-footer {
  margin-top: 36px;
  text-align: center;
  position: relative;
  z-index: 1;

  .divider {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;

    .line {
      flex: 1;
      height: 1px;
      background: linear-gradient(90deg, transparent, rgba(0,0,0,0.15), transparent);
      position: relative;
      overflow: hidden;

      &::after {
        content: '';
        position: absolute;
        top: 0;
        left: -100%;
        width: 100%;
        height: 100%;
        background: linear-gradient(90deg, transparent, rgba(0,0,0,0.3), transparent);
        animation: line-shimmer 3s infinite;
      }
      &.line-right::after { animation-delay: 1.5s; }
    }
    .text {
      font-size: 12px;
      color: $ink-pale;
      font-family: 'Noto Serif SC', 'STSong', serif;
      letter-spacing: 6px;
    }
  }

  .footer-text {
    font-size: 11px;
    color: $ink-pale;
    font-family: 'Noto Serif SC', 'STSong', serif;
    letter-spacing: 3px;
    opacity: 0.7;

    .footer-char {
      display: inline-block;
      opacity: 0;
      animation: footer-char-in 0.5s ease forwards;
    }
  }
}

@keyframes line-shimmer { to { left: 100%; } }

@keyframes footer-char-in {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
