<template>
  <div class="markdown-body" v-html="renderedContent"></div>
</template>

<script setup>
import { computed, watch, nextTick } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import { ElMessage } from 'element-plus'
import { sanitizeHtml } from '@/utils/sanitize'

const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})

// 解码 HTML 实体
function decodeHtmlEntities(text) {
  if (!text) return ''
  const textarea = document.createElement('textarea')
  textarea.innerHTML = text
  return textarea.value
}

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true,
  headerIds: false,
  mangle: false,
  sanitize: false,
  highlight: function(code, lang) {
    // 先解码 HTML 实体
    const decodedCode = decodeHtmlEntities(code)
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(decodedCode, { language: lang }).value
      } catch (e) {
        console.warn('Highlight error:', e)
      }
    }
    return hljs.highlightAuto(decodedCode).value
  }
})

// 渲染 Markdown（输出经 XSS 过滤）
const renderedContent = computed(() => {
  if (!props.content) return ''
  // 先解码内容中的 HTML 实体
  const decodedContent = decodeHtmlEntities(props.content)
  const rawHtml = marked.parse(decodedContent) || ''
  return sanitizeHtml(rawHtml)
})

// 添加代码复制按钮
function addCopyButtons() {
  nextTick(() => {
    const codeBlocks = document.querySelectorAll('.markdown-body pre')
    codeBlocks.forEach(pre => {
      // 检查是否已经有复制按钮
      if (pre.querySelector('.copy-code-btn')) return

      const code = pre.querySelector('code')
      if (!code) return

      const btn = document.createElement('button')
      btn.className = 'copy-code-btn'
      btn.textContent = '复制'
      btn.onclick = () => {
        const text = code.textContent || ''
        navigator.clipboard.writeText(text).then(() => {
          ElMessage.success('代码已复制')
          btn.textContent = '已复制!'
          setTimeout(() => {
            btn.textContent = '复制'
          }, 2000)
        }).catch(() => {
          ElMessage.error('复制失败')
        })
      }

      pre.style.position = 'relative'
      pre.appendChild(btn)
    })
  })
}

// 监听内容变化，添加复制按钮
watch(() => props.content, () => {
  addCopyButtons()
}, { immediate: true })
</script>

<style scoped>
.markdown-body {
  font-size: 14px;
  line-height: 1.8;
  color: #333;
}

/* 标题样式 */
.markdown-body :deep(h1) {
  font-size: 20px;
  font-weight: 600;
  margin: 16px 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8e8e8;
}

.markdown-body :deep(h2) {
  font-size: 18px;
  font-weight: 600;
  margin: 14px 0 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #f0f0f0;
}

.markdown-body :deep(h3) {
  font-size: 16px;
  font-weight: 600;
  margin: 12px 0 8px;
}

.markdown-body :deep(h4) {
  font-size: 15px;
  font-weight: 600;
  margin: 10px 0 6px;
}

/* 段落 */
.markdown-body :deep(p) {
  margin: 8px 0;
}

/* 列表 */
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}

.markdown-body :deep(li) {
  margin: 4px 0;
}

/* 加粗 */
.markdown-body :deep(strong) {
  font-weight: 600;
  color: #1a1a1a;
}

/* 斜体 */
.markdown-body :deep(em) {
  font-style: italic;
}

/* 代码块 */
.markdown-body :deep(pre) {
  background: #f6f8fa;
  border-radius: 6px;
  padding: 12px 16px;
  margin: 12px 0;
  overflow-x: auto;
  position: relative;
}

.markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
  font-size: 13px;
  line-height: 1.6;
}

/* 复制按钮 */
.markdown-body :deep(.copy-code-btn) {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 4px 12px;
  font-size: 12px;
  color: #666;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}

.markdown-body :deep(pre:hover .copy-code-btn) {
  opacity: 1;
}

.markdown-body :deep(.copy-code-btn:hover) {
  background: #f0f0f0;
  color: #333;
}

/* 行内代码 */
.markdown-body :deep(code) {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 13px;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  color: #d73a49;
}

/* 引用块 */
.markdown-body :deep(blockquote) {
  margin: 12px 0;
  padding: 8px 16px;
  border-left: 4px solid #dfe2e5;
  background: #f9f9f9;
  color: #666;
}

/* 分割线 */
.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid #e1e4e8;
  margin: 16px 0;
}

/* 表格 */
.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #dfe2e5;
  padding: 8px 12px;
  text-align: left;
}

.markdown-body :deep(th) {
  background: #f6f8fa;
  font-weight: 600;
}

/* 链接 */
.markdown-body :deep(a) {
  color: #0366d6;
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

/* 图片 */
.markdown-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
}

/* 任务列表 */
.markdown-body :deep(input[type="checkbox"]) {
  margin-right: 6px;
}

/* 特殊标记 */
.markdown-body :deep(.hljs) {
  background: transparent !important;
}
</style>
