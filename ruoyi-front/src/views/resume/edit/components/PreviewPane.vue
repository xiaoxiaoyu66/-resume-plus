<template>
  <div class="preview-pane">
    <div class="a4-paper" id="resume-preview" :style="previewStyle">
      <component
        :is="currentTemplate"
        :content="resumeStore.content"
        :visibility="resumeStore.moduleVisibility"
        :module-order="resumeStore.moduleOrder"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useResumeStore } from '@/store/resume'
import ModernTemplate from './templates/ModernTemplate.vue'
import ClassicTemplate from './templates/ClassicTemplate.vue'
import MinimalTemplate from './templates/MinimalTemplate.vue'

const resumeStore = useResumeStore()

const currentTemplate = computed(() => {
  const map = {
    modern: ModernTemplate,
    classic: ClassicTemplate,
    minimal: MinimalTemplate
  }
  return map[resumeStore.templateId] || ModernTemplate
})

const previewStyle = computed(() => {
  const s = resumeStore.style
  const vars = {}
  if (s.fontFamily) vars['--resume-font'] = s.fontFamily
  if (s.fontSize) vars['--resume-font-size'] = s.fontSize + 'px'
  if (s.lineHeight) vars['--resume-line-height'] = s.lineHeight
  if (s.primaryColor) vars['--resume-primary'] = s.primaryColor
  if (s.color) vars['--resume-color'] = s.color
  return vars
})
</script>

<style scoped>
.preview-pane {
  display: flex;
  justify-content: center;
  min-height: 100%;
}

.a4-paper {
  width: 210mm;
  min-height: 297mm;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  padding: 20mm;
  box-sizing: border-box;
}
</style>
