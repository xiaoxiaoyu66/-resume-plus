<template>
  <div class="ai-panel">
    <!-- 顶部分页 -->
    <div class="ai-tabs">
      <button
        v-for="t in tabs"
        :key="t.key"
        class="ai-tab"
        :class="{ active: activeTab === t.key }"
        @click="switchTab(t.key)"
      >
        <el-icon :size="14"><component :is="t.icon" /></el-icon>
        {{ t.label }}
      </button>
    </div>

    <!-- ============ 诊断结果 + 追问 ============ -->
    <div v-if="activeTab === 'diagnosis'" class="tab-body diagnosis-tab">
      <div class="tab-scroll" ref="diagnosisScrollRef">
        <template v-if="!clinicResult && !clinicLoading">
          <el-empty description="暂无诊断结果" :image-size="80">
            <el-button size="small" type="primary" @click="$emit('re-diagnose')">开始诊断</el-button>
          </el-empty>
        </template>

        <template v-if="clinicLoading">
          <div class="loading-state">
            <el-icon class="is-loading" :size="24"><Loading /></el-icon>
            <span>AI 诊断中...</span>
          </div>
        </template>

        <template v-if="clinicResult">
          <!-- 待改进 -->
          <div v-if="clinicResult.missing?.length" class="panel-section">
            <h4 class="section-title warn">
              <el-icon color="#e6a23c"><WarningFilled /></el-icon>
              待改进
            </h4>
            <ul class="missing-list">
              <li v-for="(item, i) in clinicResult.missing" :key="i">{{ item }}</li>
            </ul>
          </div>

          <!-- AI生成 -->
          <div v-if="clinicResult.generated" class="panel-section">
            <h4 class="section-title primary">
              <el-icon color="#409eff"><MagicStick /></el-icon>
              AI 生成
            </h4>
            <div v-if="clinicResult.generated.evaluation" class="gen-block">
              <div class="gen-label">自我评价</div>
              <p class="gen-text">{{ clinicResult.generated.evaluation }}</p>
              <el-button
                size="small"
                text
                :type="appliedFields.evaluation ? 'success' : 'primary'"
                :icon="appliedFields.evaluation ? Select : CopyDocument"
                class="btn-apply"
                @click="applyEvaluation"
              >
                {{ appliedFields.evaluation ? '已应用' : '应用到简历' }}
              </el-button>
            </div>
            <div v-if="clinicResult.generated.skills?.length" class="gen-block">
              <div class="gen-label">技能标签</div>
              <div class="gen-tags">
                <el-tag v-for="sk in clinicResult.generated.skills" :key="sk" size="small">{{ sk }}</el-tag>
              </div>
              <el-button
                size="small"
                text
                :type="appliedFields.skills ? 'success' : 'primary'"
                :icon="appliedFields.skills ? Select : CopyDocument"
                class="btn-apply"
                @click="applySkills"
              >
                {{ appliedFields.skills ? '已应用' : '应用到简历' }}
              </el-button>
            </div>
          </div>

          <!-- 优化建议 -->
          <div v-if="clinicResult.projectTips?.length" class="panel-section">
            <h4 class="section-title success">
              <el-icon color="#67c23a"><EditPen /></el-icon>
              优化建议
            </h4>
            <div v-for="(tip, i) in clinicResult.projectTips" :key="i" class="tip-card">
              <div class="tip-header">
                <span class="tip-badge">{{ tip.module === 'experience' ? '经历' : '项目' }} #{{ Number(tip.index) + 1 }}</span>
              </div>
              <p class="tip-text">{{ tip.tip }}</p>
              <el-button
                size="small"
                text
                :type="appliedFields['tip-' + i] ? 'success' : 'primary'"
                :icon="appliedFields['tip-' + i] ? Select : CopyDocument"
                class="btn-apply"
                @click="applyProjectTip(tip, i)"
              >
                {{ appliedFields['tip-' + i] ? '已应用' : '应用到简历' }}
              </el-button>
            </div>
          </div>

          <!-- 重新诊断 -->
          <div class="panel-actions">
            <el-button size="small" @click="$emit('re-diagnose')">
              <el-icon><Refresh /></el-icon> 重新诊断
            </el-button>
          </div>

          <!-- 根据岗位深化诊断 -->
          <div class="panel-section deepen-section">
            <h4 class="section-title" :class="props.clinicResult?._deepenedFor ? 'primary' : ''">
              <el-icon><MagicStick /></el-icon>
              {{ props.clinicResult?._deepenedFor ? '已针对「' + props.clinicResult._deepenedFor + '」深化' : '根据岗位深化诊断' }}
            </h4>

            <template v-if="!props.clinicResult?._deepenedFor">
              <div class="deepen-input-row">
                <el-input
                  v-model="targetPosition"
                  placeholder="输入目标岗位，如 Java 后端开发"
                  size="small"
                  :disabled="deepeningLoading"
                  clearable
                  @keydown.enter.prevent="deepenDiagnosis"
                />
                <el-button
                  size="small"
                  type="primary"
                  :loading="deepeningLoading"
                  :disabled="!targetPosition.trim()"
                  @click="deepenDiagnosis"
                >
                  深化
                </el-button>
              </div>
              <div class="deepen-quick-tags">
                <el-tag
                  v-for="pos in commonPositions"
                  :key="pos"
                  size="small"
                  :type="targetPosition === pos ? 'primary' : 'info'"
                  class="clickable"
                  @click="targetPosition = pos"
                >
                  {{ pos }}
                </el-tag>
              </div>
            </template>

            <template v-else>
              <div class="deepen-restore">
                <span class="deepen-hint">当前诊断结果基于该岗位要求定向优化</span>
                <el-button size="small" text type="primary" @click="resetToGeneral">
                  恢复通用诊断
                </el-button>
              </div>
            </template>
          </div>

          <!-- 追问记录 -->
          <div v-if="followUpMessages.length > 0" class="panel-section">
            <h4 class="section-title">
              <el-icon color="#909399"><ChatLineSquare /></el-icon>
              追问记录
            </h4>
            <div class="followup-list">
              <div v-for="(msg, i) in followUpMessages" :key="i" class="chat-msg" :class="msg.role">
                <div class="msg-content">{{ msg.content }}</div>
              </div>
              <div v-if="followUpThinking" class="chat-msg ai">
                <div class="msg-thinking">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  <span>更新中...</span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- 追问输入框 -->
      <div v-if="clinicResult && !clinicLoading" class="followup-input-bar">
        <el-input
          v-model="followUpInput"
          placeholder="追问：帮我把项目描述改得更具体..."
          size="small"
          :disabled="followUpThinking"
          @keydown.enter.prevent="sendFollowUp"
        >
          <template #append>
            <el-button :disabled="!followUpInput.trim() || followUpThinking" @click="sendFollowUp">
              <el-icon><Promotion /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>
    </div>

    <!-- ============ 面试模式 ============ -->
    <div v-if="activeTab === 'interview'" class="tab-body">
      <div class="interview-start">
        <el-icon :size="36" color="#409eff"><Mic /></el-icon>
        <h3>AI 面试模拟</h3>
        <p>基于当前简历内容，AI 将扮演面试官对你进行模拟面试。</p>
        <p class="interview-hint">面试官会根据你的简历提问，每次回答后给出点评和建议。</p>
        <el-button
          type="primary"
          :loading="interviewStarting"
          @click="startInterview"
        >
          开始面试
        </el-button>
      </div>

      <!-- 面试中的对话 -->
      <div v-if="interviewActive" class="interview-chat">
        <div ref="interviewMessagesRef" class="chat-messages">
          <div
            v-for="(msg, i) in interviewMessages"
            :key="i"
            class="chat-msg"
            :class="msg.role"
          >
            <div class="msg-content">{{ msg.content }}</div>
          </div>
          <div v-if="interviewThinking" class="chat-msg ai">
            <div class="msg-thinking">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>思考中...</span>
            </div>
          </div>
        </div>
        <div class="chat-input-bar">
          <el-input
            v-model="interviewInput"
            placeholder="回答面试问题..."
            size="small"
            :disabled="interviewThinking"
            @keydown.enter.prevent="sendInterviewMessage"
          >
            <template #append>
              <el-button :disabled="!interviewInput.trim() || interviewThinking" @click="sendInterviewMessage">
                <el-icon><Promotion /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>
      </div>
    </div>

    <!-- ============ 岗位匹配 ============ -->
    <div v-if="activeTab === 'match'" class="tab-body">
      <div class="tab-scroll">
        <div v-if="matchLoading" class="loading-state">
          <el-icon class="is-loading" :size="24"><Loading /></el-icon>
          <span>匹配中...</span>
        </div>

        <template v-else-if="matchError">
          <el-empty :description="matchError" :image-size="60">
            <el-button size="small" type="primary" @click="doMatchJobs">重试</el-button>
          </el-empty>
        </template>

        <template v-else-if="!matchAttempted">
          <el-empty description="根据简历内容匹配岗位" :image-size="60">
            <el-button size="small" type="primary" @click="doMatchJobs">开始匹配</el-button>
          </el-empty>
        </template>

        <template v-else-if="matchResults.length === 0">
          <el-empty description="匹配完成，暂未找到合适岗位" :image-size="60">
            <el-button size="small" @click="doMatchJobs">重新匹配</el-button>
          </el-empty>
        </template>

        <template v-else>
          <!-- 简历诊断提示 -->
          <div v-if="matchResults[0]?.diagnosisMissing" class="match-warnings-bar">
            <el-alert
              title="简历待改进项提醒"
              :description="matchResults[0].diagnosisMissing.join('；')"
              type="warning"
              :closable="true"
              show-icon
            />
          </div>
          <div class="match-header">
            <span class="match-count">找到 {{ matchResults.length }} 个匹配岗位</span>
            <el-button size="small" text @click="doMatchJobs">刷新</el-button>
          </div>
          <div class="match-list">
            <div v-for="(item, i) in matchResults" :key="i" class="match-card">
              <div class="match-score-col">
                <div class="match-score-circle" :style="{ '--score': item.score }">
                  <svg viewBox="0 0 36 36" class="score-ring">
                    <path class="ring-bg"
                      d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                    />
                    <path class="ring-fill"
                      :stroke-dasharray="`${Math.round(item.score * 100)}, 100`"
                      d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                    />
                  </svg>
                  <span class="score-text">{{ Math.round(item.score * 100) }}</span>
                </div>
              </div>
              <div class="match-info">
                <div class="match-title">{{ item.job.title }}</div>
                <div class="match-company">{{ item.job.company }}</div>
                <div class="match-meta">
                  <span v-if="item.job.salaryMin" class="match-salary">
                    {{ item.job.salaryMin / 10000 }}k-{{ item.job.salaryMax / 10000 }}k
                  </span>
                  <span v-if="item.job.location" class="match-location">{{ item.job.location }}</span>
                </div>
                <div v-if="item.job.tags?.length" class="match-tags">
                  <el-tag v-for="(tag, ti) in item.job.tags.slice(0, 4)" :key="ti" size="small">{{ tag }}</el-tag>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { WarningFilled, MagicStick, EditPen, Loading, Promotion, Mic, Search, Refresh, ChatLineSquare, Select, CopyDocument } from '@element-plus/icons-vue'
import { useResumeStore } from '@/store/resume'
import { useJobsStore } from '@/store/jobs'
import { aiResumeAction } from '@/api/resume'
import SseChatClient from '@/utils/sse-client'

const props = defineProps<{
  clinicResult: any
  clinicLoading: boolean
}>()

const emit = defineEmits<{
  're-diagnose': []
}>()

const resumeStore = useResumeStore()

/** 已应用字段标记 */
const appliedFields = reactive<Record<string, boolean>>({})

// ========== 岗位深化诊断 ==========
const targetPosition = ref('')
const deepeningLoading = ref(false)
/** 保存通用诊断结果，深化后可恢复 */
const originalClinicResult = ref<any>(null)

const commonPositions = [
  'Java后端开发', '前端开发', '全栈工程师',
  'Python开发', '产品经理', '测试工程师',
  '数据分析师', '运维工程师'
]

const tabs = [
  { key: 'diagnosis', label: '诊断', icon: MagicStick },
  { key: 'match', label: '匹配', icon: Search },
  { key: 'interview', label: '面试', icon: Mic }
]
const activeTab = ref('diagnosis')

// ========== 追问 (follow-up) ==========
const followUpInput = ref('')
const followUpMessages = ref<{ role: string; content: string }[]>([])
const followUpThinking = ref(false)
const diagnosisScrollRef = ref<HTMLElement | null>(null)

/** 新诊断结果时清空追问 + 重置深化状态 */
watch(() => props.clinicResult, (val) => {
  if (val) {
    followUpMessages.value = []
    followUpInput.value = ''
    // 如果是父组件触发的全新诊断（无 _deepenedFor），清除深化状态
    if (!val._deepenedFor) {
      originalClinicResult.value = null
      targetPosition.value = ''
    }
  }
})

async function sendFollowUp() {
  const text = followUpInput.value.trim()
  if (!text || followUpThinking.value) return

  followUpMessages.value.push({ role: 'user', content: text })
  followUpInput.value = ''
  followUpThinking.value = true
  scrollDiagnosisToBottom()

  try {
    const resumeContent = JSON.parse(JSON.stringify(resumeStore.content))
    const res = await aiResumeAction({
      action: 'clinic-followup',
      resumeContent,
      diagnosisResult: props.clinicResult,
      followUpMessage: text
    }) as any
    const patch = res.data || res

    mergeClinicPatch(patch)
    ElMessage.success('诊断结果已更新')
  } catch (err: any) {
    console.error('追问失败:', err)
    ElMessage.error('追问失败: ' + (err.message || '未知错误'))
    followUpMessages.value.push({ role: 'ai', content: '追问处理失败，请稍后重试。' })
  } finally {
    followUpThinking.value = false
    scrollDiagnosisToBottom()
  }
}

/**
 * 将追问 patch 合并到现有诊断结果中
 * patch 结构: { missing: [...]|null, generated: {...}|null, projectTips: [...]|null }
 */
function mergeClinicPatch(patch: any) {
  if (!props.clinicResult || !patch) return

  if (patch.missing !== null && patch.missing !== undefined) {
    props.clinicResult.missing = patch.missing
  }
  if (patch.generated !== null && patch.generated !== undefined) {
    if (!props.clinicResult.generated) {
      props.clinicResult.generated = {}
    }
    if (patch.generated.evaluation !== null && patch.generated.evaluation !== undefined) {
      props.clinicResult.generated.evaluation = patch.generated.evaluation
    }
    if (patch.generated.skills !== null && patch.generated.skills !== undefined) {
      props.clinicResult.generated.skills = patch.generated.skills
    }
  }
  if (patch.projectTips !== null && patch.projectTips !== undefined) {
    props.clinicResult.projectTips = patch.projectTips
  }
}

/** 应用 AI 生成的自我评价到简历 */
function applyEvaluation() {
  const text = props.clinicResult?.generated?.evaluation
  if (!text) return
  resumeStore.captureUndo(true)
  resumeStore.content.evaluation = text
  resumeStore.scheduleAutoSave()
  appliedFields.evaluation = true
  ElMessage.success('自我评价已应用到简历')
}

/** 应用 AI 生成的技能标签到简历 */
function applySkills() {
  const skills = props.clinicResult?.generated?.skills
  if (!skills?.length) return
  resumeStore.captureUndo(true)
  resumeStore.content.skills = skills.map((s: string) => ({ name: s, level: '熟练' }))
  resumeStore.scheduleAutoSave()
  appliedFields.skills = true
  ElMessage.success('技能标签已应用到简历')
}

/** 应用某条优化建议到简历对应条目 */
function applyProjectTip(tip: { module: string; index: number; tip: string }, idx: string | number) {
  const arr = resumeStore.content[tip.module as 'experience' | 'projects']
  if (!Array.isArray(arr) || !arr[tip.index]) {
    ElMessage.warning('未找到对应条目')
    return
  }
  resumeStore.captureUndo(true)
  arr[tip.index].desc = arr[tip.index].desc
    ? arr[tip.index].desc + '\n' + tip.tip
    : tip.tip
  resumeStore.scheduleAutoSave()
  appliedFields['tip-' + idx] = true
  ElMessage.success('优化建议已追加到对应描述')
}

/** 根据目标岗位深化诊断 */
async function deepenDiagnosis() {
  const pos = targetPosition.value.trim()
  if (!pos) return

  // 保存通用诊断结果以便恢复
  if (!originalClinicResult.value) {
    originalClinicResult.value = JSON.parse(JSON.stringify(props.clinicResult))
  }

  deepeningLoading.value = true
  try {
    const resumeContent = JSON.parse(JSON.stringify(resumeStore.content))
    const res = await aiResumeAction({
      action: 'clinic',
      resumeContent,
      targetPosition: pos
    }) as any
    const data = res.data || res
    if (data) {
      data._deepenedFor = pos
      // 逐个字段替换，保持引用一致
      Object.assign(props.clinicResult, data)
      // 清空追问（新语境）
      followUpMessages.value = []
      followUpInput.value = ''
      ElMessage.success('已根据「' + pos + '」深化诊断')
    }
  } catch (err: any) {
    console.error('深化诊断失败:', err)
    ElMessage.error('深化失败: ' + (err.message || '未知错误'))
  } finally {
    deepeningLoading.value = false
  }
}

/** 恢复通用诊断 */
function resetToGeneral() {
  if (!originalClinicResult.value) return
  Object.assign(props.clinicResult, originalClinicResult.value)
  originalClinicResult.value = null
  targetPosition.value = ''
  followUpMessages.value = []
  followUpInput.value = ''
  delete props.clinicResult._deepenedFor
  ElMessage.info('已恢复通用诊断')
}

function scrollDiagnosisToBottom() {
  nextTick(() => {
    if (diagnosisScrollRef.value) {
      diagnosisScrollRef.value.scrollTop = diagnosisScrollRef.value.scrollHeight
    }
  })
}

// ========== 面试 ==========
const interviewStarting = ref(false)
const interviewActive = ref(false)
const interviewInput = ref('')
const interviewMessagesRef = ref<HTMLElement | null>(null)
const interviewMessages = ref<{ role: string; content: string }[]>([])
const interviewThinking = ref(false)
let interviewSseClient: SseChatClient | null = null
let interviewSessionId: string | null = null

// 岗位匹配
const matchAttempted = ref(false)
const matchLoading = ref(false)
const matchResults = ref<any[]>([])
const matchError = ref('')

function switchTab(key: string) {
  activeTab.value = key
  if (key === 'match' && matchResults.value.length === 0) {
    doMatchJobs()
  }
}

function scrollInterviewToBottom() {
  nextTick(() => {
    if (interviewMessagesRef.value) {
      interviewMessagesRef.value.scrollTop = interviewMessagesRef.value.scrollHeight
    }
  })
}

function buildResumeContext(): string {
  const c = resumeStore.content
  const parts: string[] = []

  if (c.baseInfo?.name) parts.push(`姓名：${c.baseInfo.name}`)
  if (c.intention?.position) parts.push(`期望岗位：${c.intention.position}`)
  if (c.skills?.length) parts.push(`技能：${(c.skills as any[]).map((s: any) => s.name || s).join('、')}`)
  if (c.experience?.length) {
    const exps = c.experience.map((e: any) => `${e.company}（${e.position}）：${(e.desc || '').substring(0, 100)}`)
    parts.push(`工作经历：\n${exps.join('\n')}`)
  }
  if (c.projects?.length) {
    const projs = c.projects.map((p: any) => `${p.name}（${p.role}）：${(p.desc || '').substring(0, 100)}`)
    parts.push(`项目经验：\n${projs.join('\n')}`)
  }
  if (c.evaluation) parts.push(`自我评价：${c.evaluation.substring(0, 200)}`)

  return parts.join('\n\n')
}

async function doMatchJobs() {
  matchAttempted.value = true
  matchLoading.value = true
  matchError.value = ''
  try {
    const resumeContent = JSON.parse(JSON.stringify(resumeStore.content))
    const params: any = { action: 'matchJobs', resumeContent }

    // 传入诊断缺失项，便于后端做 skill gap 标记
    if (props.clinicResult?.missing?.length) {
      params.diagnosisMissing = props.clinicResult.missing
    }

    const res = await aiResumeAction(params) as any
    const data = res.data || res
    matchResults.value = Array.isArray(data) ? data : []
    // 同步到全局 store，侧边栏可消费
    const jobsStore = useJobsStore()
    jobsStore.setMatchResults(matchResults.value)
  } catch (err: any) {
    console.error('岗位匹配失败:', err)
    matchError.value = err.message || '匹配失败'
    matchResults.value = []
  } finally {
    matchLoading.value = false
  }
}

async function startInterview() {
  interviewStarting.value = true
  interviewActive.value = true
  activeTab.value = 'interview'

  const resumeContext = buildResumeContext()
  const userId = JSON.parse(localStorage.getItem('userInfo') || '{}')?.userId || 1

  await nextTick()

  interviewSseClient = new SseChatClient({
    onToken: (token, fullContent) => {
      interviewThinking.value = false
      const last = interviewMessages.value[interviewMessages.value.length - 1]
      if (last && last.role === 'ai') {
        interviewMessages.value.splice(interviewMessages.value.length - 1, 1, {
          role: 'ai',
          content: fullContent
        })
      } else {
        interviewMessages.value.push({ role: 'ai', content: fullContent })
      }
      scrollInterviewToBottom()
    },
    onComplete: (fullContent, data) => {
      interviewThinking.value = false
      if (data && (data as any).sessionId) {
        interviewSessionId = (data as any).sessionId
      }
    },
    onError: (errorMsg) => {
      interviewThinking.value = false
      ElMessage.error('面试启动失败: ' + errorMsg)
      interviewMessages.value.push({ role: 'ai', content: '抱歉，面试服务暂时不可用。' })
      scrollInterviewToBottom()
    }
  })

  interviewSseClient.streamChat({
    userId,
    message: '请根据我的简历开始面试',
    scene: 'interview',
    resumeContext
  }).catch(() => {
    interviewThinking.value = false
    interviewStarting.value = false
  }).finally(() => {
    interviewStarting.value = false
  })
}

function sendInterviewMessage() {
  const text = interviewInput.value.trim()
  if (!text || interviewThinking.value) return

  interviewMessages.value.push({ role: 'user', content: text })
  interviewInput.value = ''
  interviewThinking.value = true
  scrollInterviewToBottom()

  const userId = JSON.parse(localStorage.getItem('userInfo') || '{}')?.userId || 1

  interviewSseClient = new SseChatClient({
    onToken: (token, fullContent) => {
      interviewThinking.value = false
      const last = interviewMessages.value[interviewMessages.value.length - 1]
      if (last && last.role === 'ai') {
        interviewMessages.value.splice(interviewMessages.value.length - 1, 1, {
          role: 'ai',
          content: fullContent
        })
      } else {
        interviewMessages.value.push({ role: 'ai', content: fullContent })
      }
      scrollInterviewToBottom()
    },
    onComplete: (fullContent, data) => {
      interviewThinking.value = false
      if (data && (data as any).sessionId) {
        interviewSessionId = (data as any).sessionId
      }
    },
    onError: (errorMsg) => {
      interviewThinking.value = false
      ElMessage.error('发送失败: ' + errorMsg)
      scrollInterviewToBottom()
    }
  })

  interviewSseClient.streamChat({
    userId,
    sessionId: interviewSessionId ? Number(interviewSessionId) : null,
    message: text,
    scene: 'interview'
  }).catch(() => {
    interviewThinking.value = false
  })
}
</script>

<style scoped>
.ai-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

.ai-tabs {
  display: flex;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}

.ai-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 10px 4px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 13px;
  color: #888;
  transition: all 0.2s;
  border-bottom: 2px solid transparent;
}

.ai-tab:hover { color: #409eff; background: #f5f8ff; }
.ai-tab.active { color: #409eff; border-bottom-color: #409eff; font-weight: 600; }

.tab-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  padding: 12px;
}

.tab-scroll {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

/* 诊断 tab */
.diagnosis-tab {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  padding: 0;
}
.diagnosis-tab .tab-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.panel-actions {
  text-align: center;
  margin: 16px 0 8px;
}

/* 追问输入框 */
.followup-input-bar {
  flex-shrink: 0;
  padding: 8px 12px;
  border-top: 1px solid #eee;
}
.followup-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 匹配提醒 */
.match-warnings-bar {
  margin-bottom: 12px;
}

/* 诊断 */
.panel-section {
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 8px;
}
.section-title.warn { color: #e6a23c; }
.section-title.primary { color: #409eff; }
.section-title.success { color: #67c23a; }

.missing-list {
  padding-left: 18px;
  margin: 0;
  font-size: 13px;
  color: #e6a23c;
  line-height: 1.8;
}

.gen-block {
  background: #f8faff;
  border: 1px solid #e8f0fe;
  border-radius: 6px;
  padding: 8px 12px;
  margin-bottom: 8px;
}

.gen-label {
  font-size: 12px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 4px;
}

.gen-text {
  font-size: 13px;
  color: #555;
  line-height: 1.6;
  margin: 0;
}

.gen-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tip-card {
  background: #f6ffed;
  border: 1px solid #d9f7be;
  border-radius: 6px;
  padding: 8px 12px;
  margin-bottom: 8px;
}

.tip-header {
  margin-bottom: 4px;
}

.tip-badge {
  display: inline-block;
  background: #52c41a;
  color: #fff;
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 10px;
}

.tip-text {
  font-size: 13px;
  color: #555;
  line-height: 1.6;
  margin: 4px 0 0;
}

.btn-apply {
  float: right;
  margin-top: 4px;
  font-size: 12px;
}
.gen-block::after,
.tip-card::after {
  content: '';
  display: table;
  clear: both;
}

/* 岗位深化诊断 */
.deepen-section {
  border-top: 1px dashed #e0e0e0;
  padding-top: 12px;
  margin-top: 4px;
}
.deepen-input-row {
  display: flex;
  gap: 8px;
}
.deepen-quick-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
}
.deepen-quick-tags .clickable {
  cursor: pointer;
  transition: opacity 0.15s;
}
.deepen-quick-tags .clickable:hover {
  opacity: 0.75;
}
.deepen-restore {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}
.deepen-hint {
  color: #999;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  gap: 12px;
  color: #999;
  font-size: 14px;
}

/* 消息对话（追问 / 面试共用） */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding: 8px 12px;
}

.chat-msg {
  margin-bottom: 8px;
}

.chat-msg.user {
  text-align: right;
}

.msg-content {
  display: inline-block;
  max-width: 85%;
  padding: 6px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.6;
  text-align: left;
  word-break: break-word;
}

.chat-msg.user .msg-content {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.chat-msg.ai .msg-content {
  background: #f0f0f0;
  color: #333;
  border-bottom-left-radius: 4px;
}

.msg-thinking {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  color: #999;
  font-size: 13px;
}

.chat-input-bar {
  flex-shrink: 0;
  padding: 8px 12px 0;
  border-top: 1px solid #eee;
}

/* 面试 */
.interview-chat {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1;
  overflow: hidden;
}

.interview-start {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px 16px;
  text-align: center;
  gap: 8px;
}

.interview-start h3 {
  margin: 8px 0 0;
  font-size: 16px;
  color: #333;
}

.interview-start p {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  margin: 0;
  max-width: 280px;
}

.interview-hint {
  color: #999;
  font-size: 12px;
}

/* 岗位匹配 */
.match-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.match-count {
  font-size: 13px;
  color: #666;
}
.match-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.match-card {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  background: #f8faff;
  border: 1px solid #e8f0fe;
  border-radius: 8px;
  cursor: default;
  transition: box-shadow 0.2s;
}
.match-card:hover {
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
}
.match-score-col {
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  padding-top: 2px;
}
.match-score-circle {
  position: relative;
  width: 36px;
  height: 36px;
}
.score-ring {
  width: 36px;
  height: 36px;
}
.ring-bg {
  fill: none;
  stroke: #eee;
  stroke-width: 3;
}
.ring-fill {
  fill: none;
  stroke: #409eff;
  stroke-width: 3;
  stroke-linecap: round;
  transition: stroke-dasharray 0.5s ease;
}
.score-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 10px;
  font-weight: 700;
  color: #409eff;
}
.match-info {
  flex: 1;
  min-width: 0;
}
.match-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 2px;
}
.match-company {
  font-size: 12px;
  color: #888;
  margin-bottom: 4px;
}
.match-meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #999;
  margin-bottom: 6px;
}
.match-salary {
  color: #67c23a;
  font-weight: 600;
}
.match-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
