<template>
  <span
    ref="elRef"
    contenteditable="true"
    class="editable-text"
    :class="{ 'is-empty': !localValue && !modelValue }"
    :placeholder="placeholder"
    @focus="onFocus"
    @blur="onBlur"
    @input="onInput"
    @compositionstart="isComposing = true"
    @compositionend="onCompositionEnd"
    @keydown.enter.prevent="onEnter"
  >{{ displayText }}</span>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  multiline: { type: Boolean, default: false },
  tag: { type: String, default: 'span' }
})

const emit = defineEmits(['update:modelValue', 'blur', 'focus', 'enter'])

const elRef = ref(null)
const isComposing = ref(false)
const localValue = ref('')
const isFocused = ref(false)

const displayText = ref('')

// Sync display text from modelValue when not focused
watch(() => props.modelValue, (val) => {
  if (!isFocused.value) {
    displayText.value = val || ''
  }
}, { immediate: true })

function onFocus() {
  isFocused.value = true
  localValue.value = displayText.value
  emit('focus')
}

function onBlur() {
  isFocused.value = false
  const text = elRef.value?.textContent || ''
  if (text !== props.modelValue) {
    emit('update:modelValue', text)
  }
  displayText.value = text || ''
  emit('blur')
}

function onInput(e) {
  if (isComposing.value) return
  localValue.value = e.target.textContent || ''
  displayText.value = localValue.value
}

function onCompositionEnd(e) {
  isComposing.value = false
  localValue.value = e.target.textContent || ''
  displayText.value = localValue.value
  emit('update:modelValue', localValue.value)
}

function onEnter(e) {
  if (props.multiline) {
    document.execCommand('insertLineBreak')
  } else {
    elRef.value?.blur()
  }
  emit('enter')
}

function focus() {
  elRef.value?.focus()
}

defineExpose({ focus })
</script>

<style scoped>
.editable-text {
  outline: none;
  cursor: text;
  min-width: 8px;
  min-height: 1em;
  border-radius: 2px;
  transition: background 0.15s, box-shadow 0.15s;
  word-break: break-word;
}

.editable-text:hover {
  background: rgba(64, 158, 255, 0.04);
}

.editable-text:focus {
  background: rgba(64, 158, 255, 0.06);
  box-shadow: 0 0 0 1.5px rgba(64, 158, 255, 0.25);
}

.editable-text.is-empty::before {
  content: attr(placeholder);
  color: #ccc;
  font-style: italic;
  pointer-events: none;
}
</style>
