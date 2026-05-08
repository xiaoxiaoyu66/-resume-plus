<template>
  <svg 
    :class="['ink-icon', sizeClass]" 
    :style="iconStyle"
    aria-hidden="true"
  >
    <use :href="iconHref" />
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 图标名称，如 'chat', 'user', 'file' 等
  name: {
    type: String,
    required: true
  },
  // 图标尺寸: 'small', 'medium', 'large', 或数字
  size: {
    type: [String, Number],
    default: 'medium'
  },
  // 自定义颜色，不填则使用默认靛蓝色
  color: {
    type: String,
    default: ''
  },
  // 旋转角度
  rotate: {
    type: Number,
    default: 0
  }
})

// 图标尺寸映射
const sizeMap = {
  'small': 16,
  'medium': 24,
  'large': 32,
  'xlarge': 48
}

// 计算尺寸
const iconSize = computed(() => {
  if (typeof props.size === 'number') {
    return props.size
  }
  return sizeMap[props.size] || 24
})

// 尺寸类名
const sizeClass = computed(() => {
  if (typeof props.size === 'string' && sizeMap[props.size]) {
    return `size-${props.size}`
  }
  return ''
})

// 图标样式
const iconStyle = computed(() => {
  const style = {
    width: `${iconSize.value}px`,
    height: `${iconSize.value}px`
  }
  
  if (props.color) {
    style.color = props.color
  }
  
  if (props.rotate) {
    style.transform = `rotate(${props.rotate}deg)`
  }
  
  return style
})

// 图标引用路径
const iconHref = computed(() => {
  // 自动添加 ink- 前缀
  const iconName = props.name.startsWith('ink-') ? props.name : `ink-${props.name}`
  return `#${iconName}`
})
</script>

<style scoped>
.ink-icon {
  display: inline-block;
  vertical-align: middle;
  fill: currentColor;
  overflow: hidden;
  transition: all 0.3s ease;
}

/* 预设尺寸 */
.size-small {
  width: 16px;
  height: 16px;
}

.size-medium {
  width: 24px;
  height: 24px;
}

.size-large {
  width: 32px;
  height: 32px;
}

.size-xlarge {
  width: 48px;
  height: 48px;
}
</style>
