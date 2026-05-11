<template>
  <div class="app-wrapper" :class="{ 'login-page': isLoginPage }">
    <!-- 侧边导航 -->
    <aside v-if="!isLoginPage" class="sidebar">
      <div class="sidebar-header">
        <!-- 简历+ Logo -->
        <div class="logo-lishizhen">
          <span class="logo-char">简</span>
          <span class="logo-char">历</span>
          <span class="logo-char">+</span>
        </div>

        <button class="new-chat-btn" @click="goToNewChat">
          <svg class="new-chat-icon" viewBox="0 0 16 16" fill="none">
            <path d="M8 3v10M3 8h10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
          <span>新建对话</span>
        </button>

      </div>

      <nav class="sidebar-nav">
        <div 
          :class="['nav-item', { active: $route.path === '/chat' && !currentSessionId }]" 
          @click="goToNewChat"
        >
          <div class="nav-icon">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <span class="nav-label">AI 助手</span>
          <div class="nav-ink"></div>
        </div>

        <!-- 历史对话列表 - 只显示最新的3条 -->
        <div class="history-section" v-if="chatHistory.length > 0">
          <div class="history-header">
            <div class="history-title">历史对话</div>
          </div>
          <div class="history-list">
            <div 
              v-for="session in recentHistory" 
              :key="session.id"
              :class="['history-item', { active: currentSessionId === session.id }]"
              @click="goToChat(session.id)"
            >
              <el-icon><ChatLineRound /></el-icon>
              <span class="history-text">{{ session.sessionTitle || '新对话' }}</span>
              <el-icon class="delete-icon" @click.stop="deleteSession(session.id)"><Close /></el-icon>
            </div>
          </div>
          <!-- 查看全部按钮 -->
          <div v-if="chatHistory.length > 3" class="view-all-btn" @click="goToHistoryPage">
            <span>查看全部</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>

        <router-link to="/resume" :class="{ active: $route.path.startsWith('/resume') }" class="nav-item">
          <div class="nav-icon">
            <el-icon><EditPen /></el-icon>
          </div>
          <span class="nav-label">简历编辑器</span>
          <div class="nav-ink"></div>
        </router-link>

        <!-- 模拟面试 -->
        <router-link to="/interview" :class="{ active: $route.path.startsWith('/interview') }" class="nav-item">
          <div class="nav-icon">
            <el-icon><ChatDotSquare /></el-icon>
          </div>
          <span class="nav-label">模拟面试</span>
          <div class="nav-ink"></div>
        </router-link>

        <!-- 江城聘 -->
        <router-link
          to="/jiangcheng"
          :class="['nav-item', { active: $route.path === '/jiangcheng' }]"
        >
          <div class="nav-icon">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <span class="nav-label">江城聘</span>
          <span v-if="jobsStore.matchResults.length > 0" class="nav-badge">{{ jobsStore.matchResults.length }}</span>
          <div class="nav-ink"></div>
        </router-link>

        <!-- 岗位推荐列表（紧凑） -->
        <div v-if="jobsStore.matchResults.length > 0" class="jobs-section">
            <div class="jobs-list">
              <div
                v-for="(item, i) in jobsStore.matchResults.slice(0, 2)"
                :key="i"
                class="job-item"
                @click="goToJiangcheng"
              >
                <span class="job-score-badge">{{ Math.round(item.score * 100) }}</span>
                <div class="job-info">
                  <div class="job-title">{{ item.job.title }}</div>
                  <div class="job-company">{{ item.job.company }}</div>
                </div>
              </div>
            </div>
            <div class="jobs-view-all-inline" @click="goToJiangcheng">
              全部 {{ jobsStore.matchResults.length }} 个岗位 <el-icon><ArrowRight /></el-icon>
            </div>
        </div>

        <!-- 个人空间按钮 -->
        <div
          :class="['nav-item', { active: isPersonalSpaceOpen }]"
          @click="openPersonalSpace"
        >
          <div class="nav-icon">
            <el-icon><FolderOpened /></el-icon>
          </div>
          <span class="nav-label">个人空间</span>
          <div class="nav-ink"></div>
        </div>
      </nav>

      <div class="sidebar-footer">
        <div class="user-info" @click="goToUserInfo">
          <div class="user-avatar">
            <el-icon><UserFilled /></el-icon>
          </div>
          <span class="user-name">{{ userStore.name || '用户' }}</span>
          <el-icon class="user-arrow"><ArrowRight /></el-icon>
        </div>
        <button class="logout-btn" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          <span>退出登录</span>
          <div class="logout-ink"></div>
        </button>
      </div>
    </aside>

    <!-- 个人空间侧边栏 -->
    <PersonalSpace 
      :is-open="isPersonalSpaceOpen"
      @close="isPersonalSpaceOpen = false"
      @use-file="handleUseFile"
    />

    <!-- 主内容区 -->
    <main class="main-content" :class="{ 'full-height': isLoginPage }">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, watch, provide } from 'vue'
import {
  ChatDotRound, ChatDotSquare, ChatLineRound, UserFilled, Document,
  ArrowRight, SwitchButton, Close, FolderOpened, Briefcase, EditPen,
  TrendCharts
} from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useJobsStore } from '@/store/jobs'
import { listHistory, delSession } from '@/api/chat'
import { getToken } from '@/utils/auth'
import PersonalSpace from '@/components/PersonalSpace.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const jobsStore = useJobsStore()

const isLoginPage = computed(() => route.path === '/login')
const chatHistory = ref<any[]>([])
const currentSessionId = ref(null)
const newChatCounter = ref(0)
provide('newChatCounter', newChatCounter)

// 只显示最新的3条历史对话
const recentHistory = computed(() => {
  // 按时间倒序排序，取前3条
  return [...chatHistory.value]
    .sort((a, b) => Number(new Date(b.createTime)) - Number(new Date(a.createTime)))
    .slice(0, 3)
})

// 个人空间状态
const isPersonalSpaceOpen = ref(false)
const selectedFileFromSpace = ref(null)

function goToJiangcheng() {
  router.push('/jiangcheng')
}

// 打开个人空间
function openPersonalSpace() {
  isPersonalSpaceOpen.value = true
}

// 处理从个人空间选择的文件
function handleUseFile(file) {
  selectedFileFromSpace.value = file
  // 触发一个事件通知Chat组件
  window.dispatchEvent(new CustomEvent('use-file-from-space', { detail: file }))
}

// 提供给子组件使用
provide('openPersonalSpace', openPersonalSpace)

// 加载历史对话
async function loadChatHistory() {
  // 检查是否有token，避免不必要的请求
  if (!getToken()) {
    return
  }
  try {
    const res = await listHistory()
    chatHistory.value = (res.data as any[]) || []
  } catch (e) {
    // 401错误已经在request拦截器中处理，这里不再处理
    console.error('加载历史对话失败', e)
  }
}

// 跳转到指定对话
function goToChat(sessionId) {
  currentSessionId.value = sessionId
  router.push({
    path: '/chat',
    query: { sessionId }
  })
}

// 新建对话（点击 AI 助手）
function goToNewChat() {
  currentSessionId.value = null
  newChatCounter.value++
  router.push({
    path: '/chat'
  })
}

// 跳转到历史记录页面
function goToHistoryPage() {
  router.push('/chat/history')
}

// 删除会话
async function deleteSession(sessionId) {
  try {
    await ElMessageBox.confirm('确定要删除这个对话吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await delSession(sessionId)
    ElMessage.success('删除成功')
    loadChatHistory()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败', e)
    }
  }
}

// 跳转到个人信息页
function goToUserInfo() {
  router.push('/user-info')
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch (e) {
    // 取消退出
  }
}

onMounted(() => {
  if (!isLoginPage.value) {
    loadChatHistory()
  }
})
</script>

<style scoped lang="scss">
// 深海蓝主题 - 统一海洋蓝风格
$navy-deep: #0f1d38;
$navy-mid: #15294f;
$navy-light: #1e3a6f;
$navy-text: rgba(255, 255, 255, 0.85);
$navy-text-secondary: rgba(255, 255, 255, 0.55);
$navy-text-muted: rgba(255, 255, 255, 0.35);
$paper-white: #ffffff;
$paper-cream: #f8f8f6;
$accent-blue: #3b82f6;
$accent-blue-light: rgba(59, 130, 246, 0.15);

.app-wrapper {
  min-height: 100vh;
  display: flex;

  &.login-page {
    display: block;
  }
}

// 侧边导航 - 深海蓝
.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #15294f 0%, #0a1a33 100%);
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 100;
  box-shadow: 4px 0 20px rgba(15, 29, 56, 0.5);
  border-right: 1px solid rgba(255, 255, 255, 0.06);

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(59, 130, 246, 0.2), transparent);
  }
}

// 侧边栏头部
.sidebar-header {
  padding: 32px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);

  .logo {
    position: relative;
    display: inline-block;

    .logo-text {
      font-size: 36px;
      font-weight: 700;
      color: $paper-white;
      font-family: 'Noto Serif SC', 'STSong', serif;
      letter-spacing: 8px;
      position: relative;
      z-index: 2;
    }

    .logo-ink {
      position: absolute;
      bottom: 4px;
      left: 0;
      right: 0;
      height: 8px;
      background: linear-gradient(90deg, transparent, rgba(59, 130, 246, 0.5), transparent);
      filter: blur(4px);
      z-index: 1;
    }
  }

  /* 简历+ Logo */
  .logo-lishizhen {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    letter-spacing: 3px;

    .logo-char {
      font-size: 28px;
      font-weight: 700;
      color: $paper-white;
      font-family: 'Noto Serif SC', 'STSong', serif;
      line-height: 1;
    }
  }

  .new-chat-btn {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    margin-top: 20px;
    padding: 10px 16px;
    background: transparent;
    border: 1px solid rgba(255, 255, 255, 0.15);
    border-radius: 8px;
    color: rgba(255, 255, 255, 0.7);
    font-size: 14px;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
    cursor: pointer;
    transition: all 0.25s ease;

    .new-chat-icon {
      width: 16px;
      height: 16px;
      flex-shrink: 0;
    }

    &:hover {
      border-color: $accent-blue;
      color: $accent-blue;
      background: rgba(196, 92, 72, 0.08);
    }
  }
}

// 导航菜单
.sidebar-nav {
  flex: 1;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;

  // 历史对话区域（可滚动）
  .history-section {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid rgba(255, 255, 255, 0.06);
    max-height: 280px;
    display: flex;
    flex-direction: column;

    .history-title {
      font-size: 12px;
      color: rgba(255, 255, 255, 0.5);
      padding: 0 12px 8px;
      font-weight: 500;
      text-transform: uppercase;
      letter-spacing: 1px;
      flex-shrink: 0;
    }

    .history-list {
      display: flex;
      flex-direction: column;
      gap: 2px;
      overflow-y: auto;
      flex: 1;
      min-height: 0;

      &::-webkit-scrollbar { width: 3px; }
      &::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 2px; }

      .history-item {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 12px;
        border-radius: 6px;
        color: rgba(255, 255, 255, 0.75);
        font-size: 13px;
        cursor: pointer;
        transition: all 0.3s ease;
        position: relative;

        .el-icon { font-size: 14px; flex-shrink: 0; }

        .history-text {
          flex: 1;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .delete-icon {
          opacity: 0;
          transition: all 0.3s ease;
          font-size: 12px;
          padding: 4px;
          border-radius: 4px;
          color: #ff6b6b;
          background: rgba(255, 107, 107, 0.15);
          flex-shrink: 0;

          &:hover {
            background: rgba(255, 107, 107, 0.35);
            color: #ff4757;
            opacity: 1;
          }
        }

        &:hover {
          background: rgba(255, 255, 255, 0.1);
          color: rgba(255, 255, 255, 0.95);

          .delete-icon {
            opacity: 1;
            background: rgba(255, 255, 255, 0.1);
          }
        }

        &.active {
          background: rgba(255, 255, 255, 0.12);
          color: $paper-white;

          &::before {
            content: '';
            position: absolute;
            left: 0;
            top: 50%;
            transform: translateY(-50%);
            width: 3px;
            height: 16px;
            background: #3b82f6;
            border-radius: 0 2px 2px 0;
          }

          .delete-icon { opacity: 0.8; }
        }
      }
    }

    // 查看全部按钮
    .view-all-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      padding: 8px 12px;
      margin-top: 4px;
      border-radius: 6px;
      color: rgba(255, 255, 255, 0.5);
      font-size: 12px;
      cursor: pointer;
      transition: all 0.3s ease;
      flex-shrink: 0;

      &:hover {
        background: rgba(59, 130, 246, 0.15);
        color: #93c5fd;
      }

      .el-icon { font-size: 12px; }
    }
  }

  // nav-badge
  .nav-badge {
    margin-left: auto;
    min-width: 20px;
    height: 20px;
    padding: 0 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(59, 130, 246, 0.2);
    color: #93c5fd;
    border-radius: 10px;
    font-size: 11px;
    font-weight: 600;
  }

  // 岗位推荐（紧凑）
  .jobs-section {
    margin: 0 12px 8px;
    padding: 8px 10px;
    background: rgba(255, 255, 255, 0.03);
    border-radius: 8px;

    .jobs-empty {
      text-align: center;
      padding: 12px 0;
      font-size: 12px;
      color: rgba(255, 255, 255, 0.35);
    }

    .jobs-list {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .job-item {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 8px;
        border-radius: 6px;
        cursor: pointer;
        transition: background 0.2s;

        &:hover {
          background: rgba(255, 255, 255, 0.06);
        }

        .job-score-badge {
          width: 24px;
          height: 24px;
          flex-shrink: 0;
          display: flex;
          align-items: center;
          justify-content: center;
          background: $accent-blue-light;
          color: #93c5fd;
          font-size: 10px;
          font-weight: 700;
          border-radius: 6px;
        }

        .job-info {
          flex: 1;
          min-width: 0;

          .job-title {
            font-size: 12px;
            color: rgba(255, 255, 255, 0.85);
            font-weight: 500;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .job-company {
            font-size: 11px;
            color: rgba(255, 255, 255, 0.35);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            margin-top: 1px;
          }
        }
      }
    }

    .jobs-view-all-inline {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 4px;
      padding: 8px;
      margin-top: 4px;
      font-size: 12px;
      color: rgba(255, 255, 255, 0.45);
      cursor: pointer;
      border-radius: 6px;
      transition: all 0.2s;

      &:hover {
        color: #93c5fd;
        background: rgba(255, 255, 255, 0.04);
      }

      .el-icon { font-size: 11px; }
    }
  }

.nav-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 16px;
    border-radius: 8px;
    color: rgba(255, 255, 255, 0.8);
    text-decoration: none;
    font-size: 15px;
    font-weight: 500;
    transition: all 0.3s ease;
    position: relative;
    overflow: hidden;

    .nav-icon {
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 8px;
      background: rgba(255, 255, 255, 0.05);
      transition: all 0.3s ease;

      .el-icon {
        font-size: 18px;
      }
    }

    .nav-label {
      position: relative;
      z-index: 2;
    }

    .nav-ink {
      position: absolute;
      inset: 0;
      background: linear-gradient(90deg, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0.05) 100%);
      opacity: 0;
      transition: opacity 0.3s ease;
    }

    &:hover {
      color: $paper-white;

      .nav-icon {
        background: rgba(255, 255, 255, 0.1);
      }

      .nav-ink {
        opacity: 1;
      }
    }

    &.active {
      color: $paper-white;
      background: rgba(255, 255, 255, 0.08);

      .nav-icon {
        background: rgba(59, 130, 246, 0.8);
      }

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 3px;
        height: 24px;
        background: #3b82f6;
        border-radius: 0 2px 2px 0;
      }
    }
  }
}

// 侧边栏底部
.sidebar-footer {
  padding: 20px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);

  .user-info {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px;
    margin-bottom: 12px;
    background: rgba(255, 255, 255, 0.03);
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      background: rgba(255, 255, 255, 0.06);

      .user-arrow {
        opacity: 1;
        transform: translateX(0);
      }
    }

    .user-avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: linear-gradient(135deg, $navy-light 0%, $navy-deep 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      color: $paper-white;

      .el-icon {
        font-size: 18px;
      }
    }

    .user-name {
      font-size: 14px;
      color: $paper-white;
      font-weight: 500;
      flex: 1;
    }

    .user-arrow {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.5);
      opacity: 0;
      transform: translateX(-4px);
      transition: all 0.3s ease;
    }
  }

  .logout-btn {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 8px;
    background: transparent;
    color: rgba(255, 255, 255, 0.7);
    font-size: 14px;
    cursor: pointer;
    transition: all 0.3s ease;
    position: relative;
    overflow: hidden;

    .logout-ink {
      position: absolute;
      inset: 0;
      background: rgba(59, 130, 246, 0.2);
      opacity: 0;
      transition: opacity 0.3s ease;
    }

    span, .el-icon {
      position: relative;
      z-index: 2;
    }

    &:hover {
      color: $paper-white;
      border-color: rgba(59, 130, 246, 0.5);

      .logout-ink {
        opacity: 1;
      }
    }
  }
}

// 主内容区
.main-content {
  flex: 1;
  margin-left: 240px;
  min-height: 100vh;
  background: $paper-white;

  &.full-height {
    margin-left: 0;
  }
}
</style>
