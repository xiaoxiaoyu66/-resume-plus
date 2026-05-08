<template>
  <div class="minimal-template">
    <header v-if="visibility.baseInfo !== false" class="section" :style="{ order: moduleOrder.indexOf('baseInfo') }">
      <div class="header-inner">
        <div class="info-col">
          <h1 class="name">
            <EditableText class="name-text" :model-value="content.baseInfo.name" :placeholder="'姓名'" @update:model-value="v => set('baseInfo.name', v)" />
          </h1>
          <div class="meta-lines">
            <span v-if="content.baseInfo.gender"><EditableText :model-value="content.baseInfo.gender" @update:model-value="v => set('baseInfo.gender', v)" /></span>
            <span v-if="content.baseInfo.birth" class="sep"><EditableText :model-value="content.baseInfo.birth" @update:model-value="v => set('baseInfo.birth', v)" /></span>
            <span v-if="content.baseInfo.phone" class="sep"><EditableText :model-value="content.baseInfo.phone" @update:model-value="v => set('baseInfo.phone', v)" /></span>
            <span v-if="content.baseInfo.email" class="sep"><EditableText :model-value="content.baseInfo.email" @update:model-value="v => set('baseInfo.email', v)" /></span>
            <span v-if="content.baseInfo.city" class="sep"><EditableText :model-value="content.baseInfo.city" @update:model-value="v => set('baseInfo.city', v)" /></span>
          </div>
        </div>
        <div class="avatar-col" v-if="content.baseInfo.avatar">
          <img :src="content.baseInfo.avatar" class="avatar-img" />
        </div>
      </div>
    </header>

    <section v-if="visibility.intention !== false && content.intention?.position" class="section" :style="{ order: moduleOrder.indexOf('intention') }">
      <h3 class="sec-title">求职意向</h3>
      <div class="inline-meta">
        <span v-if="content.intention.position"><strong>岗位：</strong><EditableText :model-value="content.intention.position" @update:model-value="v => set('intention.position', v)" /></span>
        <span v-if="content.intention.city"><strong>城市：</strong><EditableText :model-value="content.intention.city" @update:model-value="v => set('intention.city', v)" /></span>
        <span v-if="content.intention.salary"><strong>薪资：</strong><EditableText :model-value="content.intention.salary" @update:model-value="v => set('intention.salary', v)" /></span>
        <span v-if="content.intention.entryTime"><strong>到岗：</strong><EditableText :model-value="content.intention.entryTime" @update:model-value="v => set('intention.entryTime', v)" /></span>
      </div>
    </section>

    <section v-if="visibility.education !== false && content.education?.length" class="section" :style="{ order: moduleOrder.indexOf('education') }">
      <h3 class="sec-title">教育背景</h3>
      <div v-for="(edu, i) in content.education" :key="i" class="edu-row">
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
      </div>
    </section>

    <section v-if="visibility.experience !== false && content.experience?.length" class="section" :style="{ order: moduleOrder.indexOf('experience') }">
      <h3 class="sec-title">工作经历</h3>
      <div v-for="(exp, i) in content.experience" :key="i" class="exp-block">
        <div class="exp-header">
          <EditableText class="exp-company" :model-value="exp.company" @update:model-value="v => set('experience.' + i + '.company', v)" />
          <EditableText class="exp-position" :model-value="exp.position" @update:model-value="v => set('experience.' + i + '.position', v)" />
          <EditableText class="exp-time" :model-value="exp.start + ' – ' + exp.end" @update:model-value="v => { const parts = v.split(' – '); set('experience.' + i + '.start', parts[0] || ''); set('experience.' + i + '.end', parts[1] || '') }" />
        </div>
        <EditableText class="exp-desc" :model-value="exp.desc" :multiline="true" :placeholder="'描述工作内容...'" @update:model-value="v => set('experience.' + i + '.desc', v)" />
      </div>
    </section>

    <section v-if="visibility.projects !== false && content.projects?.length" class="section" :style="{ order: moduleOrder.indexOf('projects') }">
      <h3 class="sec-title">项目经验</h3>
      <div v-for="(proj, i) in content.projects" :key="i" class="exp-block">
        <div class="exp-header">
          <EditableText class="exp-company" :model-value="proj.name" @update:model-value="v => set('projects.' + i + '.name', v)" />
          <EditableText class="exp-position" :model-value="proj.role" @update:model-value="v => set('projects.' + i + '.role', v)" />
          <EditableText class="exp-time" :model-value="proj.start + ' – ' + proj.end" @update:model-value="v => { const parts = v.split(' – '); set('projects.' + i + '.start', parts[0] || ''); set('projects.' + i + '.end', parts[1] || '') }" />
        </div>
        <EditableText class="exp-desc" :model-value="proj.desc" :multiline="true" :placeholder="'描述项目内容...'" @update:model-value="v => set('projects.' + i + '.desc', v)" />
      </div>
    </section>

    <section v-if="visibility.skills !== false && content.skills?.length" class="section" :style="{ order: moduleOrder.indexOf('skills') }">
      <h3 class="sec-title">技能特长</h3>
      <div class="skills-wrap">
        <EditableText v-for="(skill, i) in content.skills" :key="i" class="skill-tag" :model-value="skill" @update:model-value="v => { const arr = [...content.skills]; arr[i] = v; set('skills', arr) }" />
      </div>
    </section>

    <section v-if="visibility.evaluation !== false && content.evaluation" class="section" :style="{ order: moduleOrder.indexOf('evaluation') }">
      <h3 class="sec-title">自我评价</h3>
      <EditableText class="eval-text" :model-value="content.evaluation" :multiline="true" :placeholder="'写一段自我评价...'" @update:model-value="v => set('evaluation', v)" />
    </section>
  </div>
</template>

<script setup lang="ts">
import EditableText from '@/components/EditableText.vue'
import { useEditable } from '@/composables/useEditable'

defineProps<{
  content: Record<string, any>
  visibility: Record<string, boolean>
  moduleOrder: string[]
}>()

const { setDeep } = useEditable()

function set(path: string, value: any) {
  setDeep(path, value)
}
</script>

<style scoped>
.minimal-template {
  display: flex;
  flex-direction: column;
  font-family: var(--resume-font, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif);
  font-size: var(--resume-font-size, 11.5px);
  line-height: var(--resume-line-height, 1.7);
  color: var(--resume-color, #333);
}

.sec-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--resume-primary, #444);
  margin: 0 0 6px 0;
  padding-bottom: 4px;
  border-bottom: 1px solid var(--resume-primary, #ccc);
  letter-spacing: 0.5px;
}

.section {
  margin-bottom: 14px;
}

.header-inner {
  display: flex;
  gap: 20px;
}

.info-col {
  flex: 1;
  min-width: 0;
}

.name {
  margin: 0 0 6px 0;
}

.name-text {
  font-size: 20px;
  font-weight: 400;
  letter-spacing: 1px;
  color: #222;
}

.meta-lines {
  font-size: 11px;
  color: #666;
}

.sep::before {
  content: "·";
  margin: 0 6px;
  color: #aaa;
}

.avatar-col {
  flex-shrink: 0;
  width: 80px;
}

.avatar-img {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #e0e0e0;
}

.inline-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
  font-size: 11.5px;
  color: #555;
}

.edu-row {
  padding: 5px 0;
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px 0;
}

.edu-left {
  width: 35%;
  flex-shrink: 0;
}

.edu-school {
  font-weight: 600;
  font-size: 12px;
  color: #333;
}

.edu-mid {
  width: 35%;
  flex-shrink: 0;
}

.edu-major {
  color: #555;
}

.edu-degree {
  margin-left: 6px;
  font-size: 10.5px;
  color: #888;
}

.edu-right {
  width: 28%;
  flex-shrink: 0;
  text-align: right;
}

.edu-time {
  font-size: 10.5px;
  color: #aaa;
}

.edu-gpa {
  width: 100%;
  font-size: 10.5px;
  color: #888;
  margin-top: 1px;
}

.exp-block {
  margin-bottom: 8px;
}

.exp-block:last-child {
  margin-bottom: 0;
}

.exp-header {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 4px 12px;
  margin-bottom: 1px;
}

.exp-company {
  font-weight: 600;
  font-size: 12px;
  color: #333;
}

.exp-position {
  font-size: 11.5px;
  color: #666;
}

.exp-time {
  margin-left: auto;
  font-size: 10.5px;
  color: #aaa;
}

.exp-desc {
  font-size: 11.5px;
  color: #555;
  white-space: pre-wrap;
  display: block;
}

.skills-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.skill-tag {
  font-size: 11px;
  color: #555;
}

.skill-tag:not(:last-child)::after {
  content: "/";
  margin-left: 4px;
  color: #ccc;
}

.eval-text {
  font-size: 11.5px;
  color: #555;
  white-space: pre-wrap;
  display: block;
}
</style>
