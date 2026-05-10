<template>
  <div class="module-nav">
    <div class="nav-header">模块导航</div>
    <div class="nav-list">
      <div
        v-for="(mod, index) in orderedModules"
        :key="mod.key"
        :draggable="true"
        class="nav-item"
        :class="{
          active: activeModule === mod.key,
          'drag-over': dragOverIndex === index,
          dragging: draggingIndex === index
        }"
        @click="scrollToModule(mod.key)"
        @dragstart="onDragStart($event, index)"
        @dragend="onDragEnd"
        @dragover="onDragOver($event, index)"
        @dragleave="onDragLeave(index)"
        @drop="onDrop($event, index)"
      >
        <span class="drag-handle">
          <el-icon><Rank /></el-icon>
        </span>
        <span class="nav-dot" :class="{ 'is-visible': resumeStore.moduleVisibility[mod.key] }" />
        <span class="nav-label">{{ mod.label }}</span>
        <span class="nav-actions">
          <el-icon
            v-if="mod.arrayType"
            class="add-icon"
            title="添加条目"
            @click.stop="addEntry(mod.key)"
          >
            <Plus />
          </el-icon>
          <el-icon
            v-if="mod.canHide"
            class="eye-icon"
            @click.stop="toggleVisibility(mod.key)"
          >
            <View v-if="resumeStore.moduleVisibility[mod.key]" />
            <Hide v-else />
          </el-icon>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { View, Hide, Rank, Plus } from '@element-plus/icons-vue'
import { useResumeStore } from '@/store/resume'

interface ModuleMeta {
  key: string
  label: string
  canHide: boolean
  arrayType: boolean
}

const resumeStore = useResumeStore()
const activeModule = ref('baseInfo')

const moduleMeta: Record<string, ModuleMeta> = {
  baseInfo: { key: 'baseInfo', label: '基本信息', canHide: false, arrayType: false },
  intention: { key: 'intention', label: '求职意向', canHide: true, arrayType: false },
  education: { key: 'education', label: '教育经历', canHide: false, arrayType: true },
  experience: { key: 'experience', label: '工作经历', canHide: true, arrayType: true },
  campus: { key: 'campus', label: '校园经历', canHide: true, arrayType: true },
  projects: { key: 'projects', label: '项目经验', canHide: true, arrayType: true },
  awards: { key: 'awards', label: '荣誉奖项', canHide: true, arrayType: true },
  certificates: { key: 'certificates', label: '证书', canHide: true, arrayType: true },
  skills: { key: 'skills', label: '技能特长', canHide: true, arrayType: false },
  evaluation: { key: 'evaluation', label: '自我评价', canHide: true, arrayType: false }
}

const orderedModules = computed(() => {
  return resumeStore.moduleOrder
    .map(key => moduleMeta[key])
    .filter(Boolean)
})

const draggingIndex = ref(-1)
const dragOverIndex = ref(-1)

const onDragStart = (event: DragEvent, index: number) => {
  draggingIndex.value = index
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(index))
  }
}

const onDragEnd = () => {
  draggingIndex.value = -1
  dragOverIndex.value = -1
}

const onDragOver = (event: DragEvent, index: number) => {
  if (draggingIndex.value === index) return
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
  dragOverIndex.value = index
}

const onDragLeave = (index: number) => {
  if (dragOverIndex.value === index) {
    dragOverIndex.value = -1
  }
}

const onDrop = (event: DragEvent, index: number) => {
  event.preventDefault()
  const fromIndex = parseInt(event.dataTransfer?.getData('text/plain') || '', 10)
  if (!isNaN(fromIndex) && fromIndex !== index) {
    resumeStore.moveModule(fromIndex, index)
  }
  onDragEnd()
}

const scrollToModule = (key: string) => {
  activeModule.value = key
  const el = document.getElementById(`section-${key}`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const toggleVisibility = (key: string) => {
  resumeStore.toggleModule(key)
}

const addEntry = (key: string) => {
  resumeStore.addArrayEntry(key)
}
</script>

<style scoped>
.module-nav {
  width: 180px;
  flex-shrink: 0;
  background: #ffffff;
  border-right: 1px solid rgba(37, 99, 235, 0.08);
  padding: 16px 0;
  overflow-y: auto;
}

.nav-header {
  font-size: 12px;
  font-weight: 600;
  color: #93c5fd;
  padding: 0 16px 12px;
  border-bottom: 1px solid rgba(37, 99, 235, 0.08);
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.nav-list {
  padding: 0 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 8px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
  gap: 6px;
  user-select: none;
  color: #6b7280;
}

.nav-item:hover {
  background: #f0f7ff;
  color: #2563eb;
}

.nav-item.active {
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 500;
}

.nav-item.dragging {
  opacity: 0.4;
}

.nav-item.drag-over {
  border-top: 2px solid #2563eb;
  background: #f0f7ff;
}

.drag-handle {
  cursor: grab;
  color: #bfdbfe;
  display: flex;
  align-items: center;
  font-size: 14px;
  flex-shrink: 0;
}

.drag-handle:hover {
  color: #2563eb;
}

.nav-item:active .drag-handle {
  cursor: grabbing;
}

.nav-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #dbeafe;
  flex-shrink: 0;
}

.nav-dot.is-visible {
  background: #10b981;
}

.nav-label {
  flex: 1;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.add-icon {
  font-size: 13px;
  color: #bfdbfe;
  cursor: pointer;
}

.add-icon:hover {
  color: #10b981;
}

.eye-icon {
  font-size: 14px;
  color: #bfdbfe;
  cursor: pointer;
  flex-shrink: 0;
}

.eye-icon:hover {
  color: #2563eb;
}
</style>
