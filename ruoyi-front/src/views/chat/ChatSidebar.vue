<template>
  <aside class="chat-sidebar" :class="{ collapsed: !showSidebar }">
    <div class="sidebar-body">
      <div v-if="sessionsLoading" class="sidebar-loading">
        <div class="sidebar-skeleton" v-for="n in 4" :key="n">
          <div class="skeleton-line w-60"></div>
          <div class="skeleton-line w-30"></div>
        </div>
      </div>
      <div v-else-if="sessions.length === 0" class="sidebar-empty">
        <p>暂无历史对话</p>
      </div>
      <div v-else class="session-list">
        <button
          v-for="s in sessions"
          :key="s.id"
          :class="['session-item', { active: activeSessionId === s.id }]"
          @click="$emit('select-session', s.id)"
        >
          <div class="session-info">
            <span class="session-title">{{ s.sessionTitle || '新对话' }}</span>
            <span class="session-meta">
              <span class="session-scene">{{ sceneLabel(s.scene) }}</span>
              <span class="session-dot">·</span>
              <span class="session-time">{{ formatSessionTime(s.updateTime) }}</span>
            </span>
          </div>
          <button class="session-del" title="删除" @click.stop="$emit('delete-session', s.id)">
            <el-icon><Close /></el-icon>
          </button>
        </button>
      </div>
    </div>
  </aside>
  <button class="sidebar-toggle" @click="$emit('update:showSidebar', !showSidebar)">
    <span class="toggle-arrow">{{ showSidebar ? '◀' : '▶' }}</span>
  </button>
</template>

<script setup lang="ts">
import type { ChatSessionItem } from '@/types/chat'

defineProps<{
  sessions: ChatSessionItem[]
  sessionsLoading: boolean
  activeSessionId: number | null
  showSidebar: boolean
}>()

defineEmits<{
  'select-session': [id: number]
  'delete-session': [id: number]
  'update:showSidebar': [value: boolean]
}>()

function sceneLabel(scene: string) {
  const map: Record<string, string> = {
    default: '综合',
    resume: '简历诊断',
    'interview-hr': 'HR面试',
    'interview-pro': '专业面试',
    career: '职业规划',
  }
  return map[scene] || '综合'
}

function formatSessionTime(t: string) {
  if (!t) return ''
  const d = new Date(t)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>

<style scoped lang="scss">
$ink-deep: #141414;

.chat-sidebar {
  width: 280px;
  flex-shrink: 0;
  background: $ink-deep;
  display: flex;
  flex-direction: column;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  border-right: 1px solid rgba(255, 255, 255, 0.04);

  &.collapsed {
    width: 0;
    border-right: none;
  }
}

.sidebar-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 8px 12px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: rgba(255, 255, 255, 0.08); border-radius: 2px; }
}

.sidebar-loading { padding: 8px; }

.sidebar-skeleton {
  padding: 12px;
  margin-bottom: 8px;
  .skeleton-line {
    height: 12px;
    background: rgba(255, 255, 255, 0.06);
    border-radius: 4px;
    margin-bottom: 8px;
    animation: skeleton-pulse 1.5s ease-in-out infinite;
    &.w-60 { width: 60%; }
    &.w-30 { width: 30%; }
  }
}

@keyframes skeleton-pulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 0.8; }
}

.sidebar-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
  color: rgba(255, 255, 255, 0.3);
  font-size: 13px;
  font-family: 'Noto Serif SC', 'STSong', serif;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: all 0.15s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.06);
    .session-del { opacity: 1; }
  }
  &.active { background: rgba(255, 255, 255, 0.1); }

  .session-info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 3px;
  }
  .session-title {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.85);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    line-height: 1.3;
  }
  .session-meta {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 11px;
    color: rgba(255, 255, 255, 0.3);
  }
  .session-scene { color: rgba(255, 255, 255, 0.35); }
  .session-dot { opacity: 0.4; }

  .session-del {
    flex-shrink: 0;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
    border-radius: 4px;
    background: transparent;
    color: rgba(255, 255, 255, 0.25);
    cursor: pointer;
    opacity: 0;
    transition: all 0.15s ease;
    padding: 0;
    &:hover { background: rgba(255, 255, 255, 0.1); color: #f56c6c; }
    .el-icon { font-size: 14px; }
  }
}

.sidebar-toggle {
  position: absolute;
  left: 280px;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 48px;
  border: none;
  border-radius: 0 8px 8px 0;
  background: $ink-deep;
  color: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 5;
  &:hover {
    background: lighten($ink-deep, 5%);
    color: rgba(255, 255, 255, 0.7);
  }
  .toggle-arrow { font-size: 10px; line-height: 1; }
}

.chat-sidebar.collapsed + .sidebar-toggle {
  left: 0;
}
</style>
