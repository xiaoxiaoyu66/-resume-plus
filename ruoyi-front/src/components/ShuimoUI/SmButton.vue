<template>
  <button
    :class="['sm-button', `sm-button--${type}`, { 'sm-button--loading': loading }]"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <span v-if="loading" class="sm-button__loading">
      <span class="dot"></span>
      <span class="dot"></span>
      <span class="dot"></span>
    </span>
    <slot v-else />
  </button>
</template>

<script setup lang="ts">
/**
 * 水墨画风格按钮组件
 * @description 带有水墨涟漪效果的按钮
 */
const props = defineProps({
  type: {
    type: String,
    default: 'primary',
    validator: (val: unknown) => ['primary', 'secondary', 'accent', 'text'].includes(val as string)
  },
  loading: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

function handleClick(e: MouseEvent) {
  if (!props.loading && !props.disabled) {
    emit('click', e)
  }
}
</script>

<style scoped lang="scss">
@import './styles/variables.scss';
@import './styles/mixins.scss';

.sm-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 20px;
  font-size: $font-size-base;
  font-family: $font-family;
  font-weight: 500;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all $transition-base;
  overflow: hidden;
  border: none;
  outline: none;
  letter-spacing: 1px;

  // 水墨涟漪效果
  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    background: rgba(255, 255, 255, 0.3);
    border-radius: 50%;
    transform: translate(-50%, -50%);
    transition: width 0.6s, height 0.6s;
  }

  &:active::before {
    width: 300%;
    height: 300%;
  }

  // 主按钮 - 浓墨
  &--primary {
    background: linear-gradient(135deg, $ink-primary 0%, $ink-primary-light 100%);
    color: $paper-white;
    box-shadow: 0 2px 6px $shadow-mid;

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px $shadow-strong;
    }

    &:active:not(:disabled) {
      transform: translateY(0);
    }
  }

  // 次要按钮 - 淡墨
  &--secondary {
    background: transparent;
    color: $ink-mid;
    border: 1px solid $border-mid;

    &:hover:not(:disabled) {
      background: rgba(14, 50, 101, 0.05);
      border-color: $ink-light;
    }

    &::before {
      background: rgba(14, 50, 101, 0.1);
    }
  }

  // 强调按钮 - 朱砂
  &--accent {
    background: $accent-red;
    color: white;
    box-shadow: 0 2px 6px rgba(196, 92, 72, 0.3);

    &:hover:not(:disabled) {
      background: darken($accent-red, 8%);
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(196, 92, 72, 0.4);
    }

    &:active:not(:disabled) {
      transform: translateY(0);
    }
  }

  // 文字按钮
  &--text {
    background: transparent;
    color: $ink-mid;
    padding: 6px 12px;

    &:hover:not(:disabled) {
      color: $ink-primary;
      background: rgba(14, 50, 101, 0.05);
    }

    &::before {
      display: none;
    }
  }

  // 禁用状态
  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  // 加载状态
  &__loading {
    display: inline-flex;
    gap: 4px;
    align-items: center;

    .dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: currentColor;
      animation: ink-drop-think 1.4s infinite ease-in-out;

      &:nth-child(2) { animation-delay: 0.2s; }
      &:nth-child(3) { animation-delay: 0.4s; }
    }
  }
}

@keyframes ink-drop-think {
  0%, 80%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  40% {
    transform: translateY(-6px);
    opacity: 1;
  }
}
</style>
