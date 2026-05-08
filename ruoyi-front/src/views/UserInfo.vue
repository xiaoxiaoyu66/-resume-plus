<template>
  <div class="user-info-page">
    <div class="page-header">
      <h1 class="page-title">个人信息</h1>
      <p class="page-subtitle">查看和管理您的个人信息</p>
    </div>

    <div class="content-grid">
      <!-- 左侧：基本信息 -->
      <div class="info-card">
        <div class="card-header">
          <el-icon><User /></el-icon>
          <span>基本信息</span>
        </div>
        <div class="card-body">
          <div class="info-item">
            <label>用户名</label>
            <span>{{ userStore.name || '未设置' }}</span>
          </div>
          <div class="info-item">
            <label>手机号</label>
            <span>{{ userInfo.phone || '未绑定' }}</span>
          </div>
          <div class="info-item">
            <label>邮箱</label>
            <span>{{ userInfo.email || '未绑定' }}</span>
          </div>
          <div class="info-item">
            <label>注册时间</label>
            <span>{{ formatDate(userInfo.createTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧：使用额度 -->
      <div class="info-card quota-card">
        <div class="card-header">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 使用额度</span>
        </div>
        <div class="card-body">
          <div class="quota-display">
            <div class="quota-circle">
              <el-progress
                type="circle"
                :percentage="quotaPercentage"
                :color="quotaColor"
                :stroke-width="10"
                :width="140"
                :show-text="false"
              />
              <div class="quota-text">
                <span class="used">{{ quota.used }}</span>
                <span class="separator">/</span>
                <span class="total">{{ quota.total }}</span>
              </div>
            </div>
            <div class="quota-info">
              <p class="quota-label">今日已使用</p>
              <p class="quota-desc">每日限额 {{ quota.total }} 次，次日自动重置</p>
            </div>
          </div>
          <div class="quota-actions">
            <el-button type="primary" @click="goToChat">
              <el-icon><ChatLineRound /></el-icon>
              去对话
            </el-button>
          </div>
        </div>
      </div>

      <!-- 实习档案预览 -->
      <div class="info-card profile-card">
        <div class="card-header">
          <el-icon><Document /></el-icon>
          <span>实习档案</span>
          <el-button type="primary" link @click="goToProfile">
            查看详情
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div class="card-body">
          <template v-if="profileLoading">
            <el-skeleton :rows="3" animated />
          </template>
          <template v-else-if="profile">
            <div class="profile-preview">
              <div class="profile-item">
                <label>姓名</label>
                <span>{{ profile.name || '未填写' }}</span>
              </div>
              <div class="profile-item">
                <label>学校</label>
                <span>{{ profile.school || '未填写' }}</span>
              </div>
              <div class="profile-item">
                <label>专业</label>
                <span>{{ profile.major || '未填写' }}</span>
              </div>
              <div class="profile-item">
                <label>技能标签</label>
                <div class="skill-tags">
                  <el-tag
                    v-for="skill in profileSkills"
                    :key="skill"
                    size="small"
                    effect="dark"
                  >
                    {{ skill }}
                  </el-tag>
                  <span v-if="!profileSkills.length" class="empty-text">未添加技能</span>
                </div>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="empty-profile">
              <el-icon><DocumentDelete /></el-icon>
              <p>暂无实习档案</p>
              <el-button type="primary" @click="goToProfile">立即创建</el-button>
            </div>
          </template>
        </div>
      </div>

      <!-- 最近对话 -->
      <div class="info-card chat-card">
        <div class="card-header">
          <el-icon><ChatLineSquare /></el-icon>
          <span>最近对话</span>
          <el-button type="primary" link @click="goToChatHistory">
            查看全部
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </div>
        <div class="card-body">
          <template v-if="recentChats.length">
            <div
              v-for="chat in recentChats"
              :key="chat.id"
              class="chat-item"
              @click="goToChatSession(chat.id)"
            >
              <div class="chat-icon">
                <el-icon><ChatRound /></el-icon>
              </div>
              <div class="chat-info">
                <span class="chat-title">{{ chat.sessionTitle || '新对话' }}</span>
                <span class="chat-time">{{ formatDate(chat.createTime) }}</span>
              </div>
              <el-icon class="chat-arrow"><ArrowRight /></el-icon>
            </div>
          </template>
          <template v-else>
            <div class="empty-chat">
              <el-icon><ChatLineRound /></el-icon>
              <p>暂无对话记录</p>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getProfile } from '@/api/profile'
import { listHistory, getQuota } from '@/api/chat'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

// 用户信息
const userInfo = ref({
  phone: '',
  email: '',
  createTime: ''
})

// 额度信息
const quota = ref({
  used: 0,
  total: 50
})

// 实习档案
const profile = ref(null)
const profileLoading = ref(false)

// 最近对话
const recentChats = ref([])

// 计算额度百分比
const quotaPercentage = computed(() => {
  return Math.round((quota.value.used / quota.value.total) * 100)
})

// 计算额度颜色
const quotaColor = computed(() => {
  const percentage = quotaPercentage.value
  if (percentage < 50) return '#67C23A'
  if (percentage < 80) return '#E6A23C'
  return '#F56C6C'
})

// 解析技能标签
const profileSkills = computed(() => {
  if (!profile.value?.skills) return []
  try {
    return JSON.parse(profile.value.skills)
  } catch {
    return []
  }
})

// 加载数据
async function loadData() {
  console.log('UserInfo loadData - store 当前状态:', {
    name: userStore.name,
    phonenumber: userStore.phonenumber,
    email: userStore.email,
    createTime: userStore.createTime
  })

  // 确保用户信息已加载
  if (!userStore.name) {
    console.log('UserInfo store 中无用户信息，调用 getInfo')
    await userStore.getInfo()
  }

  console.log('UserInfo 从 store 获取:', {
    phonenumber: userStore.phonenumber,
    email: userStore.email,
    createTime: userStore.createTime
  })

  // 加载用户信息
  userInfo.value = {
    phone: userStore.phonenumber || '',
    email: userStore.email || '',
    createTime: userStore.createTime || new Date()
  }
  console.log('UserInfo 设置后的 userInfo:', userInfo.value)

  // 加载实习档案
  profileLoading.value = true
  try {
    const res = await getProfile()
    profile.value = res.data
  } catch (e) {
    console.error('加载档案失败', e)
  } finally {
    profileLoading.value = false
  }

  // 加载最近对话
  try {
    const res = await listHistory()
    recentChats.value = (res.data || []).slice(0, 5)
  } catch (e) {
    console.error('加载对话失败', e)
  }

  // 加载额度信息
  try {
    const quotaRes = await getQuota()
    if (quotaRes.data) {
      quota.value.used = quotaRes.data.used || 0
      quota.value.total = quotaRes.data.total || 50
    }
  } catch (e) {
    console.error('加载额度信息失败', e)
  }
}

// 页面跳转
function goToChat() {
  router.push('/chat')
}

function goToChatHistory() {
  router.push('/chat/history')
}

function goToChatSession(sessionId) {
  router.push({
    path: '/chat',
    query: { sessionId }
  })
}

function goToProfile() {
  router.push('/profile')
}

// 格式化日期
function formatDate(date) {
  if (!date) return '未知'
  const d = new Date(date)
  return d.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
$ink-black: #0a0a0a;
$ink-deep: #141414;
$ink-mid: #1f1f1f;
$ink-light: #2d2d2d;
$ink-pale: #5a5a5a;
$paper-white: #ffffff;
$paper-cream: #f8f8f6;
$accent-red: #8b1a1a;

.user-info-page {
  padding: 32px;
  min-height: 100vh;
  background: linear-gradient(135deg, $paper-cream 0%, #f0f0f0 100%);
}

.page-header {
  margin-bottom: 32px;

  .page-title {
    font-size: 32px;
    font-weight: 700;
    color: $ink-black;
    font-family: 'Noto Serif SC', 'STSong', serif;
    margin-bottom: 8px;
  }

  .page-subtitle {
    font-size: 14px;
    color: $ink-pale;
  }
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  max-width: 1200px;
}

.info-card {
  background: $paper-white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  overflow: hidden;

  .card-header {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 20px 24px;
    background: linear-gradient(135deg, $ink-deep 0%, $ink-black 100%);
    color: $paper-white;
    font-weight: 600;
    font-size: 16px;

    .el-icon {
      font-size: 20px;
    }

    .el-button {
      margin-left: auto;
      color: rgba(255, 255, 255, 0.8);

      &:hover {
        color: $paper-white;
      }
    }
  }

  .card-body {
    padding: 24px;
  }
}

// 基本信息
.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);

  &:last-child {
    border-bottom: none;
  }

  label {
    font-size: 14px;
    color: $ink-pale;
    font-weight: 500;
  }

  span {
    font-size: 14px;
    color: $ink-black;
    font-weight: 600;
  }
}

// 额度卡片
.quota-card {
  .card-body {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 24px;
  }

  .quota-display {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
  }

  .quota-circle {
    position: relative;
    width: 140px;
    height: 140px;

    .quota-text {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      text-align: center;
      white-space: nowrap;

      .used {
        font-size: 32px;
        font-weight: 700;
        color: $ink-black;
      }

      .separator {
        font-size: 18px;
        color: $ink-pale;
        margin: 0 4px;
      }

      .total {
        font-size: 16px;
        color: $ink-pale;
      }
    }
  }

  .quota-info {
    text-align: center;

    .quota-label {
      font-size: 14px;
      color: $ink-black;
      font-weight: 600;
      margin-bottom: 4px;
    }

    .quota-desc {
      font-size: 12px;
      color: $ink-pale;
    }
  }
}

// 档案卡片
.profile-card {
  grid-column: span 2;

  .profile-preview {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;

    .profile-item {
      display: flex;
      flex-direction: column;
      gap: 8px;

      label {
        font-size: 12px;
        color: $ink-pale;
        font-weight: 500;
      }

      span {
        font-size: 14px;
        color: $ink-black;
        font-weight: 600;
      }

      .skill-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;

        .el-tag {
          background: $ink-deep;
          border-color: $ink-deep;
        }

        .empty-text {
          font-size: 12px;
          color: $ink-pale;
        }
      }
    }
  }

  .empty-profile {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
    padding: 40px;

    .el-icon {
      font-size: 48px;
      color: $ink-pale;
    }

    p {
      font-size: 14px;
      color: $ink-pale;
    }
  }
}

// 对话卡片
.chat-card {
  grid-column: span 2;

  .chat-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      background: rgba(0, 0, 0, 0.03);

      .chat-arrow {
        opacity: 1;
        transform: translateX(0);
      }
    }

    .chat-icon {
      width: 40px;
      height: 40px;
      border-radius: 8px;
      background: linear-gradient(135deg, $ink-deep 0%, $ink-mid 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      color: $paper-white;
    }

    .chat-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 4px;

      .chat-title {
        font-size: 14px;
        color: $ink-black;
        font-weight: 600;
      }

      .chat-time {
        font-size: 12px;
        color: $ink-pale;
      }
    }

    .chat-arrow {
      font-size: 16px;
      color: $ink-pale;
      opacity: 0;
      transform: translateX(-4px);
      transition: all 0.3s ease;
    }
  }

  .empty-chat {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
    padding: 40px;

    .el-icon {
      font-size: 48px;
      color: $ink-pale;
    }

    p {
      font-size: 14px;
      color: $ink-pale;
    }
  }
}

@media (max-width: 768px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .profile-card,
  .chat-card {
    grid-column: span 1;
  }
}
</style>
