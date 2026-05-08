<template>
  <div class="style-panel" :class="{ open: isOpen }">
    <el-button class="style-trigger" :icon="Brush" circle @click="isOpen = !isOpen" />
    <Transition name="panel-slide">
      <div v-if="isOpen" class="style-body">
        <div class="style-header">
          <span>样式设置</span>
          <el-button text :icon="Close" @click="isOpen = false" />
        </div>
        <div class="style-content">
          <div class="style-group">
            <label>字体</label>
            <el-select v-model="s.fontFamily" placeholder="默认" size="small" clearable>
              <el-option label="默认" value="" />
              <el-option label="宋体" value="SimSun, Songti SC, serif" />
              <el-option label="黑体" value="SimHei, Heiti SC, sans-serif" />
              <el-option label="微软雅黑" value="Microsoft YaHei, PingFang SC, sans-serif" />
              <el-option label="Noto Serif" value="Noto Serif SC, STSong, serif" />
              <el-option label="衬线" value="Georgia, Times New Roman, serif" />
            </el-select>
          </div>
          <div class="style-group">
            <label>字号</label>
            <el-slider v-model.number="s.fontSize" :min="9" :max="16" :step="0.5" show-input size="small" />
          </div>
          <div class="style-group">
            <label>行距</label>
            <el-slider v-model.number="s.lineHeight" :min="1" :max="2.5" :step="0.1" show-input size="small" />
          </div>
          <div class="style-group">
            <label>主色</label>
            <el-input v-model="s.primaryColor" placeholder="默认" size="small" clearable>
              <template #prepend>
                <span class="color-swatch" :style="{ background: s.primaryColor || '#ccc' }" />
              </template>
            </el-input>
          </div>
          <div class="style-group">
            <label>文字颜色</label>
            <el-input v-model="s.color" placeholder="默认" size="small" clearable>
              <template #prepend>
                <span class="color-swatch" :style="{ background: s.color || '#333' }" />
              </template>
            </el-input>
          </div>
        </div>
        <div class="style-footer">
          <el-button size="small" @click="resetStyle">重置</el-button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useResumeStore } from '@/store/resume'
import { Brush, Close } from '@element-plus/icons-vue'

const resumeStore = useResumeStore()
const isOpen = ref(false)

const s = computed({
  get: () => resumeStore.style,
  set: (val) => { resumeStore.style = val }
})

function resetStyle() {
  resumeStore.style = {
    fontFamily: '',
    fontSize: 12,
    lineHeight: 1.7,
    primaryColor: '',
    color: ''
  }
  resumeStore.scheduleAutoSave()
}
</script>

<style scoped>
.style-panel {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 999;
}

.style-trigger {
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.style-body {
  position: absolute;
  bottom: 52px;
  right: 0;
  width: 280px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  overflow: hidden;
}

.style-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #eee;
}

.style-content {
  padding: 14px 16px;
}

.style-group {
  margin-bottom: 14px;
}

.style-group:last-child {
  margin-bottom: 0;
}

.style-group label {
  display: block;
  font-size: 12px;
  color: #666;
  margin-bottom: 6px;
}

.color-swatch {
  display: inline-block;
  width: 16px;
  height: 16px;
  border-radius: 3px;
  border: 1px solid #ddd;
  vertical-align: middle;
}

.style-footer {
  padding: 10px 16px;
  border-top: 1px solid #eee;
  text-align: center;
}

.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.panel-slide-enter-from,
.panel-slide-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
