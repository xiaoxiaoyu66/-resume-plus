<template>
  <div v-if="props.loadingHistory" class="loading-history">
    <el-icon class="loading-spin"><Loading /></el-icon>
    <span>加载对话历史...</span>
    <div v-if="props.loadingTimedOut" class="loading-timeout">
      <span>加载时间过长，请检查网络后</span>
      <el-button size="small" text @click="$emit('retry')">重试</el-button>
    </div>
  </div>
  <div v-else class="message-list" ref="messageListRef">
    <div v-if="props.messages.length === 0" class="history-empty-hint">
      <p>暂无对话记录，发送消息开始对话</p>
    </div>
    <div v-for="(msg, index) in props.messages" :key="index">
      <div v-if="shouldShowDateSeparator(index)" class="date-separator">
        <span class="date-separator-text">{{ formatDateSeparator(msg) }}</span>
      </div>
      <div :class="['message-item', msg.role]">
        <div class="message-avatar">
          <InkAvatar v-if="msg.role === 'ai'" />
          <div v-else class="avatar user-avatar">
            <el-icon><UserFilled /></el-icon>
          </div>
        </div>
        <div class="message-content">
          <div class="message-bubble">
            <template v-if="msg.role === 'ai'">
              <MarkdownRenderer :content="msg.content" />
            </template>
            <template v-else>
              {{ msg.content }}
            </template>
          </div>
          <span class="message-time">{{ formatMessageTime(msg.time) }}</span>
        </div>
      </div>
    </div>
    <div v-if="props.isThinking" class="message-item ai thinking">
      <div class="message-avatar">
        <InkAvatar state="thinking" />
      </div>
      <div class="message-content">
        <div class="thinking-bubble">
          <div class="thinking-steps">
            <div class="thinking-step"><span class="step-dot active"></span><span class="step-label">理解问题</span></div>
            <div class="thinking-step"><span class="step-dot pulse"></span><span class="step-label">分析中</span></div>
            <div class="thinking-step"><span class="step-dot"></span><span class="step-label">生成回答</span></div>
          </div>
          <span class="thinking-text">简历+ 思考中...</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import InkAvatar from '@/components/chat/InkAvatar.vue'
import type { ChatMessage } from '@/types/chat'

const props = defineProps<{
  messages: ChatMessage[]
  isThinking: boolean
  loadingHistory: boolean
  loadingTimedOut: boolean
}>()

defineEmits<{
  retry: []
}>()

const messageListRef = ref<HTMLElement | null>(null)

function shouldShowDateSeparator(index: number): boolean {
  if (index === 0) return true
  const msgs = props.messages
  const prev = msgs[index - 1]
  const curr = msgs[index]
  if (!prev || !curr) return false
  const prevDate = new Date(prev.time || 0)
  const currDate = new Date(curr.time || 0)
  return prevDate.toDateString() !== currDate.toDateString()
}

function formatDateSeparator(msg: ChatMessage): string {
  const d = new Date(msg.time || 0)
  const now = new Date()
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (d.toDateString() === now.toDateString()) return '今天'
  if (d.toDateString() === yesterday.toDateString()) return '昨天'
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}年${pad(d.getMonth() + 1)}月${pad(d.getDate())}日`
}

/** 从完整时间中提取 HH:mm 用于气泡显示 */
function formatMessageTime(time: string): string {
  if (!time) return ''
  // 如果是完整格式 "2026-05-11 09:05:00" → 取 09:05
  const m = time.match(/(\d{2}:\d{2})/)
  return m ? m[1] : time
}

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

function scrollToBottomDelayed() {
  scrollToBottom()
  setTimeout(() => scrollToBottom(), 100)
}

defineExpose({ scrollToBottom, scrollToBottomDelayed })
</script>

<style scoped lang="scss">
$ink-black: #0a0a0a;
$ink-deep: #141414;
$ink-mid: #1f1f1f;
$ink-light: #2d2d2d;
$ink-pale: #5a5a5a;
$paper-white: #ffffff;
$paper-gray: #f5f5f5;

/* ---- 加载中 ---- */
.loading-history {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: $ink-pale;
  font-size: 14px;
  .loading-spin {
    font-size: 28px;
    animation: loading-rotate 1s linear infinite;
  }
}

@keyframes loading-rotate {
  to { transform: rotate(360deg); }
}

/* ---- 加载超时提示 ---- */
.loading-timeout {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 8px 16px;
  background: rgba(245, 108, 108, 0.06);
  border: 1px solid rgba(245, 108, 108, 0.15);
  border-radius: 8px;
  font-size: 13px;
  color: #f56c6c;
}

/* ---- 日期分隔 ---- */
.date-separator {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 0 12px;
  position: relative;
  &::before {
    content: '';
    position: absolute;
    left: 0; right: 0; top: 50%;
    height: 1px;
    background: rgba(0, 0, 0, 0.06);
  }
  .date-separator-text {
    position: relative;
    z-index: 1;
    padding: 0 16px;
    font-size: 12px;
    color: #bbb;
    background: $paper-white;
  }
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px 0;

  .history-empty-hint {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 60px 20px;
    color: #999;
    font-size: 14px;
  }
}

.message-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  animation: message-in 0.3s ease;

  &.user {
    flex-direction: row-reverse;
    .message-content { align-items: flex-end; }
    .message-bubble {
      background: $ink-black;
      color: $paper-white;
      border-radius: 16px 16px 4px 16px;
    }
  }
  &.ai {
    .message-bubble {
      background: $paper-gray;
      color: $ink-black;
      border-radius: 16px 16px 16px 4px;
    }
  }

  .message-avatar {
    flex-shrink: 0;
    .avatar {
      width: 40px; height: 40px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;

      &.user-avatar {
        background: linear-gradient(135deg, $ink-light 0%, $ink-mid 100%);
        color: $paper-white;
      }
    }
  }

  .message-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 4px;
    max-width: calc(100% - 60px);
    .message-bubble {
      padding: 12px 16px;
      font-size: 15px;
      line-height: 1.6;
      word-wrap: break-word;
    }
    .message-time { font-size: 12px; color: $ink-pale; }
  }
}

/* ---- thinking ---- */
.thinking .thinking-bubble {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px 20px;
  background: $paper-gray;
  border-radius: 16px 16px 16px 4px;
  min-width: 180px;

  .thinking-steps {
    display: flex;
    gap: 16px;
    .thinking-step {
      display: flex;
      align-items: center;
      gap: 5px;
      .step-dot {
        width: 7px; height: 7px;
        border-radius: 50%;
        background: #ddd;
        transition: all 0.3s ease;
        &.active {
          background: #3b82f6;
          box-shadow: 0 0 6px rgba(59, 130, 246, 0.5);
        }
        &.pulse {
          background: #3b82f6;
          animation: step-pulse 1s ease-in-out infinite;
        }
      }
      .step-label { font-size: 12px; color: #999; }
    }
  }
  .thinking-text {
    font-size: 13px;
    color: $ink-pale;
    font-family: 'Noto Serif SC', 'STSong', serif;
    margin-left: 1px;
  }
}

@keyframes step-pulse {
  0%, 100% { opacity: 0.5; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
}

@keyframes message-in {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
