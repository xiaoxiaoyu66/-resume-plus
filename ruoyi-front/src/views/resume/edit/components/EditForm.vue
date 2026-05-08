<template>
  <div class="edit-form" @focusin.capture="onFormFocus">
    <!-- 基本信息 -->
    <section id="section-baseInfo" class="form-section" :style="{ order: getOrder('baseInfo') }">
      <h3 class="section-title">基本信息</h3>
      <div class="base-info-layout">
        <el-form :model="content.baseInfo" label-width="80px" size="default" class="info-form">
          <el-form-item label="姓名" required>
            <el-input v-model="content.baseInfo.name" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="电话" required>
            <el-input v-model="content.baseInfo.phone" placeholder="请输入电话" />
          </el-form-item>
          <el-form-item label="邮箱" required>
            <el-input v-model="content.baseInfo.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="content.baseInfo.gender">
              <el-radio value="男">男</el-radio>
              <el-radio value="女">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="出生年月">
            <el-date-picker
              v-model="content.baseInfo.birth"
              type="month"
              placeholder="选择出生年月"
              value-format="YYYY-MM"
            />
          </el-form-item>
          <el-form-item label="所在城市">
            <el-input v-model="content.baseInfo.city" placeholder="如：北京" />
          </el-form-item>
        </el-form>
        <div class="avatar-upload-col">
          <div class="avatar-uploader" @click="triggerAvatarInput">
            <img v-if="content.baseInfo.avatar" :src="content.baseInfo.avatar" class="avatar-preview" />
            <div v-else class="avatar-placeholder">
              <el-icon size="24"><Plus /></el-icon>
              <span>上传头像</span>
            </div>
          </div>
          <input
            ref="avatarInputRef"
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            class="avatar-file-input"
            @change="onAvatarChange"
          />
          <p class="avatar-hint" v-if="content.baseInfo.avatar">
            <el-button type="danger" link size="small" @click="removeAvatar">删除头像</el-button>
          </p>
          <p class="avatar-hint">支持 JPG/PNG/GIF/WebP，建议 1:1 比例</p>
        </div>
      </div>
    </section>

    <!-- 求职意向 -->
    <section id="section-intention" class="form-section" v-show="moduleVisibility.intention" :style="{ order: getOrder('intention') }">
      <h3 class="section-title">求职意向</h3>
      <el-form :model="content.intention" label-width="80px" size="default">
        <el-form-item label="期望岗位" required>
          <el-input v-model="content.intention.position" placeholder="如：Java开发工程师" />
        </el-form-item>
        <el-form-item label="期望城市" required>
          <el-input v-model="content.intention.city" placeholder="如：北京/上海" />
        </el-form-item>
        <el-form-item label="期望薪资">
          <el-input v-model="content.intention.salary" placeholder="如：15K-20K" />
        </el-form-item>
        <el-form-item label="到岗时间">
          <el-radio-group v-model="content.intention.entryTime">
            <el-radio value="随时">随时</el-radio>
            <el-radio value="一周内">一周内</el-radio>
            <el-radio value="两周内">两周内</el-radio>
            <el-radio value="一个月内">一个月内</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </section>

    <!-- 教育经历 -->
    <section id="section-education" class="form-section" :style="{ order: getOrder('education') }">
      <h3 class="section-title">
        教育经历
        <el-button type="primary" link size="small" @click="addEducation">
          <el-icon><Plus /></el-icon>添加
        </el-button>
      </h3>
      <div v-for="(edu, index) in content.education" :key="index" class="item-card">
        <div class="item-header">
          <span>经历 {{ index + 1 }}</span>
          <el-button type="danger" link size="small" @click="removeEducation(index)">
            <el-icon><Delete /></el-icon>删除
          </el-button>
        </div>
        <el-form :model="edu" label-width="80px" size="default">
          <el-form-item label="学校" required>
            <el-input v-model="edu.school" placeholder="学校名称" />
          </el-form-item>
          <el-form-item label="专业" required>
            <el-input v-model="edu.major" placeholder="专业名称" />
          </el-form-item>
          <el-form-item label="学历" required>
            <el-select v-model="edu.degree" placeholder="请选择">
              <el-option label="大专" value="大专" />
              <el-option label="本科" value="本科" />
              <el-option label="硕士" value="硕士" />
              <el-option label="博士" value="博士" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间段" required>
            <el-date-picker v-model="edu.start" type="month" placeholder="开始时间" value-format="YYYY.MM" style="width: 45%" />
            <span style="margin: 0 8px">-</span>
            <el-date-picker v-model="edu.end" type="month" placeholder="结束时间" value-format="YYYY.MM" style="width: calc(45% - 56px)" :disabled="edu.end === '至今'" />
            <el-checkbox :model-value="edu.end === '至今'" @update:model-value="c => { edu.end = c ? '至今' : '' }" size="small">至今</el-checkbox>
          </el-form-item>
          <el-form-item label="GPA">
            <el-input v-model="edu.gpa" placeholder="如：3.8/4.0" />
          </el-form-item>
        </el-form>
      </div>
      <el-empty v-if="content.education.length === 0" description="暂无教育经历，点击上方添加" :image-size="60" />
    </section>

    <!-- 工作经历 -->
    <section id="section-experience" class="form-section" v-show="moduleVisibility.experience" :style="{ order: getOrder('experience') }">
      <h3 class="section-title">
        工作经历
        <el-button type="primary" link size="small" @click="addExperience">
          <el-icon><Plus /></el-icon>添加
        </el-button>
      </h3>
      <div v-for="(exp, index) in content.experience" :key="index" class="item-card">
        <div class="item-header">
          <span>经历 {{ index + 1 }}</span>
          <el-button type="danger" link size="small" @click="removeExperience(index)">
            <el-icon><Delete /></el-icon>删除
          </el-button>
        </div>
        <el-form :model="exp" label-width="80px" size="default">
          <el-form-item label="公司" required>
            <el-input v-model="exp.company" placeholder="公司名称" />
          </el-form-item>
          <el-form-item label="职位" required>
            <el-input v-model="exp.position" placeholder="职位名称" />
          </el-form-item>
          <el-form-item label="时间段" required>
            <el-date-picker v-model="exp.start" type="month" placeholder="开始时间" value-format="YYYY.MM" style="width: 45%" />
            <span style="margin: 0 8px">-</span>
            <el-date-picker v-model="exp.end" type="month" placeholder="结束时间" value-format="YYYY.MM" style="width: calc(45% - 56px)" :disabled="exp.end === '至今'" />
            <el-checkbox :model-value="exp.end === '至今'" @update:model-value="c => { exp.end = c ? '至今' : '' }" size="small">至今</el-checkbox>
          </el-form-item>
          <el-form-item label="工作描述" required>
            <MarkdownTextarea v-model="exp.desc" placeholder="描述你的工作职责和成果，支持 Markdown 语法" />
          </el-form-item>
        </el-form>
      </div>
      <el-empty v-if="content.experience.length === 0" description="暂无工作经历" :image-size="60" />
    </section>

    <!-- 项目经验 -->
    <section id="section-projects" class="form-section" v-show="moduleVisibility.projects" :style="{ order: getOrder('projects') }">
      <h3 class="section-title">
        项目经验
        <el-button type="primary" link size="small" @click="addProject">
          <el-icon><Plus /></el-icon>添加
        </el-button>
      </h3>
      <div v-for="(proj, index) in content.projects" :key="index" class="item-card">
        <div class="item-header">
          <span>项目 {{ index + 1 }}</span>
          <el-button type="danger" link size="small" @click="removeProject(index)">
            <el-icon><Delete /></el-icon>删除
          </el-button>
        </div>
        <el-form :model="proj" label-width="80px" size="default">
          <el-form-item label="项目名" required>
            <el-input v-model="proj.name" placeholder="项目名称" />
          </el-form-item>
          <el-form-item label="角色" required>
            <el-input v-model="proj.role" placeholder="如：后端开发" />
          </el-form-item>
          <el-form-item label="时间段" required>
            <el-date-picker v-model="proj.start" type="month" placeholder="开始时间" value-format="YYYY.MM" style="width: 45%" />
            <span style="margin: 0 8px">-</span>
            <el-date-picker v-model="proj.end" type="month" placeholder="结束时间" value-format="YYYY.MM" style="width: calc(45% - 56px)" :disabled="proj.end === '至今'" />
            <el-checkbox :model-value="proj.end === '至今'" @update:model-value="c => { proj.end = c ? '至今' : '' }" size="small">至今</el-checkbox>
          </el-form-item>
          <el-form-item label="项目描述" required>
            <MarkdownTextarea v-model="proj.desc" placeholder="描述项目背景、你的职责和成果，支持 Markdown 语法" />
          </el-form-item>
        </el-form>
      </div>
      <el-empty v-if="content.projects.length === 0" description="暂无项目经验" :image-size="60" />
    </section>

    <!-- 技能特长 -->
    <section id="section-skills" class="form-section" v-show="moduleVisibility.skills" :style="{ order: getOrder('skills') }">
      <h3 class="section-title">技能特长</h3>
      <div class="skills-input-wrapper">
        <el-tag
          v-for="(skill, index) in content.skills"
          :key="index"
          closable
          class="skill-tag"
          @close="removeSkill(index)"
        >
          {{ skill }}
        </el-tag>
        <el-input
          v-if="inputVisible"
          ref="skillInputRef"
          v-model="inputValue"
          size="small"
          style="width: 100px"
          @keyup.enter="confirmSkill"
          @blur="confirmSkill"
        />
        <el-button v-else type="primary" link size="small" @click="showSkillInput">
          <el-icon><Plus /></el-icon>添加技能
        </el-button>
      </div>
    </section>

    <!-- 自我评价 -->
    <section id="section-evaluation" class="form-section" v-show="moduleVisibility.evaluation" :style="{ order: getOrder('evaluation') }">
      <h3 class="section-title">自我评价</h3>
      <MarkdownTextarea v-model="content.evaluation" :rows="5" placeholder="简单描述你的优势、特点，支持 Markdown 语法" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { useResumeStore } from '@/store/resume'
import MarkdownTextarea from './MarkdownTextarea.vue'

const resumeStore = useResumeStore()
const content = computed(() => resumeStore.content)
const moduleVisibility = computed(() => resumeStore.moduleVisibility)

const getOrder = (key: string) => resumeStore.moduleOrder.indexOf(key as any)

const inputVisible = ref(false)
const inputValue = ref('')
const skillInputRef = ref<HTMLElement | null>(null)
const avatarInputRef = ref<HTMLElement | null>(null)

// 头像上传
const triggerAvatarInput = () => {
  avatarInputRef.value?.click()
}

const onAvatarChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像图片不能超过 2MB')
    return
  }
  const reader = new FileReader()
  reader.onload = (ev: ProgressEvent<FileReader>) => {
    content.value.baseInfo.avatar = (ev.target?.result as string) || ''
    resumeStore.scheduleAutoSave()
  }
  reader.readAsDataURL(file)
  target.value = ''
}

const removeAvatar = () => {
  content.value.baseInfo.avatar = ''
  resumeStore.scheduleAutoSave()
}

// 教育经历
const addEducation = () => {
  content.value.education.push({
    school: '', major: '', degree: '', start: '', end: '', gpa: ''
  })
  resumeStore.scheduleAutoSave()
}

const removeEducation = (index: number) => {
  content.value.education.splice(index, 1)
  resumeStore.scheduleAutoSave()
}

// 工作经历
const addExperience = () => {
  content.value.experience.push({
    company: '', position: '', start: '', end: '', desc: ''
  })
  resumeStore.scheduleAutoSave()
}

const removeExperience = (index: number) => {
  content.value.experience.splice(index, 1)
  resumeStore.scheduleAutoSave()
}

// 项目经验
const addProject = () => {
  content.value.projects.push({
    name: '', role: '', start: '', end: '', desc: ''
  })
  resumeStore.scheduleAutoSave()
}

const removeProject = (index: number) => {
  content.value.projects.splice(index, 1)
  resumeStore.scheduleAutoSave()
}

// 技能
const showSkillInput = () => {
  inputVisible.value = true
  nextTick(() => {
    skillInputRef.value?.focus()
  })
}

const confirmSkill = () => {
  if (inputValue.value.trim()) {
    ;(content.value.skills as any).push(inputValue.value.trim())
    resumeStore.scheduleAutoSave()
  }
  inputVisible.value = false
  inputValue.value = ''
}

const removeSkill = (index: number) => {
  content.value.skills.splice(index, 1)
  resumeStore.scheduleAutoSave()
}

// 表单聚焦时捕获撤回快照
const onFormFocus = () => {
  resumeStore.captureUndo()
}
</script>

<style scoped>
.edit-form {
  max-width: 600px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
}

.form-section {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #eee;
}

.form-section:last-child {
  border-bottom: none;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.item-card {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 13px;
  color: #666;
}

.skills-input-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.skill-tag {
  margin-right: 0;
}

.base-info-layout {
  display: flex;
  gap: 24px;
}

.info-form {
  flex: 1;
  min-width: 0;
}

.avatar-upload-col {
  flex-shrink: 0;
  width: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 4px;
}

.avatar-uploader {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border: 2px dashed var(--el-border-color);
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.2s;
  background: #fafafa;
}

.avatar-uploader:hover {
  border-color: var(--el-color-primary);
}

.avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: #999;
  font-size: 11px;
}

.avatar-file-input {
  display: none;
}

.avatar-hint {
  margin: 4px 0 0;
  font-size: 11px;
  color: #999;
  text-align: center;
}
</style>
