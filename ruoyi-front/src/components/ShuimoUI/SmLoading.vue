<template>
  <div class="sm-loading">
    <div class="ink-drop-container">
      <div class="ink-drop"></div>
      <div class="ink-ripple"></div>
    </div>
    <p class="loading-text">{{ text }}</p>
  </div>
</template>

<script setup lang="ts">
/**
 * 水墨画风格加载动画组件
 * @description "研墨构思中..." 水墨动画
 */
const props = defineProps({
  text: {
    type: String,
    default: '研墨构思中...'
  }
})
</script>

<style scoped lang="scss">
@import './styles/variables.scss';
@import './styles/mixins.scss';

.sm-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 20px;
}

.ink-drop-container {
  position: relative;
  width: 60px;
  height: 60px;
}

.ink-drop {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 12px;
  height: 12px;
  background: $ink-primary;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  animation: ink-drop-fall 2s ease-in-out infinite;
}

.ink-ripple {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 12px;
  height: 12px;
  border: 2px solid $ink-primary;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  animation: ink-ripple-spread 2s ease-out infinite;
  opacity: 0;
}

.loading-text {
  font-size: $font-size-md;
  color: $ink-mid;
  font-family: $font-family;
  letter-spacing: 2px;
  animation: text-fade 2s ease-in-out infinite;
}

@keyframes ink-drop-fall {
  0% {
    transform: translate(-50%, -150%);
    opacity: 0;
  }
  30% {
    transform: translate(-50%, -50%);
    opacity: 1;
  }
  50% {
    transform: translate(-50%, -50%) scale(1);
  }
  70% {
    transform: translate(-50%, -50%) scale(1.5);
    opacity: 0.5;
  }
  100% {
    transform: translate(-50%, -50%) scale(2);
    opacity: 0;
  }
}

@keyframes ink-ripple-spread {
  0% {
    width: 12px;
    height: 12px;
    opacity: 0.8;
  }
  100% {
    width: 60px;
    height: 60px;
    opacity: 0;
  }
}

@keyframes text-fade {
  0%, 100% {
    opacity: 0.6;
  }
  50% {
    opacity: 1;
  }
}
</style>
