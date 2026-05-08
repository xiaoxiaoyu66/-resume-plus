<template>
  <div :class="['sm-message', `sm-message--${role}`]">
    <div class="sm-message__avatar">
      <slot name="avatar">
        <el-icon :size="20">
          <UserFilled v-if="role === 'user'" />
          <MagicStick v-else />
        </el-icon>
      </slot>
    </div>
    <div class="sm-message__content">
      <div class="sm-message__bubble" v-html="content"></div>
      <div v-if="time" class="sm-message__time">{{ time }}</div>
    </div>
  </div>
</template>

<script setup>
/**
 * 水墨画风格消息气泡组件
 * @description 带有墨迹效果的聊天消息
 */
const props = defineProps({
  role: {
    type: String,
    default: 'user',
    validator: (val) => ['user', 'ai'].includes(val)
  },
  content: {
    type: String,
    default: ''
  },
  time: {
    type: String,
    default: ''
  }
})
</script>

<style scoped lang="scss">
@import './styles/variables.scss';
@import './styles/mixins.scss';

.sm-message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  animation: message-in 0.3s ease;

  &--user {
    flex-direction: row-reverse;

    .sm-message__avatar {
      background: linear-gradient(135deg, $ink-primary 0%, $ink-primary-light 100%);
      color: $paper-white;
    }

    .sm-message__bubble {
      background: linear-gradient(135deg, $ink-primary 0%, $ink-primary-light 100%);
      color: $paper-white;
      border-bottom-right-radius: 4px;
      animation: ink-dry 0.5s ease forwards;
    }

    .sm-message__time {
      color: rgba(249, 246, 240, 0.7);
      text-align: left;
    }
  }

  &--ai {
    .sm-message__avatar {
      background: linear-gradient(135deg, #0A1E3D 0%, #0E3265 100%);
      color: $paper-white;
    }

    .sm-message__bubble {
      background: $paper-white;
      color: $ink-deep;
      border: 1px solid $border-light;
      border-bottom-left-radius: 4px;
      position: relative;

      // 顶部水墨晕染装饰线
      &::before {
        content: '';
        position: absolute;
        top: -1px;
        left: 8px;
        right: 40%;
        height: 2px;
        background: linear-gradient(90deg, $ink-primary 0%, transparent 100%);
        border-radius: 1px;
        opacity: 0.3;
      }
    }

    .sm-message__time {
      color: $ink-light;
    }
  }

  &__avatar {
    width: 38px;
    height: 38px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    font-size: 16px;
    box-shadow: 0 2px 6px $shadow-soft;
  }

  &__content {
    max-width: 70%;
  }

  &__bubble {
    padding: 14px 18px;
    border-radius: 12px;
    line-height: 1.7;
    font-size: $font-size-base;
    word-break: break-word;
    box-shadow: 0 1px 4px $shadow-soft;
    white-space: pre-wrap;
  }

  &__time {
    font-size: 11px;
    margin-top: 6px;
    text-align: right;
    opacity: 0.8;
  }
}

@keyframes message-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes ink-dry {
  0% {
    filter: brightness(1.15);
    box-shadow: 0 2px 8px rgba(14, 50, 101, 0.2);
  }
  100% {
    filter: brightness(1);
    box-shadow: 0 1px 4px $shadow-soft;
  }
}
</style>
