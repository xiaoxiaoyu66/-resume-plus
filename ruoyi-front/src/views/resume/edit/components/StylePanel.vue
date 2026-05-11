<template>
  <Transition name="panel-slide">
    <div v-if="isOpen" class="style-panel">
        <div class="style-header">
          <span class="style-title">样式设置</span>
          <div class="style-header-actions">
            <el-button size="small" text :icon="Refresh" @click="resetAll">重置</el-button>
            <el-button text :icon="Close" @click="isOpen = false" />
          </div>
        </div>

        <div class="style-scroll">
          <!-- 预设主题 -->
          <div class="style-section">
            <div class="section-label">配色主题</div>
            <div class="theme-grid">
              <div
                v-for="t in themes"
                :key="t.id"
                class="theme-card"
                :class="{ active: isThemeActive(t) }"
                @click="applyTheme(t)"
              >
                <div class="theme-swatches">
                  <span class="swatch-primary" :style="{ background: t.primaryColor }" />
                  <span class="swatch-text" :style="{ background: t.color }" />
                </div>
                <span class="theme-name">{{ t.label }}</span>
              </div>
            </div>
          </div>

          <!-- 自定义颜色 -->
          <div class="style-section">
            <div class="section-label">自定义颜色</div>
            <div class="color-fields">
              <div class="color-row">
                <label>主色</label>
                <div class="color-input-wrap">
                  <input
                    type="color"
                    :value="s.primaryColor || '#1a365d'"
                    @input="onPrimaryColorInput"
                    class="color-picker"
                  />
                  <input
                    class="color-hex-input"
                    :value="s.primaryColor"
                    @input="onPrimaryColorHex"
                    placeholder="默认"
                  />
                </div>
              </div>
              <div class="color-row">
                <label>文字色</label>
                <div class="color-input-wrap">
                  <input
                    type="color"
                    :value="s.color || '#2c3e50'"
                    @input="onTextColorInput"
                    class="color-picker"
                  />
                  <input
                    class="color-hex-input"
                    :value="s.color"
                    @input="onTextColorHex"
                    placeholder="默认"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- 纸张背景 -->
          <div class="style-section">
            <div class="section-label">纸张质感</div>
            <div class="paper-grid">
              <div
                v-for="p in paperOptions"
                :key="p.value"
                class="paper-card"
                :class="{ active: (s.paperBackground || '') === p.value }"
                @click="s.paperBackground = p.value"
              >
                <div class="paper-swatch" :style="{ background: p.color }" />
                <span class="paper-name">{{ p.label }}</span>
              </div>
            </div>
          </div>

          <!-- 字体排版 -->
          <div class="style-section">
            <div class="section-label">字体排版</div>
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
              <label>字号 {{ s.fontSize }}px</label>
              <el-slider v-model.number="s.fontSize" :min="9" :max="16" :step="0.5" size="small" />
            </div>
            <div class="style-group">
              <label>行距</label>
              <el-slider v-model.number="s.lineHeight" :min="1" :max="2.5" :step="0.1" show-input size="small" />
            </div>
          </div>
        </div>
      </div>
    </Transition>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useResumeStore } from '@/store/resume'
import { Close, Refresh } from '@element-plus/icons-vue'

interface ThemePreset {
  id: string
  label: string
  primaryColor: string
  color: string
}

const themes: ThemePreset[] = [
  { id: 'ink',     label: '墨韵', primaryColor: '#1a365d', color: '#2c3e50' },
  { id: 'cinnabar',label: '朱砂', primaryColor: '#8b1a1a', color: '#2c3e50' },
  { id: 'blue',    label: '青花', primaryColor: '#1e40af', color: '#2c3e50' },
  { id: 'green',   label: '竹青', primaryColor: '#2d6a4f', color: '#2c3e50' },
  { id: 'gold',    label: '鎏金', primaryColor: '#b8860b', color: '#2c3e50' },
  { id: 'rose',    label: '玫瑰', primaryColor: '#9d174d', color: '#2c3e50' },
]

const paperOptions = [
  { label: '纯白',   value: '',         color: '#ffffff' },
  { label: '米白',   value: '#faf8f5',  color: '#faf8f5' },
  { label: '宣纸',   value: '#f5f0e8',  color: '#f5f0e8' },
  { label: '羊皮纸', value: '#ede4d3',  color: '#ede4d3' },
]

const resumeStore = useResumeStore()

const props = withDefaults(defineProps<{ visible?: boolean }>(), { visible: false })
const emit = defineEmits<{ "update:visible": [v: boolean] }>()

const isOpen = ref(false)
watch(() => props.visible, (v) => { isOpen.value = v })
watch(isOpen, (v) => emit("update:visible", v))


const s = computed({
  get: () => resumeStore.style,
  set: (val) => { resumeStore.style = val }
})

function isThemeActive(t: ThemePreset): boolean {
  return s.value.primaryColor === t.primaryColor && s.value.color === t.color
}

function applyTheme(t: ThemePreset) {
  s.value.primaryColor = t.primaryColor
  s.value.color = t.color
}

function fromInput(e: Event): string {
  return (e.target as HTMLInputElement).value
}

function onPrimaryColorInput(e: Event) {
  s.value.primaryColor = fromInput(e) || ''
}
function onPrimaryColorHex(e: Event) {
  s.value.primaryColor = fromInput(e)
}
function onTextColorInput(e: Event) {
  s.value.color = fromInput(e) || ''
}
function onTextColorHex(e: Event) {
  s.value.color = fromInput(e)
}

function resetAll() {
  resumeStore.style = {
    fontFamily: '',
    fontSize: 12,
    lineHeight: 1.7,
    primaryColor: '',
    color: '',
    paperBackground: ''
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
  width: 300px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.14);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: 70vh;
}

.style-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.style-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.style-header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.style-scroll {
  padding: 12px 16px 16px;
  overflow-y: auto;
}

/* section */
.style-section {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f5f5f5;
}
.style-section:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.section-label {
  font-size: 12px;
  font-weight: 600;
  color: #888;
  margin-bottom: 10px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* theme grid */
.theme-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
}

.theme-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 6px 8px;
  border-radius: 8px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.15s ease;
  background: #fafafa;
}
.theme-card:hover {
  border-color: #dbeafe;
  background: #f8faff;
}
.theme-card.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.theme-swatches {
  display: flex;
  gap: 3px;
  align-items: center;
}
.swatch-primary {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 1px solid rgba(0,0,0,0.08);
}
.swatch-text {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1px solid rgba(0,0,0,0.08);
}

.theme-name {
  font-size: 11px;
  color: #555;
  font-weight: 500;
}

/* color fields */
.color-fields {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.color-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.color-row label {
  width: 48px;
  font-size: 13px;
  color: #555;
  flex-shrink: 0;
}

.color-input-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
}

.color-picker {
  width: 30px;
  height: 30px;
  padding: 0;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  background: none;
}
.color-picker::-webkit-color-swatch-wrapper {
  padding: 2px;
}
.color-picker::-webkit-color-swatch {
  border-radius: 4px;
  border: none;
}

.color-hex-input {
  flex: 1;
  height: 30px;
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 0 8px;
  font-size: 12px;
  outline: none;
  font-family: monospace;
  color: #333;
  transition: border-color 0.15s;
}
.color-hex-input:focus {
  border-color: #2563eb;
}

/* paper grid */
.paper-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  gap: 8px;
}

.paper-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 4px 6px;
  border-radius: 8px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.15s ease;
}
.paper-card:hover {
  border-color: #dbeafe;
}
.paper-card.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.paper-swatch {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  border: 1px solid #e5e5e5;
}

.paper-name {
  font-size: 10px;
  color: #666;
  font-weight: 500;
}

/* style groups (font) */
.style-group {
  margin-bottom: 10px;
}
.style-group:last-child {
  margin-bottom: 0;
}
.style-group label {
  display: block;
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

/* transition */
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
