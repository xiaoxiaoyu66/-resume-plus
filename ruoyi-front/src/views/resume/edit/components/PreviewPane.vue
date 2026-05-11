<template>
  <div class="preview-pane" :class="{ 'edit-mode': props.editMode }" ref="paneRef" @click="onPreviewClick">
    <div class="a4-paper" id="resume-preview" :style="previewStyle">
      <component
        :is="currentTemplate"
        :content="resumeStore.content"
        :visibility="resumeStore.moduleVisibility"
        :module-order="resumeStore.moduleOrder"
      />
    </div>

    <!-- Annotation badges overlay -->
    <div v-if="annotations.length" class="annotation-overlay">
      <div
        v-for="ann in annotations"
        :key="ann.id"
        class="ann-badge"
        :class="'ann-' + ann.type"
        :style="getBadgePos(ann)"
        :title="ann.message"
        @click.stop="$emit('section-click', ann)"
      >
        <span class="ann-dot" />
        <span class="ann-tooltip">{{ ann.message }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useResumeStore } from '@/store/resume'
import ModernTemplate from './templates/ModernTemplate.vue'
import ClassicTemplate from './templates/ClassicTemplate.vue'
import MinimalTemplate from './templates/MinimalTemplate.vue'

interface Annotation {
  id: string
  module: string
  index?: number
  type: 'warning' | 'improve' | 'success' | 'ai'
  message: string
}

const props = defineProps<{
  annotations?: Annotation[]
  focusedSection?: { module: string; index?: number } | null
  editMode?: boolean
}>()

const emit = defineEmits<{
  'section-click': [ann: Annotation]
  'preview-module-click': [module: string]
}>()

const resumeStore = useResumeStore()
const paneRef = ref<HTMLElement | null>(null)
const badgePositions = ref<Record<string, { top: number; left: number }>>({})

const currentTemplate = computed(() => {
  const map: Record<string, any> = {
    modern: ModernTemplate,
    classic: ClassicTemplate,
    minimal: MinimalTemplate
  }
  return map[resumeStore.templateId] || ModernTemplate
})

const previewStyle = computed(() => {
  const s = resumeStore.style
  const vars: Record<string, string> = {}
  if (s.fontFamily) vars['--resume-font'] = s.fontFamily
  if (s.fontSize) vars['--resume-font-size'] = s.fontSize + 'px'
  if (s.lineHeight) vars['--resume-line-height'] = String(s.lineHeight)
  if (s.primaryColor) vars['--resume-primary'] = s.primaryColor
  if (s.color) vars['--resume-color'] = s.color
  if (s.paperBackground) vars['--resume-paper'] = s.paperBackground
  return vars
})

function getBadgePos(ann: Annotation) {
  const pos = badgePositions.value[ann.id]
  if (!pos) return { display: 'none' }
  return { top: pos.top + 'px', left: pos.left + 'px' }
}

/** Click on preview → find data-section ancestor and emit for form sync */
function onPreviewClick(e: MouseEvent) {
  if (!props.editMode) return
  // Don't interfere with annotation clicks
  if ((e.target as HTMLElement)?.closest?.('.ann-badge')) return
  const sectionEl = (e.target as HTMLElement)?.closest?.('[data-section]') as HTMLElement | null
  if (sectionEl) {
    const module = sectionEl.dataset.section
    if (module) {
      emit('preview-module-click', module)
    }
  }
}

let resizeObserver: ResizeObserver | null = null

function computeBadgePositions() {
  if (!paneRef.value || !props.annotations?.length) return

  const a4 = paneRef.value.querySelector('.a4-paper') as HTMLElement
  if (!a4) return

  const paneRect = paneRef.value.getBoundingClientRect()
  const positions: Record<string, { top: number; left: number }> = {}

  for (const ann of props.annotations) {
    // Find the section element using data-section and optional data-section-index
    let selector = `[data-section="${ann.module}"]`
    if (ann.index !== undefined) {
      const items = paneRef.value!.querySelectorAll(
        `[data-section="${ann.module}"] [data-section-index]`
      )
      const el = items[ann.index] as HTMLElement | undefined
      if (el) {
        const rect = el.getBoundingClientRect()
        positions[ann.id] = {
          top: rect.top - paneRect.top + paneRef.value!.scrollTop,
          left: rect.right - paneRect.left + 6
        }
      }
    } else {
      const el = paneRef.value.querySelector(selector) as HTMLElement | null
      if (el) {
        const rect = el.getBoundingClientRect()
        positions[ann.id] = {
          top: rect.top - paneRect.top + paneRef.value!.scrollTop + 8,
          left: rect.right - paneRect.left + 6
        }
      }
    }
  }

  badgePositions.value = positions
}

/** Scroll to a section and flash-highlight it */
function scrollToSection(module: string, index?: number) {
  nextTick(() => {
    const pane = paneRef.value
    if (!pane) return

    let selector = `[data-section="${module}"]`
    let target: HTMLElement | null

    if (index !== undefined) {
      const items = pane.querySelectorAll(
        `[data-section="${module}"] [data-section-index]`
      )
      target = items[index] as HTMLElement | null
    } else {
      target = pane.querySelector(selector)
    }

    if (!target) return

    // Remove any existing highlight
    pane.querySelectorAll('.section-highlight').forEach(el => el.remove())

    target.scrollIntoView({ behavior: 'smooth', block: 'center' })
    target.classList.add('section-highlight')

    setTimeout(() => {
      target?.classList.remove('section-highlight')
    }, 2000)
  })
}

// Watch for focusedSection changes from parent
watch(() => props.focusedSection, (val) => {
  if (val) {
    scrollToSection(val.module, val.index)
  }
}, { deep: true })

// Recompute badge positions when annotations change or after render
watch(() => props.annotations, () => {
  nextTick(computeBadgePositions)
}, { deep: true })

// Periodic refresh for layout shifts
let refreshTimer: ReturnType<typeof setInterval> | null = null

function onPaneScroll() {
  computeBadgePositions()
}

onMounted(() => {
  computeBadgePositions()
  resizeObserver = new ResizeObserver(() => computeBadgePositions())
  if (paneRef.value) {
    resizeObserver.observe(paneRef.value)
    paneRef.value.addEventListener('scroll', onPaneScroll, { passive: true })
  }
  refreshTimer = setInterval(computeBadgePositions, 2000)
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  if (refreshTimer) clearInterval(refreshTimer)
  paneRef.value?.removeEventListener('scroll', onPaneScroll)
})
</script>

<style scoped>
.preview-pane {
  display: flex;
  justify-content: center;
  min-height: 100%;
  position: relative;
}

.a4-paper {
  width: 210mm;
  min-height: 297mm;
  background: var(--resume-paper, #fff);
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  padding: 20mm;
  box-sizing: border-box;
  transition: box-shadow 0.3s ease;
}

/* Annotation overlay */
.annotation-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 10;
}

.ann-badge {
  position: absolute;
  pointer-events: auto;
  cursor: pointer;
  z-index: 10;
  width: 22px;
  height: 22px;
}

.ann-dot {
  display: block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.25);
  transition: transform 0.2s ease;
  margin-top: 3px;
  margin-left: 3px;
}

.ann-badge:hover .ann-dot {
  transform: scale(1.35);
}

.ann-badge:hover .ann-tooltip {
  opacity: 1;
  transform: translateX(0);
  pointer-events: auto;
}

.ann-tooltip {
  position: absolute;
  top: -4px;
  left: 26px;
  white-space: nowrap;
  background: #1f2937;
  color: #fff;
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 6px;
  pointer-events: none;
  opacity: 0;
  transform: translateX(-4px);
  transition: opacity 0.2s ease, transform 0.2s ease;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  box-shadow: 0 2px 8px rgba(0,0,0,0.18);
}

/* Dot colors */
.ann-warning .ann-dot { background: #e6a23c; }
.ann-improve .ann-dot { background: #2563eb; }
.ann-success .ann-dot { background: #10b981; }
.ann-ai .ann-dot { background: #8b5cf6; }

/* Section highlight animation */
:deep(.section-highlight) {
  animation: section-pulse 2s ease;
  border-radius: 4px;
}

@keyframes section-pulse {
  0%   { box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.3); }
  50%  { box-shadow: 0 0 0 6px rgba(37, 99, 235, 0.08); }
  100% { box-shadow: 0 0 0 0 rgba(37, 99, 235, 0); }
}

/* Ensure section elements can be highlighted */
:deep(.section) {
  transition: box-shadow 0.3s ease;
  border-radius: 3px;
}

/* Edit mode: section hover cues */
.preview-pane.edit-mode :deep([data-section]) {
  position: relative;
  cursor: pointer;
  transition: outline 0.2s ease, background 0.2s ease;
  outline: 2px solid transparent;
  outline-offset: -1px;
  border-radius: 4px;
}
.preview-pane.edit-mode :deep([data-section]:hover) {
  outline-color: rgba(37, 99, 235, 0.3);
  background: rgba(37, 99, 235, 0.02);
}

/* Edit mode indicator floating badge */
.preview-pane.edit-mode::after {
  content: '✎ 编辑模式';
  position: absolute;
  top: 8px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 20;
  font-size: 11px;
  color: #2563eb;
  background: rgba(255,255,255,0.92);
  padding: 3px 12px;
  border-radius: 12px;
  border: 1px solid rgba(37, 99, 235, 0.2);
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  pointer-events: none;
  white-space: nowrap;
  backdrop-filter: blur(4px);
}
</style>
