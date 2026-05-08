<template>
  <div :class="['sm-card', { 'sm-card--hoverable': hoverable, 'sm-card--scroll': scroll }]" :style="cardStyle">
    <div v-if="$slots.header || title" class="sm-card__header">
      <div class="header-wrapper">
        <slot name="header">
          <div class="header-content">
            <span class="title">{{ title }}</span>
          </div>
        </slot>
        <span v-if="seal" class="seal">{{ seal }}</span>
      </div>
    </div>
    <div class="sm-card__body" :style="bodyStyle">
      <slot />
    </div>
    <div v-if="$slots.footer" class="sm-card__footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

/**
 * 水墨画风格卡片组件
 * @description 带有卷轴装饰效果的卡片
 */
const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  seal: {
    type: String,
    default: ''
  },
  hoverable: {
    type: Boolean,
    default: true
  },
  scroll: {
    type: Boolean,
    default: false
  },
  padding: {
    type: [String, Number],
    default: '24px'
  },
  maxHeight: {
    type: String,
    default: ''
  }
})

const cardStyle = computed(() => {
  const style = {}
  if (props.maxHeight) {
    style.maxHeight = props.maxHeight
  }
  return style
})

const bodyStyle = computed(() => {
  const style = {}
  if (typeof props.padding === 'number') {
    style.padding = `${props.padding}px`
  } else {
    style.padding = props.padding
  }
  return style
})
</script>

<style scoped lang="scss">
@import './styles/variables.scss';
@import './styles/mixins.scss';

.sm-card {
  background: $paper-white;
  border: 1px solid $border-light;
  border-radius: $radius-md;
  box-shadow: 0 2px 8px $shadow-soft;
  transition: all $transition-base;
  overflow: hidden;
  position: relative;

  // 卷轴装饰
  &--scroll {
    &::before,
    &::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      height: 3px;
      background: linear-gradient(90deg, $ink-primary 0%, transparent 15%, transparent 85%, $ink-primary 100%);
      opacity: 0.15;
    }

    &::before { top: 0; }
    &::after { bottom: 0; }
  }

  // 悬停效果
  &--hoverable:hover {
    box-shadow: 0 4px 16px $shadow-mid;
    transform: translateY(-2px);
  }

  &__header {
    padding: 16px 24px;
    border-bottom: 1px solid $border-light;
    background: linear-gradient(180deg, $paper-light 0%, $paper-white 100%);

    .header-wrapper {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;

      .header-content {
        flex: 1;

        .title {
          font-size: $font-size-lg;
          font-weight: 600;
          color: $ink-deep;
          font-family: $font-family;
          letter-spacing: 2px;
        }
      }

      .seal {
        @include seal($accent-red, 28px);
        flex-shrink: 0;
      }
    }
  }

  &__body {
    @include ink-wash-bg(180deg, 0.03);
  }

  &__footer {
    padding: 12px 24px;
    border-top: 1px solid $border-light;
    background: $paper-light;
  }
}
</style>
