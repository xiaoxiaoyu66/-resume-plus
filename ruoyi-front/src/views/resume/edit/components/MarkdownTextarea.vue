<template>
  <div class="rich-textarea" :class="{ focused: isFocused }">
    <div class="rt-toolbar" v-show="mode === 'edit'">
      <div class="toolbar-actions">
        <el-tooltip content="AI 润色" placement="top">
          <el-button
            text
            size="small"
            :loading="aiLoading"
            :disabled="aiLoading"
            @click="handleAiPolish"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M12 2l1.5 5.5L19 9l-5.5 1.5L12 16l-1.5-5.5L5 9l5.5-1.5z"/>
              <path d="M8 14l-3 6"/>
              <path d="M16 14l3 6"/>
            </svg>
          </el-button>
        </el-tooltip>
        <span class="toolbar-divider" />
        <el-tooltip content="加粗" placement="top">
          <el-button text size="small" @click="exec('bold')" :class="{ active: activeTags.b }">
            <strong>B</strong>
          </el-button>
        </el-tooltip>
        <el-tooltip content="斜体" placement="top">
          <el-button text size="small" @click="exec('italic')" :class="{ active: activeTags.i }">
            <em>I</em>
          </el-button>
        </el-tooltip>
        <el-tooltip content="删除线" placement="top">
          <el-button text size="small" @click="exec('strikeThrough')" :class="{ active: activeTags.s }">
            <span style="text-decoration: line-through">S</span>
          </el-button>
        </el-tooltip>
        <span class="toolbar-divider" />
        <el-tooltip content="无序列表" placement="top">
          <el-button text size="small" @click="exec('insertUnorderedList')" :class="{ active: activeTags.ul }">
            <svg width="14" height="14" viewBox="0 0 14 14"><circle cx="3" cy="7" r="1.5" fill="currentColor"/><rect x="6" y="6.5" width="8" height="1" fill="currentColor"/></svg>
          </el-button>
        </el-tooltip>
        <el-tooltip content="有序列表" placement="top">
          <el-button text size="small" @click="exec('insertOrderedList')" :class="{ active: activeTags.ol }">
            <svg width="14" height="14" viewBox="0 0 14 14"><text x="0" y="10" font-size="9" font-weight="bold" fill="currentColor">1.</text><rect x="6" y="6.5" width="8" height="1" fill="currentColor"/></svg>
          </el-button>
        </el-tooltip>
        <span class="toolbar-divider" />
        <el-tooltip content="撤销" placement="top">
          <el-button text size="small" @click="exec('undo')">
            <svg width="14" height="14" viewBox="0 0 14 14"><path d="M4.5 3L1 6l3.5 3" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/><path d="M1 6h8a3 3 0 010 6H7" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
          </el-button>
        </el-tooltip>
        <el-tooltip content="重做" placement="top">
          <el-button text size="small" @click="exec('redo')">
            <svg width="14" height="14" viewBox="0 0 14 14"><path d="M9.5 3L13 6l-3.5 3" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/><path d="M13 6H5a3 3 0 000 6h2" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
          </el-button>
        </el-tooltip>
      </div>
      <div class="toolbar-preview">
        <el-button text size="small" @click="mode = 'preview'">
          预览
        </el-button>
      </div>
    </div>
    <div class="editor-area">
      <div
        v-show="mode === 'edit'"
        ref="editorRef"
        class="rt-editor"
        contenteditable
        @input="onInput"
        @focus="isFocused = true"
        @blur="onBlur"
        @keydown="onKeydown"
        @paste="onPaste"
      />
      <div v-show="mode === 'preview'" class="rt-preview" @click="mode = 'edit'">
        <div v-if="previewHtml" v-html="previewHtml"></div>
        <span v-else class="empty-hint">暂无内容，点击返回编辑</span>
      </div>
      <div v-if="!localValue && mode === 'edit' && !isFocused" class="editor-placeholder">{{ placeholder }}</div>
    </div>
    <div v-if="aiKeywords.length" class="ai-keywords">
      <span class="keywords-label">推荐关键词：</span>
      <el-tag
        v-for="kw in aiKeywords"
        :key="kw"
        size="small"
        type="primary"
        class="keyword-tag"
      >{{ kw }}</el-tag>
      <el-button text size="small" class="keywords-close" @click="aiKeywords = []">
        关闭
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted, onBeforeUnmount, nextTick, reactive } from 'vue'
import { ElMessage as ElMsg } from 'element-plus'
import { marked } from 'marked'
import { aiResumeAction } from '@/api/resume'

interface ActiveTags {
  b: boolean
  i: boolean
  s: boolean
  ul: boolean
  ol: boolean
}

const props = defineProps<{
  modelValue: string
  rows?: number
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const mode = ref('edit')
const localValue = ref(props.modelValue)
const isFocused = ref(false)
const editorRef = ref<HTMLElement | null>(null)
const activeTags = reactive<ActiveTags>({ b: false, i: false, s: false, ul: false, ol: false })
const aiLoading = ref(false)
const aiKeywords = ref<string[]>([])

const previewHtml = computed(() => {
  const html = localValue.value || ''
  if (/<[a-z][\s\S]*>/i.test(html)) {
    return html
  }
  return marked(html) as string
})

function exec(command: string) {
  document.execCommand(command, false)
  editorRef.value?.focus()
  syncContent()
  nextTick(updateActiveTags)
}

function syncContent() {
  if (!editorRef.value) return
  const html = editorRef.value.innerHTML
  if (html === '<br>') {
    localValue.value = ''
    emit('update:modelValue', '')
  } else {
    localValue.value = html
    emit('update:modelValue', html)
  }
}

function onInput() {
  syncContent()
}

function onBlur() {
  isFocused.value = false
  syncContent()
  if (editorRef.value) {
    const text = editorRef.value.textContent || ''
    if (!text.trim()) {
      editorRef.value.innerHTML = ''
    }
  }
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Tab') {
    e.preventDefault()
    document.execCommand('insertHTML', false, '  ')
  }
}

function onPaste(e: ClipboardEvent) {
  e.preventDefault()
  const text = (e.clipboardData || (window as any).clipboardData).getData('text/plain')
  document.execCommand('insertText', false, text)
}

async function handleAiPolish() {
  if (!editorRef.value) return
  const plainText = editorRef.value.textContent || ''
  if (!plainText.trim()) {
    ElMsg.warning('没有内容需要优化')
    return
  }
  aiLoading.value = true
  try {
    const res = await aiResumeAction({ action: 'polish', text: plainText }) as any
    const data = res.data || res
    if (data && data.polished) {
      const polished = data.polished.replace(/\n/g, '<br>')
      editorRef.value.innerHTML = polished
      syncContent()
      ElMsg.success('润色完成')
    }
    if (data && data.keywords && data.keywords.length) {
      aiKeywords.value = data.keywords
    }
  } catch (err: any) {
    console.error('AI润色失败:', err)
    ElMsg.error('润色失败: ' + (err.message || '未知错误'))
  } finally {
    aiLoading.value = false
  }
}

function updateActiveTags() {
  activeTags.b = document.queryCommandState('bold')
  activeTags.i = document.queryCommandState('italic')
  activeTags.s = document.queryCommandState('strikeThrough')
  activeTags.ul = document.queryCommandState('insertUnorderedList')
  activeTags.ol = document.queryCommandState('insertOrderedList')
}

let tagTimer: ReturnType<typeof setInterval> | null = null

watch(() => props.modelValue, (val: string) => {
  localValue.value = val
  if (editorRef.value && val !== editorRef.value.innerHTML) {
    editorRef.value.innerHTML = val || ''
  }
})

watch(mode, (val: string) => {
  if (val === 'edit' && editorRef.value) {
    editorRef.value.innerHTML = localValue.value || ''
    nextTick(() => editorRef.value?.focus())
  }
})

onMounted(() => {
  if (editorRef.value) {
    editorRef.value.innerHTML = props.modelValue || ''
  }
  tagTimer = setInterval(updateActiveTags, 300)
})

onBeforeUnmount(() => {
  if (tagTimer) clearInterval(tagTimer)
})
</script>

<style scoped>
.rich-textarea {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  overflow: hidden;
  transition: border-color 0.2s;
  position: relative;
}

.rich-textarea.focused {
  border-color: var(--el-color-primary);
}

.rt-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px;
  background: #f9f9f9;
  border-bottom: 1px solid var(--el-border-color);
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;
}

.toolbar-actions .el-button {
  font-size: 12px;
  min-width: 28px;
  height: 28px;
  padding: 0 4px;
  color: #555;
}

.toolbar-actions .el-button.active {
  background: #e6f2ff;
  color: var(--el-color-primary);
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background: #ddd;
  margin: 0 4px;
}

.toolbar-preview .el-button {
  font-size: 12px;
  color: var(--el-color-primary);
}

.editor-area {
  position: relative;
}

.rt-editor {
  padding: 12px 16px;
  min-height: 100px;
  background: #fff;
  font-size: 14px;
  line-height: 1.8;
  outline: none;
  cursor: text;
}

.rt-editor:empty::before {
  content: attr(data-placeholder);
  color: #bbb;
  pointer-events: none;
}

.editor-placeholder {
  position: absolute;
  top: 12px;
  left: 16px;
  color: #bbb;
  font-size: 14px;
  pointer-events: none;
}

.rt-preview {
  padding: 12px 16px;
  min-height: 100px;
  background: #fff;
  font-size: 14px;
  line-height: 1.8;
  color: #333;
  cursor: pointer;
}

.rt-preview :deep(p) { margin: 6px 0; }
.rt-preview :deep(ul),
.rt-preview :deep(ol) { padding-left: 24px; margin: 6px 0; }
.rt-preview :deep(blockquote) {
  border-left: 3px solid #ddd;
  padding-left: 12px;
  margin: 8px 0;
  color: #666;
}

.empty-hint {
  color: #ccc;
  font-size: 13px;
}

.ai-keywords {
  padding: 6px 12px;
  background: #f0f7ff;
  border-top: 1px solid #d0e4f7;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.keywords-label {
  font-size: 12px;
  color: #666;
  flex-shrink: 0;
}

.keyword-tag {
  margin: 0;
}

.keywords-close {
  margin-left: auto;
  flex-shrink: 0;
}
</style>
