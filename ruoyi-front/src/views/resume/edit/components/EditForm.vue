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
            <DatePicker v-model="content.baseInfo.birth" />
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
      <div
        v-for="(edu, index) in content.education"
        :key="index"
        class="item-card"
        :class="{ 'drag-over': dragState.education?.over === index }"
        draggable="true"
        @dragstart="onArrayDragStart('education', index)"
        @dragover="onArrayDragOver($event, 'education', index)"
        @dragleave="onArrayDragLeave('education')"
        @drop="onArrayDrop('education', index)"
        @dragend="onArrayDragEnd('education')"
      >
        <div class="item-header">
          <span class="drag-handle-item">
            <el-icon><Rank /></el-icon>
            经历 {{ index + 1 }}
          </span>
          <el-button type="danger" link size="small" @click="removeEducation(index)">
            <el-icon><Delete /></el-icon>删除
          </el-button>
        </div>
        <el-form :model="edu" label-width="80px" size="default">
          <el-form-item label="学校" required>
            <SchoolSearch v-model="edu.school" :school-id="edu.schoolId" @update:school-id="v => edu.schoolId = v" />
          </el-form-item>
          <el-form-item label="专业" required>
            <el-input v-model="edu.major" placeholder="专业名称" />
          </el-form-item>
          <el-form-item label="学历" required>
            <el-input v-model="edu.degree" placeholder="如：本科、硕士" />
          </el-form-item>
          <el-form-item label="时间段" required>
            <DateRangePicker :model-value="{ start: edu.start, end: edu.end }" @update:model-value="v => { edu.start = v.start; edu.end = v.end }" />
          </el-form-item>
          <el-form-item label="GPA">
            <el-input v-model="edu.gpa" placeholder="如：3.8/4.0 · 前10%" />
          </el-form-item>
          <el-form-item label="主修课程">
            <div class="courses-input-wrap">
              <el-tag
                v-for="(c, ci) in edu.courses"
                :key="ci"
                closable
                size="small"
                class="course-tag"
                @close="removeCourse(edu, ci)"
              >{{ c }}</el-tag>
              <el-input
                v-if="edu._courseInputVisible"
                ref="courseInputRef"
                v-model="edu._courseValue"
                size="small"
                style="width: 100px"
                @keyup.enter="confirmCourse(edu)"
                @blur="confirmCourse(edu)"
              />
              <el-button v-else type="primary" link size="small" @click="showCourseInput(edu)">
                <el-icon><Plus /></el-icon>添加
              </el-button>
            </div>
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
      <div
        v-for="(exp, index) in content.experience"
        :key="index"
        class="item-card"
        :class="{ 'drag-over': dragState.experience?.over === index }"
        draggable="true"
        @dragstart="onArrayDragStart('experience', index)"
        @dragover="onArrayDragOver($event, 'experience', index)"
        @dragleave="onArrayDragLeave('experience')"
        @drop="onArrayDrop('experience', index)"
        @dragend="onArrayDragEnd('experience')"
      >
        <div class="item-header">
          <span class="drag-handle-item">
            <el-icon><Rank /></el-icon>
            经历 {{ index + 1 }}
          </span>
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
            <DateRangePicker :model-value="{ start: exp.start, end: exp.end }" @update:model-value="v => { exp.start = v.start; exp.end = v.end }" />
          </el-form-item>
          <el-form-item label="工作描述" required>
            <div class="star-hint">💡 建议按 STAR 格式：情境 → 任务 → 行动 → 量化结果</div>
            <MarkdownTextarea v-model="exp.desc" :placeholder="'如：独立负责用户中心重构，日活 50w+，接口响应从 800ms 降到 120ms'" />
          </el-form-item>
        </el-form>
      </div>
      <el-empty v-if="content.experience.length === 0" description="暂无工作经历" :image-size="60" />
    </section>

    <!-- 校园经历 -->
    <section id="section-campus" class="form-section" v-show="moduleVisibility.campus" :style="{ order: getOrder('campus') }">
      <h3 class="section-title">
        校园经历
        <el-button type="primary" link size="small" @click="addCampus">
          <el-icon><Plus /></el-icon>添加
        </el-button>
      </h3>
      <div
        v-for="(item, index) in content.campus"
        :key="index"
        class="item-card"
        :class="{ 'drag-over': dragState.campus?.over === index }"
        draggable="true"
        @dragstart="onArrayDragStart('campus', index)"
        @dragover="onArrayDragOver($event, 'campus', index)"
        @dragleave="onArrayDragLeave('campus')"
        @drop="onArrayDrop('campus', index)"
        @dragend="onArrayDragEnd('campus')"
      >
        <div class="item-header">
          <span class="drag-handle-item">
            <el-icon><Rank /></el-icon>
            经历 {{ index + 1 }}
          </span>
          <el-button type="danger" link size="small" @click="removeCampus(index)">
            <el-icon><Delete /></el-icon>删除
          </el-button>
        </div>
        <el-form :model="item" label-width="80px" size="default">
          <el-form-item label="组织" required>
            <el-input v-model="item.organization" placeholder="如：学生会/社团/志愿者团队" />
          </el-form-item>
          <el-form-item label="职位" required>
            <el-input v-model="item.position" placeholder="如：外联部部长" />
          </el-form-item>
          <el-form-item label="时间段" required>
            <DateRangePicker :model-value="{ start: item.start, end: item.end }" @update:model-value="v => { item.start = v.start; item.end = v.end }" />
          </el-form-item>
          <el-form-item label="活动描述" required>
            <div class="star-hint">💡 突出组织规模、活动影响、量化成果</div>
            <MarkdownTextarea v-model="item.desc" :placeholder="'如：独立策划 300 人校园技术讲座，邀请 3 位企业嘉宾，活动满意度 92%'" />
          </el-form-item>
        </el-form>
      </div>
      <el-empty v-if="content.campus.length === 0" description="暂无校园经历" :image-size="60" />
    </section>

    <!-- 项目经验 -->
    <section id="section-projects" class="form-section" v-show="moduleVisibility.projects" :style="{ order: getOrder('projects') }">
      <h3 class="section-title">
        项目经验
        <el-button type="primary" link size="small" @click="addProject">
          <el-icon><Plus /></el-icon>添加
        </el-button>
      </h3>
      <div
        v-for="(proj, index) in content.projects"
        :key="index"
        class="item-card"
        :class="{ 'drag-over': dragState.projects?.over === index }"
        draggable="true"
        @dragstart="onArrayDragStart('projects', index)"
        @dragover="onArrayDragOver($event, 'projects', index)"
        @dragleave="onArrayDragLeave('projects')"
        @drop="onArrayDrop('projects', index)"
        @dragend="onArrayDragEnd('projects')"
      >
        <div class="item-header">
          <span class="drag-handle-item">
            <el-icon><Rank /></el-icon>
            项目 {{ index + 1 }}
          </span>
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
            <DateRangePicker :model-value="{ start: proj.start, end: proj.end }" @update:model-value="v => { proj.start = v.start; proj.end = v.end }" />
          </el-form-item>
          <el-form-item label="项目描述" required>
            <div class="star-hint">💡 建议按 STAR 格式：背景 → 任务 → 行动 → 量化结果</div>
            <MarkdownTextarea v-model="proj.desc" :placeholder="'如：基于 Spring Boot + Vue 开发校园二手交易平台，上线 2 个月注册用户 2000+，日均订单 50+'" />
          </el-form-item>
        </el-form>
      </div>
      <el-empty v-if="content.projects.length === 0" description="暂无项目经验" :image-size="60" />
    </section>

    <!-- 荣誉奖项 -->
    <section id="section-awards" class="form-section" v-show="moduleVisibility.awards" :style="{ order: getOrder('awards') }">
      <h3 class="section-title">
        荣誉奖项
        <el-button type="primary" link size="small" @click="addAward">
          <el-icon><Plus /></el-icon>添加
        </el-button>
      </h3>
      <div
        v-for="(award, index) in content.awards"
        :key="index"
        class="item-card"
        :class="{ 'drag-over': dragState.awards?.over === index }"
        draggable="true"
        @dragstart="onArrayDragStart('awards', index)"
        @dragover="onArrayDragOver($event, 'awards', index)"
        @dragleave="onArrayDragLeave('awards')"
        @drop="onArrayDrop('awards', index)"
        @dragend="onArrayDragEnd('awards')"
      >
        <div class="item-header">
          <span class="drag-handle-item">
            <el-icon><Rank /></el-icon>
            奖项 {{ index + 1 }}
          </span>
          <el-button type="danger" link size="small" @click="removeAward(index)">
            <el-icon><Delete /></el-icon>删除
          </el-button>
        </div>
        <el-form :model="award" label-width="80px" size="default">
          <el-form-item label="奖项名称" required>
            <el-input v-model="award.name" placeholder="如：全国大学生数学建模竞赛一等奖" />
          </el-form-item>
          <el-form-item label="获奖时间">
            <el-date-picker v-model="award.date" type="month" placeholder="获奖时间" value-format="YYYY.MM" style="width: 100%" />
          </el-form-item>
          <el-form-item label="级别">
            <el-select v-model="award.level" placeholder="请选择">
              <el-option label="国家级" value="国家级" />
              <el-option label="省级" value="省级" />
              <el-option label="市级" value="市级" />
              <el-option label="校级" value="校级" />
              <el-option label="院级" value="院级" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <el-empty v-if="content.awards.length === 0" description="暂无荣誉奖项" :image-size="60" />
    </section>

    <!-- 证书 -->
    <section id="section-certificates" class="form-section" v-show="moduleVisibility.certificates" :style="{ order: getOrder('certificates') }">
      <h3 class="section-title">
        证书
        <el-button type="primary" link size="small" @click="addCertificate">
          <el-icon><Plus /></el-icon>添加
        </el-button>
      </h3>
      <div
        v-for="(cert, index) in content.certificates"
        :key="index"
        class="item-card"
        :class="{ 'drag-over': dragState.certificates?.over === index }"
        draggable="true"
        @dragstart="onArrayDragStart('certificates', index)"
        @dragover="onArrayDragOver($event, 'certificates', index)"
        @dragleave="onArrayDragLeave('certificates')"
        @drop="onArrayDrop('certificates', index)"
        @dragend="onArrayDragEnd('certificates')"
      >
        <div class="item-header">
          <span class="drag-handle-item">
            <el-icon><Rank /></el-icon>
            证书 {{ index + 1 }}
          </span>
          <el-button type="danger" link size="small" @click="removeCertificate(index)">
            <el-icon><Delete /></el-icon>删除
          </el-button>
        </div>
        <el-form :model="cert" label-width="80px" size="default">
          <el-form-item label="证书名称" required>
            <el-input v-model="cert.name" placeholder="如：大学英语六级 (CET-6)" />
          </el-form-item>
          <el-form-item label="获得时间">
            <el-date-picker v-model="cert.date" type="month" placeholder="获得时间" value-format="YYYY.MM" style="width: 100%" />
          </el-form-item>
        </el-form>
      </div>
      <el-empty v-if="content.certificates.length === 0" description="暂无证书" :image-size="60" />
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
      <div class="star-hint" style="margin-bottom: 8px;">💡 用 3-4 点概括核心优势，每点有事实依据</div>
      <MarkdownTextarea v-model="content.evaluation" :rows="5" placeholder="如：计算机专业前 10%，3 个项目经验，熟悉 Java/Spring 技术栈，善于团队协作" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete, Rank } from '@element-plus/icons-vue'
import { useResumeStore } from '@/store/resume'
import MarkdownTextarea from './MarkdownTextarea.vue'
import DateRangePicker from './DateRangePicker.vue'
import DatePicker from './DatePicker.vue'
import SchoolSearch from '@/components/SchoolSearch.vue'
import type { EducationEntry } from '@/types/resume'

const resumeStore = useResumeStore()
const content = computed(() => resumeStore.content)
const moduleVisibility = computed(() => resumeStore.moduleVisibility)

const getOrder = (key: string) => resumeStore.moduleOrder.indexOf(key as any)

const inputVisible = ref(false)
const inputValue = ref('')
const skillInputRef = ref<HTMLElement | null>(null)
const avatarInputRef = ref<HTMLElement | null>(null)
const courseInputRef = ref<HTMLElement | null>(null)

// 可拖拽条目状态
const dragState = reactive<Record<string, { from: number; over: number }>>({})
function getDragState(module: string) {
  if (!dragState[module]) dragState[module] = { from: -1, over: -1 }
  return dragState[module]
}
function onArrayDragStart(module: string, index: number) {
  getDragState(module).from = index
}
function onArrayDragOver(e: DragEvent, module: string, index: number) {
  const ds = getDragState(module)
  if (ds.from === index) return
  e.preventDefault()
  if (e.dataTransfer) e.dataTransfer.dropEffect = 'move'
  ds.over = index
}
function onArrayDragLeave(module: string) {
  getDragState(module).over = -1
}
function onArrayDrop(module: string, index: number) {
  const ds = getDragState(module)
  if (ds.from >= 0 && ds.from !== index) {
    resumeStore.moveArrayEntry(module, ds.from, index)
  }
  ds.from = -1
  ds.over = -1
}
function onArrayDragEnd(module: string) {
  const ds = getDragState(module)
  ds.from = -1
  ds.over = -1
}

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
    school: '', schoolId: null, major: '', degree: '', start: '', end: '', gpa: '', courses: []
  })
  resumeStore.scheduleAutoSave()
}

const removeEducation = (index: number) => {
  content.value.education.splice(index, 1)
  resumeStore.scheduleAutoSave()
}

// 主修课程
function showCourseInput(edu: any) {
  edu._courseValue = ''
  edu._courseInputVisible = true
  nextTick(() => {
    courseInputRef.value?.focus()
  })
}

function confirmCourse(edu: any) {
  const val = edu._courseValue?.trim()
  if (val) {
    if (!Array.isArray(edu.courses)) edu.courses = []
    edu.courses.push(val)
    resumeStore.scheduleAutoSave()
  }
  edu._courseValue = ''
  edu._courseInputVisible = false
}

function removeCourse(edu: any, index: number) {
  if (Array.isArray(edu.courses)) {
    edu.courses.splice(index, 1)
    resumeStore.scheduleAutoSave()
  }
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

// 校园经历
const addCampus = () => {
  content.value.campus.push({
    organization: '', position: '', start: '', end: '', desc: ''
  })
  resumeStore.scheduleAutoSave()
}

const removeCampus = (index: number) => {
  content.value.campus.splice(index, 1)
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

// 荣誉奖项
const addAward = () => {
  content.value.awards.push({
    name: '', date: '', level: ''
  })
  resumeStore.scheduleAutoSave()
}

const removeAward = (index: number) => {
  content.value.awards.splice(index, 1)
  resumeStore.scheduleAutoSave()
}

// 证书
const addCertificate = () => {
  content.value.certificates.push({
    name: '', date: ''
  })
  resumeStore.scheduleAutoSave()
}

const removeCertificate = (index: number) => {
  content.value.certificates.splice(index, 1)
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
  gap: 4px;
}

.form-section {
  background: #ffffff;
  border-radius: 14px;
  padding: 24px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(37, 99, 235, 0.06), 0 1px 2px rgba(37, 99, 235, 0.04);
  border: 1px solid rgba(37, 99, 235, 0.06);
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.form-section:hover {
  border-color: rgba(37, 99, 235, 0.12);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.06), 0 2px 4px rgba(37, 99, 235, 0.04);
}

.form-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e40af;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  letter-spacing: 0.3px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(37, 99, 235, 0.08);
}

.item-card {
  background: #f0f7ff;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 12px;
  border: 1px solid rgba(37, 99, 235, 0.06);
  transition: border-color 0.2s, opacity 0.15s, box-shadow 0.15s;
  cursor: default;
}

.item-card:hover {
  border-color: rgba(37, 99, 235, 0.15);
}

.item-card:last-child {
  margin-bottom: 0;
}

.item-card[draggable="true"] {
  cursor: grab;
}
.item-card[draggable="true"]:active {
  cursor: grabbing;
  opacity: 0.85;
}
.item-card.drag-over {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.15);
  background: #eff6ff;
}

.drag-handle-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #60a5fa;
  font-weight: 500;
}
.drag-handle-item .el-icon {
  cursor: grab;
  color: #bfdbfe;
  font-size: 14px;
}
.drag-handle-item .el-icon:hover {
  color: #2563eb;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 13px;
  color: #60a5fa;
  font-weight: 500;
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
  border: 2px dashed rgba(37, 99, 235, 0.2);
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.2s, box-shadow 0.2s;
  background: #f0f7ff;
}

.avatar-uploader:hover {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
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
  color: #93c5fd;
  font-size: 11px;
}

.avatar-file-input {
  display: none;
}

.avatar-hint {
  margin: 4px 0 0;
  font-size: 11px;
  color: #93c5fd;
  text-align: center;
}

/* STAR 提示 */
.star-hint {
  font-size: 11px;
  color: #93c5fd;
  margin-bottom: 4px;
  line-height: 1.5;
}

/* 主修课程 */
.courses-input-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  width: 100%;
}

.course-tag {
  margin-right: 0;
}
</style>
