<template>
  <Transition name="diffusion">
    <div v-if="active" class="ink-diffusion">
      <div class="diffusion-ring ring-1"></div>
      <div class="diffusion-ring ring-2"></div>
      <div class="diffusion-ring ring-3"></div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
defineProps<{ active: boolean }>()
</script>

<style scoped lang="scss">
.ink-diffusion {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;

  .diffusion-ring {
    position: absolute;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(10,10,10,0.9) 0%, rgba(20,20,20,0.8) 40%, transparent 70%);
    animation: diffuse-out 1s cubic-bezier(0.4, 0, 0.2, 1) forwards;

    &.ring-1 { width: 200vmax; height: 200vmax; animation-delay: 0s; }
    &.ring-2 { width: 200vmax; height: 200vmax; animation-delay: 0.1s; opacity: 0.7; }
    &.ring-3 { width: 200vmax; height: 200vmax; animation-delay: 0.2s; opacity: 0.5; }
  }
}

@keyframes diffuse-out { to { transform: scale(1); } }

.diffusion-enter-active { transition: opacity 0.3s ease; }
.diffusion-leave-active { transition: opacity 0.5s ease; }
.diffusion-enter-from, .diffusion-leave-to { opacity: 0; }
</style>
