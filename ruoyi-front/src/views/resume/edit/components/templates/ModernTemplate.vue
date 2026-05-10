<template>
  <div class="modern-template">
    <!-- 基本信息 -->
    <header v-if="visibility.baseInfo !== false" class="section" data-section="baseInfo" :style="{ order: moduleOrder.indexOf('baseInfo') }">
      <div class="header-inner">
        <div class="info-col">
          <h1 class="name">
            <EditableText class="name-text" :model-value="content.baseInfo.name" :placeholder="'姓名'" @update:model-value="v => set('baseInfo.name', v)" />
          </h1>
          <table class="info-table">
            <tr v-if="content.baseInfo.gender">
              <td class="label">性别</td>
              <td><EditableText :model-value="content.baseInfo.gender" @update:model-value="v => set('baseInfo.gender', v)" /></td>
            </tr>
            <tr v-if="content.baseInfo.birth">
              <td class="label">出生年月</td>
              <td><EditableText :model-value="content.baseInfo.birth" @update:model-value="v => set('baseInfo.birth', v)" /></td>
            </tr>
            <tr v-if="content.baseInfo.phone">
              <td class="label">手机</td>
              <td><EditableText :model-value="content.baseInfo.phone" @update:model-value="v => set('baseInfo.phone', v)" /></td>
            </tr>
            <tr v-if="content.baseInfo.email">
              <td class="label">邮箱</td>
              <td><EditableText :model-value="content.baseInfo.email" @update:model-value="v => set('baseInfo.email', v)" /></td>
            </tr>
            <tr v-if="content.baseInfo.city">
              <td class="label">所在城市</td>
              <td><EditableText :model-value="content.baseInfo.city" @update:model-value="v => set('baseInfo.city', v)" /></td>
            </tr>
          </table>
        </div>
        <div class="avatar-col" v-if="content.baseInfo.avatar">
          <img :src="content.baseInfo.avatar" class="avatar-img" />
        </div>
      </div>
    </header>

    <!-- 求职意向 -->
    <section v-if="visibility.intention !== false && content.intention?.position" class="section" data-section="intention" :style="{ order: moduleOrder.indexOf('intention') }">
      <h3 class="sec-title">求职意向</h3>
      <table class="info-table">
        <tr v-if="content.intention.position">
          <td class="label">期望岗位</td>
          <td><EditableText :model-value="content.intention.position" @update:model-value="v => set('intention.position', v)" /></td>
        </tr>
        <tr v-if="content.intention.city">
          <td class="label">期望城市</td>
          <td><EditableText :model-value="content.intention.city" @update:model-value="v => set('intention.city', v)" /></td>
        </tr>
        <tr v-if="content.intention.salary">
          <td class="label">期望薪资</td>
          <td><EditableText :model-value="content.intention.salary" @update:model-value="v => set('intention.salary', v)" /></td>
        </tr>
        <tr v-if="content.intention.entryTime">
          <td class="label">到岗时间</td>
          <td><EditableText :model-value="content.intention.entryTime" @update:model-value="v => set('intention.entryTime', v)" /></td>
        </tr>
      </table>
    </section>

    <!-- 教育经历 -->
    <section v-if="visibility.education !== false && content.education?.length" class="section" data-section="education" :style="{ order: moduleOrder.indexOf('education') }">
      <h3 class="sec-title">教育背景</h3>
      <div v-for="(edu, i) in content.education" :key="i" class="edu-row" :data-section-index="i">
        <div class="edu-left">
          <EditableText class="edu-school" :model-value="edu.school" @update:model-value="v => set('education.' + i + '.school', v)" />
        </div>
        <div class="edu-mid">
          <EditableText class="edu-major" :model-value="edu.major" @update:model-value="v => set('education.' + i + '.major', v)" />
          <EditableText class="edu-degree" :model-value="edu.degree" @update:model-value="v => set('education.' + i + '.degree', v)" />
        </div>
        <div class="edu-right">
          <EditableText class="edu-time" :model-value="edu.start + ' – ' + edu.end" @update:model-value="v => { const parts = v.split(' – '); set('education.' + i + '.start', parts[0] || ''); set('education.' + i + '.end', parts[1] || '') }" />
        </div>
        <div v-if="edu.gpa" class="edu-gpa">
          GPA：<EditableText :model-value="edu.gpa" @update:model-value="v => set('education.' + i + '.gpa', v)" />
        </div>
        <div v-if="edu.courses?.length" class="edu-courses">
          主修课程：{{ edu.courses.join('、') }}
        </div>
      </div>
    </section>

    <!-- 工作经历 -->
    <section v-if="visibility.experience !== false && content.experience?.length" class="section" data-section="experience" :style="{ order: moduleOrder.indexOf('experience') }">
      <h3 class="sec-title">工作经历</h3>
      <div v-for="(exp, i) in content.experience" :key="i" class="exp-block" :data-section-index="i">
        <div class="exp-header">
          <EditableText class="exp-company" :model-value="exp.company" @update:model-value="v => set('experience.' + i + '.company', v)" />
          <EditableText class="exp-position" :model-value="exp.position" @update:model-value="v => set('experience.' + i + '.position', v)" />
          <EditableText class="exp-time" :model-value="exp.start + ' – ' + exp.end" @update:model-value="v => { const parts = v.split(' – '); set('experience.' + i + '.start', parts[0] || ''); set('experience.' + i + '.end', parts[1] || '') }" />
        </div>
        <EditableText class="exp-desc" :model-value="exp.desc" :multiline="true" :placeholder="'描述工作内容...'" @update:model-value="v => set('experience.' + i + '.desc', v)" />
      </div>
    </section>

    <!-- 校园经历 -->
    <section v-if="visibility.campus !== false && content.campus?.length" class="section" data-section="campus" :style="{ order: moduleOrder.indexOf('campus') }">
      <h3 class="sec-title">校园经历</h3>
      <div v-for="(item, i) in content.campus" :key="i" class="exp-block" :data-section-index="i">
        <div class="exp-header">
          <EditableText class="exp-company" :model-value="item.organization" @update:model-value="v => set('campus.' + i + '.organization', v)" />
          <EditableText class="exp-position" :model-value="item.position" @update:model-value="v => set('campus.' + i + '.position', v)" />
          <EditableText class="exp-time" :model-value="item.start + ' – ' + item.end" @update:model-value="v => { const parts = v.split(' – '); set('campus.' + i + '.start', parts[0] || ''); set('campus.' + i + '.end', parts[1] || '') }" />
        </div>
        <EditableText class="exp-desc" :model-value="item.desc" :multiline="true" :placeholder="'描述活动内容...'" @update:model-value="v => set('campus.' + i + '.desc', v)" />
      </div>
    </section>

    <!-- 项目经验 -->
    <section v-if="visibility.projects !== false && content.projects?.length" class="section" data-section="projects" :style="{ order: moduleOrder.indexOf('projects') }">
      <h3 class="sec-title">项目经验</h3>
      <div v-for="(proj, i) in content.projects" :key="i" class="exp-block" :data-section-index="i">
        <div class="exp-header">
          <EditableText class="exp-company" :model-value="proj.name" @update:model-value="v => set('projects.' + i + '.name', v)" />
          <EditableText class="exp-position" :model-value="proj.role" @update:model-value="v => set('projects.' + i + '.role', v)" />
          <EditableText class="exp-time" :model-value="proj.start + ' – ' + proj.end" @update:model-value="v => { const parts = v.split(' – '); set('projects.' + i + '.start', parts[0] || ''); set('projects.' + i + '.end', parts[1] || '') }" />
        </div>
        <EditableText class="exp-desc" :model-value="proj.desc" :multiline="true" :placeholder="'描述项目内容...'" @update:model-value="v => set('projects.' + i + '.desc', v)" />
      </div>
    </section>

    <!-- 荣誉奖项 -->
    <section v-if="visibility.awards !== false && content.awards?.length" class="section" data-section="awards" :style="{ order: moduleOrder.indexOf('awards') }">
      <h3 class="sec-title">荣誉奖项</h3>
      <div v-for="(award, i) in content.awards" :key="i" class="award-row">
        <span class="award-name"><EditableText :model-value="award.name" @update:model-value="v => set('awards.' + i + '.name', v)" /></span>
        <span v-if="award.level" class="award-level"><EditableText :model-value="award.level" @update:model-value="v => set('awards.' + i + '.level', v)" /></span>
        <span v-if="award.date" class="award-date"><EditableText :model-value="award.date" @update:model-value="v => set('awards.' + i + '.date', v)" /></span>
      </div>
    </section>

    <!-- 证书 -->
    <section v-if="visibility.certificates !== false && content.certificates?.length" class="section" data-section="certificates" :style="{ order: moduleOrder.indexOf('certificates') }">
      <h3 class="sec-title">证书</h3>
      <div v-for="(cert, i) in content.certificates" :key="i" class="award-row">
        <span class="award-name"><EditableText :model-value="cert.name" @update:model-value="v => set('certificates.' + i + '.name', v)" /></span>
        <span v-if="cert.date" class="award-date"><EditableText :model-value="cert.date" @update:model-value="v => set('certificates.' + i + '.date', v)" /></span>
      </div>
    </section>

    <!-- 技能特长 -->
    <section v-if="visibility.skills !== false && content.skills?.length" class="section" data-section="skills" :style="{ order: moduleOrder.indexOf('skills') }">
      <h3 class="sec-title">技能特长</h3>
      <div class="skills-wrap">
        <EditableText v-for="(skill, i) in content.skills" :key="i" class="skill-tag" :model-value="skill" @update:model-value="v => { const arr = [...content.skills]; arr[i] = v; set('skills', arr) }" />
      </div>
    </section>

    <!-- 自我评价 -->
    <section v-if="visibility.evaluation !== false && content.evaluation" class="section" data-section="evaluation" :style="{ order: moduleOrder.indexOf('evaluation') }">
      <h3 class="sec-title">自我评价</h3>
      <EditableText class="eval-text" :model-value="content.evaluation" :multiline="true" :placeholder="'写一段自我评价...'" @update:model-value="v => set('evaluation', v)" />
    </section>
  </div>
</template>

<script setup lang="ts">
import EditableText from '@/components/EditableText.vue'
import { useEditable } from '@/composables/useEditable'

interface TemplateContent {
  baseInfo: Record<string, any>
  intention?: Record<string, any>
  education?: any[]
  experience?: any[]
  projects?: any[]
  skills?: string[]
  evaluation?: string
  [key: string]: any
}

defineProps<{
  content: TemplateContent
  visibility: Record<string, boolean>
  moduleOrder: string[]
}>()

const { setDeep } = useEditable()

function set(path: string, value: any) {
  setDeep(path, value)
}
</script>

<style scoped>
.modern-template {
  display: flex;
  flex-direction: column;
  font-family: var(--resume-font, "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif);
  font-size: var(--resume-font-size, 12px);
  line-height: var(--resume-line-height, 1.7);
  color: var(--resume-color, #2c3e50);
}

/* ── 章节标题 ── */
.sec-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--resume-primary, #1a365d);
  margin: 0 0 8px 0;
  padding-bottom: 5px;
  border-bottom: 2px solid var(--resume-primary, #2b6cb0);
  letter-spacing: 0.5px;
}

/* ── 基本信息 ── */
.header-inner {
  display: flex;
  gap: 24px;
}

.info-col {
  flex: 1;
  min-width: 0;
}

.name {
  margin: 0 0 10px 0;
}

.name-text {
  font-size: 22px;
  font-weight: 700;
  color: var(--resume-primary, #1a365d);
  letter-spacing: 2px;
}

.avatar-col {
  flex-shrink: 0;
  width: 90px;
}

.avatar-img {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #e2e8f0;
}

/* ── 信息表格 ── */
.info-table {
  width: 100%;
  border-collapse: collapse;
}

.info-table td {
  padding: 2px 0;
  font-size: 12px;
  vertical-align: top;
}

.info-table .label {
  width: 72px;
  color: #718096;
  white-space: nowrap;
}

/* ── 教育背景 ── */
.edu-row {
  padding: 6px 0;
  border-bottom: 1px dashed #edf2f7;
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px 0;
}

.edu-row:last-child {
  border-bottom: none;
}

.edu-left {
  width: 35%;
  flex-shrink: 0;
}

.edu-school {
  font-weight: 600;
  font-size: 12.5px;
  color: #2d3748;
}

.edu-mid {
  width: 35%;
  flex-shrink: 0;
}

.edu-major {
  color: #4a5568;
}

.edu-degree {
  margin-left: 6px;
  font-size: 11px;
  color: #718096;
}

.edu-right {
  width: 28%;
  flex-shrink: 0;
  text-align: right;
}

.edu-time {
  font-size: 11px;
  color: #a0aec0;
}

.edu-gpa {
  width: 100%;
  font-size: 11px;
  color: #718096;
  margin-top: 2px;
}

/* ── 工作/项目经历 ── */
.exp-block {
  margin-bottom: 10px;
}

.exp-block:last-child {
  margin-bottom: 0;
}

.exp-header {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 4px 12px;
  margin-bottom: 2px;
}

.exp-company {
  font-weight: 600;
  font-size: 12.5px;
  color: #2d3748;
}

.exp-position {
  font-size: 12px;
  color: #4a5568;
}

.exp-time {
  margin-left: auto;
  font-size: 11px;
  color: #a0aec0;
}

.exp-desc {
  font-size: 12px;
  color: #4a5568;
  padding-left: 0;
  white-space: pre-wrap;
  display: block;
}

/* ── 技能 ── */
.skills-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.skill-tag {
  background: #ebf4ff;
  color: #2b6cb0;
  padding: 2px 10px;
  border-radius: 3px;
  font-size: 11px;
}

/* ── 自我评价 ── */
.eval-text {
  font-size: 12px;
  color: #4a5568;
  white-space: pre-wrap;
  display: block;
}

/* ── 通用 section ── */
/* ── 主修课程 ── */
.edu-courses {
  width: 100%;
  font-size: 11px;
  color: #718096;
  margin-top: 2px;
}

/* ── 奖项/证书 ── */
.award-row {
  padding: 5px 0;
  display: flex;
  align-items: baseline;
  gap: 10px;
  font-size: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.award-row:last-child {
  border-bottom: none;
}

.award-name {
  font-weight: 500;
  color: #2d3748;
  flex: 1;
  min-width: 0;
}

.award-level {
  font-size: 10px;
  color: #2b6cb0;
  background: #ebf8ff;
  padding: 1px 8px;
  border-radius: 10px;
  white-space: nowrap;
  flex-shrink: 0;
}

.award-date {
  font-size: 11px;
  color: #a0aec0;
  white-space: nowrap;
  flex-shrink: 0;
}

.section {
  margin-bottom: 16px;
}
</style>
