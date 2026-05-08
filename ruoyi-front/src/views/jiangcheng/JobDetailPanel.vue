<template>
  <div class="job-detail-panel" :class="{ open: isOpen }">
    <!-- 遮罩层 -->
    <div v-if="isOpen" class="overlay" @click="close"></div>

    <!-- 侧滑面板 -->
    <div class="panel">
      <!-- 头部 -->
      <div class="panel-header">
        <h2 class="panel-title">{{ job?.title || '岗位详情' }}</h2>
        <button class="close-btn" @click="close">
          <el-icon><ArrowRight /></el-icon>
        </button>
      </div>

      <!-- 内容 -->
      <div v-if="job" class="panel-body">
        <!-- 薪资 + 印章 -->
        <div class="pd-section pd-top">
          <span class="pd-salary">{{ formatSalary(job) }}</span>
          <span v-if="job.sourceType !== undefined" :class="['pd-source-badge', sourceBadgeClass]">
            {{ job.source || '未知来源' }}
          </span>
        </div>

        <!-- 公司 + 地点 -->
        <div class="pd-company-row">
          <div class="pd-company-icon">{{ job.company?.charAt(0) || '?' }}</div>
          <div class="pd-company-info">
            <span class="pd-company-name">{{ job.company }}</span>
            <span class="pd-meta">{{ job.location }} · {{ job.type || job.education }}</span>
          </div>
        </div>

        <!-- 笔触分隔 -->
        <div class="pd-divider"></div>

        <!-- 技能标签 -->
        <div v-if="job.tags" class="pd-section">
          <h3 class="pd-label">技能要求</h3>
          <div class="pd-tags">
            <span v-for="tag in parseTags(job.tags)" :key="tag" class="pd-tag">{{ tag }}</span>
          </div>
        </div>

        <!-- 岗位职责 -->
        <div class="pd-section">
          <h3 class="pd-label">岗位职责</h3>
          <p class="pd-text">{{ displayDescription || '暂无描述' }}</p>
        </div>

        <!-- 招聘要求 -->
        <div class="pd-section">
          <h3 class="pd-label">招聘要求</h3>
          <div v-if="displayRequirementsParsed.length > 0">
            <p v-for="(req, i) in displayRequirementsParsed" :key="i" class="pd-req-item">
              · {{ req }}
            </p>
          </div>
          <p v-else class="pd-text">暂无要求</p>
        </div>

        <!-- 发布信息 -->
        <div class="pd-section pd-pub-info">
          <h3 class="pd-label">发布信息</h3>
          <div class="pd-pub-row">
            <span class="pd-pub-label">发布时间</span>
            <span>{{ job.publishTime || '待核实' }}</span>
          </div>
          <div class="pd-pub-row">
            <span class="pd-pub-label">发布来源</span>
            <span :class="['pd-pub-source', sourceBadgeClass]">{{ job.source || '手动录入' }}</span>
          </div>
        </div>

      </div>

      <!-- 空状态 -->
      <div v-else class="panel-body panel-empty">
        <p>暂无岗位信息</p>
      </div>

      <!-- 底部按钮 -->
      <div class="panel-footer">
        <a
          v-if="job?.sourceUrl"
          :href="job.sourceUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="pd-apply-btn"
        >
          前往投递
          <el-icon><TopRight /></el-icon>
        </a>
        <button v-else class="pd-apply-btn pd-apply-btn--disabled" disabled>
          暂无可投递链接
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ArrowRight, TopRight } from '@element-plus/icons-vue'

const props = defineProps({
  isOpen: { type: Boolean, default: false },
  job: { type: Object, default: null }
})

const emit = defineEmits(['close'])

function close() {
  emit('close')
}

const sourceBadgeClass = computed(() => {
  if (!props.job) return ''
  const st = props.job.sourceType
  if (st === 0) return 'badge-official'    // 官方
  if (st === 1) return 'badge-commercial'  // 商业平台
  return 'badge-other'
})

/** 当 requirements 为空时，尝试从 description 中按关键词拆分 */
const reqMarkers = ['任职要求', '招聘要求', '职位要求', '任职资格', '岗位要求', '应聘条件']

function splitDescription(text) {
  if (!text) return { description: '', requirements: '' }
  let splitIdx = -1
  for (const marker of reqMarkers) {
    const idx = text.indexOf(marker)
    if (idx !== -1 && (splitIdx === -1 || idx < splitIdx)) splitIdx = idx
  }
  if (splitIdx === -1) return { description: text.trim(), requirements: '' }
  return {
    description: text.substring(0, splitIdx).trim(),
    requirements: text.substring(splitIdx).trim()
  }
}

const displayDescription = computed(() => {
  if (!props.job) return ''
  if (props.job.requirements) return props.job.description || ''
  const splitted = splitDescription(props.job.description || '')
  return splitted.description
})

const displayRequirementsParsed = computed(() => {
  if (!props.job) return []
  let reqText = props.job.requirements || ''
  if (!reqText) {
    const splitted = splitDescription(props.job.description || '')
    reqText = splitted.requirements
  }
  return reqText.split(/[\n·•]/).map(s => s.trim()).filter(Boolean)
})

function formatSalary(job) {
  if (job.salary) return job.salary
  if (job.salaryMin && job.salaryMax) {
    const min = job.salaryMin >= 10000 ? (job.salaryMin / 10000).toFixed(0) + '万' : job.salaryMin
    const max = job.salaryMax >= 10000 ? (job.salaryMax / 10000).toFixed(0) + '万' : job.salaryMax
    return `${min}-${max}/月`
  }
  if (job.salaryMin) return `${job.salaryMin}/月`
  return '薪资面议'
}

function parseRequirements(text) {
  if (!text) return []
  // 按换行或 · 分割
  return text.split(/[\n·•]/).map(s => s.trim()).filter(Boolean)
}

function parseTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  try { return JSON.parse(tags) } catch { return [] }
}
</script>

<style scoped lang="scss">
@import '@/components/ShuimoUI/styles/variables.scss';
@import '@/components/ShuimoUI/styles/mixins.scss';

.job-detail-panel {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  pointer-events: none;
  z-index: 1500;

  &.open {
    pointer-events: auto;
  }
}

.overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(2px);
}

.panel {
  position: absolute;
  top: 0;
  right: 0;
  width: 420px;
  height: 100%;
  background: $paper-white;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  transform: translateX(100%);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  .job-detail-panel.open & {
    transform: translateX(0);
  }
}

/* 头部 */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 24px 20px;
  border-bottom: 1px solid rgba(14, 50, 101, 0.08);

  .panel-title {
    font-size: 20px;
    font-weight: 600;
    color: $ink-deep;
    font-family: $font-family;
    letter-spacing: 1px;
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .close-btn {
    width: 36px;
    height: 36px;
    border: none;
    background: rgba(14, 50, 101, 0.06);
    color: $ink-pale;
    cursor: pointer;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all $transition-fast;
    flex-shrink: 0;

    &:hover {
      background: rgba(14, 50, 101, 0.12);
      color: $ink-mid;
    }
  }
}

/* 内容 */
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.pd-section {
  margin-bottom: 24px;
}

.pd-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.pd-salary {
  font-size: 24px;
  font-weight: 700;
  color: $accent-red;
  letter-spacing: 1px;
}

/* 来源标识 */
.pd-source-badge {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 3px;
  letter-spacing: 1px;
  font-family: $font-family;
  flex-shrink: 0;

  &.badge-official {
    border: 1.5px solid $accent-red;
    color: $accent-red;
    background: rgba(196, 92, 72, 0.06);
    transform: rotate(-2deg);
    font-weight: 600;
  }

  &.badge-commercial {
    border: 1px solid rgba(14, 50, 101, 0.2);
    color: $ink-light;
    background: transparent;
  }

  &.badge-other {
    border: 1px solid $border-light;
    color: $ink-pale;
    background: transparent;
  }
}

.pd-company-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}

.pd-company-icon {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: linear-gradient(135deg, $ink-primary 0%, $ink-primary-light 100%);
  color: $paper-white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  font-family: $font-family;
  flex-shrink: 0;
}

.pd-company-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.pd-company-name {
  font-size: 16px;
  font-weight: 500;
  color: $ink-deep;
}

.pd-meta {
  font-size: 13px;
  color: $ink-light;
}

.pd-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, $ink-pale 30%, $ink-pale 70%, transparent 100%);
  margin-bottom: 24px;
  opacity: 0.3;
}

.pd-label {
  font-size: 14px;
  font-weight: 600;
  color: $ink-mid;
  font-family: $font-family;
  letter-spacing: 1px;
  margin-bottom: 10px;
  position: relative;
  display: inline-block;

  &::after {
    content: '';
    position: absolute;
    bottom: -2px;
    left: 0;
    right: 0;
    height: 1px;
    background: $ink-pale;
    opacity: 0.3;
  }
}

.pd-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pd-tag {
  padding: 4px 12px;
  background: rgba(14, 50, 101, 0.06);
  border: 1px solid rgba(14, 50, 101, 0.1);
  border-radius: 4px;
  font-size: 12px;
  color: $ink-light;
}

.pd-text {
  font-size: 14px;
  color: $ink-mid;
  line-height: 1.8;
}

.pd-req-item {
  font-size: 14px;
  color: $ink-mid;
  line-height: 1.8;
  padding-left: 4px;
}

/* 发布信息 */
.pd-pub-info {
  background: rgba(14, 50, 101, 0.03);
  border-radius: 8px;
  padding: 16px;
}

.pd-pub-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 13px;
  color: $ink-mid;
}

.pd-pub-label {
  color: $ink-pale;
  font-family: $font-family;
}

.pd-pub-source {
  @extend .pd-source-badge;
  font-size: 11px;
}

/* 空状态 */
.panel-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: $ink-pale;
  font-family: $font-family;
}

/* 底部 */
.panel-footer {
  padding: 20px 24px;
  border-top: 1px solid rgba(14, 50, 101, 0.08);
}

.pd-apply-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, $accent-red 0%, darken($accent-red, 8%) 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-family: $font-family;
  letter-spacing: 2px;
  cursor: pointer;
  text-decoration: none;
  transition: all $transition-base;
  box-shadow: 0 4px 12px rgba(196, 92, 72, 0.3);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(196, 92, 72, 0.4);
  }

  &:active {
    transform: translateY(0);
  }

  &--disabled {
    background: rgba(14, 50, 101, 0.08);
    color: $ink-pale;
    box-shadow: none;
    cursor: not-allowed;

    &:hover {
      transform: none;
      box-shadow: none;
    }
  }
}

/* 响应式 */
@media (max-width: 640px) {
  .panel {
    width: 100%;
  }
}
</style>
