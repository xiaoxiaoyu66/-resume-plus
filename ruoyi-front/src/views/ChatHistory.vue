<template>
  <div class="chat-history-page">
    <div class="page-header">
      <div class="header-left">
        <el-button class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h1 class="page-title">历史会话</h1>
      </div>
      <div class="header-right">
        <div class="search-box">
          <el-icon><Search /></el-icon>
          <input 
            v-model="searchKeyword"
            type="text" 
            placeholder="搜索历史会话"
            @input="handleSearch"
          />
        </div>
      </div>
    </div>

    <div class="history-content">
      <!-- 按日期分组显示 -->
      <div v-if="filteredHistory.length > 0" class="history-groups">
        <div 
          v-for="group in groupedHistory" 
          :key="group.date"
          class="history-group"
        >
          <div class="group-title">{{ group.label }}</div>
          <div class="history-cards">
            <div 
              v-for="session in group.sessions" 
              :key="session.id"
              class="history-card"
              @click="goToChat(session.id)"
            >
              <div class="card-content">
                <div class="card-header">
                  <h4 class="card-title">{{ session.sessionTitle || '新对话' }}</h4>
                  <span class="card-time">{{ formatTime(session.createTime) }}</span>
                </div>
                <p class="card-preview">{{ getPreviewText(session) }}</p>
              </div>
              <div class="card-actions">
                <el-icon class="delete-icon" @click.stop="deleteSession(session.id)">
                  <Delete />
                </el-icon>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <el-icon class="empty-icon"><ChatLineRound /></el-icon>
        <p class="empty-text">暂无历史会话</p>
        <el-button type="primary" @click="goToNewChat">
          开始新对话
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Search, Delete, ChatLineRound } from '@element-plus/icons-vue'
import { listHistory, delSession } from '@/api/chat'

const router = useRouter()
const route = useRoute()
const chatHistory = ref([])
const searchKeyword = ref('')

// 加载历史对话
async function loadChatHistory() {
  try {
    const res = await listHistory()
    chatHistory.value = res.data || []
  } catch (e) {
    console.error('加载历史对话失败', e)
    ElMessage.error('加载历史对话失败')
  }
}

// 过滤后的历史对话
const filteredHistory = computed(() => {
  if (!searchKeyword.value.trim()) {
    return chatHistory.value
  }
  const keyword = searchKeyword.value.toLowerCase()
  return chatHistory.value.filter(session => 
    (session.sessionTitle || '').toLowerCase().includes(keyword) ||
    (session.lastMessage || '').toLowerCase().includes(keyword)
  )
})

// 按日期分组
const groupedHistory = computed(() => {
  const groups = {}
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000)
  const weekAgo = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000)

  filteredHistory.value.forEach(session => {
    const sessionDate = new Date(session.createTime)
    let groupKey
    let groupLabel

    if (sessionDate >= today) {
      groupKey = 'today'
      groupLabel = '今天'
    } else if (sessionDate >= yesterday) {
      groupKey = 'yesterday'
      groupLabel = '昨天'
    } else if (sessionDate >= weekAgo) {
      groupKey = 'week'
      groupLabel = '本周'
    } else {
      groupKey = 'earlier'
      groupLabel = '更早'
    }

    if (!groups[groupKey]) {
      groups[groupKey] = {
        date: groupKey,
        label: groupLabel,
        sessions: []
      }
    }
    groups[groupKey].sessions.push(session)
  })

  // 按时间顺序排序
  return Object.values(groups).sort((a, b) => {
    const order = { today: 0, yesterday: 1, week: 2, earlier: 3 }
    return order[a.date] - order[b.date]
  })
})

// 搜索处理
function handleSearch() {
  // 搜索逻辑已在计算属性中实现
}

// 获取预览文本
function getPreviewText(session) {
  if (session.lastMessage) {
    return truncateText(session.lastMessage, 80)
  }
  return '暂无消息内容'
}

// 截断文本
function truncateText(text, length) {
  if (!text) return ''
  if (text.length <= length) return text
  return text.substring(0, length) + '...'
}

// 格式化时间
function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000)
  const sessionDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())

  if (sessionDate.getTime() === today.getTime()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else if (sessionDate.getTime() === yesterday.getTime()) {
    return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else {
    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }) + ' ' +
           date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
}

// 返回上一页
function goBack() {
  router.back()
}

// 跳转到对话详情
function goToChat(sessionId) {
  router.push(`/chat?sessionId=${sessionId}`)
}

// 开始新对话
function goToNewChat() {
  router.push('/chat')
}

// 删除会话
async function deleteSession(sessionId) {
  try {
    await ElMessageBox.confirm('确定要删除这个会话吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await delSession(sessionId)
    ElMessage.success('删除成功')
    // 重新加载列表
    loadChatHistory()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除会话失败', e)
      ElMessage.error('删除失败')
    }
  }
}

// 每次进入页面都重新加载历史数据
watch(() => route.fullPath, () => {
  loadChatHistory()
}, { immediate: true })
</script>

<style scoped lang="scss">
.chat-history-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f8f8f6;
  padding: 24px 32px;
  overflow: hidden;
  box-sizing: border-box;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .back-btn {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      border: 1px solid rgba(0, 0, 0, 0.1);
      background: #fff;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        background: rgba(0, 0, 0, 0.04);
      }
    }

    .page-title {
      font-size: 24px;
      font-weight: 600;
      color: #0a0a0a;
      margin: 0;
    }
  }

  .header-right {
    .search-box {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 16px;
      background: #fff;
      border-radius: 8px;
      border: 1px solid rgba(0, 0, 0, 0.1);
      width: 280px;

      input {
        flex: 1;
        border: none;
        background: transparent;
        font-size: 14px;
        color: #333;
        outline: none;

        &::placeholder {
          color: #999;
        }
      }
    }
  }
}

.history-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;

  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  &::-webkit-scrollbar-thumb {
    background: #ddd;
    border-radius: 2px;
  }
}

.history-groups {
  .history-group {
    margin-bottom: 32px;

    .group-title {
      font-size: 14px;
      font-weight: 600;
      color: #666;
      margin-bottom: 16px;
      padding-left: 8px;
    }
  }
}

.history-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    transform: translateY(-2px);
  }

  &:active {
    transform: translateY(0);
  }

  .card-content {
    flex: 1;
    min-width: 0;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;

      .card-title {
        font-size: 16px;
        font-weight: 600;
        color: #0a0a0a;
        margin: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .card-time {
        font-size: 13px;
        color: #999;
        flex-shrink: 0;
      }
    }

    .card-preview {
      font-size: 14px;
      color: #666;
      line-height: 1.6;
      margin: 0;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  }

  .card-actions {
    opacity: 0;
    transition: opacity 0.2s ease;

    .delete-icon {
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 6px;
      color: #999;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        background: rgba(245, 108, 108, 0.1);
        color: #f56c6c;
      }
    }
  }

  &:hover .card-actions {
    opacity: 1;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;

  .empty-icon {
    font-size: 64px;
    color: #ddd;
    margin-bottom: 16px;
  }

  .empty-text {
    font-size: 16px;
    color: #999;
    margin-bottom: 24px;
  }
}

@media (max-width: 768px) {
  .chat-history-page {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;

    .header-right {
      width: 100%;

      .search-box {
        width: 100%;
      }
    }
  }
}
</style>
