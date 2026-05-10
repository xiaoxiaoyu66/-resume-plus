<template>
  <div class="custom-date-picker">
    <div class="select-row">
      <select v-model="year" class="range-select" @change="emitValue">
        <option v-for="y in years" :key="y" :value="y">{{ y }}年</option>
      </select>
      <select v-model="month" class="range-select" @change="emitValue">
        <option v-for="m in months" :key="m" :value="m">{{ m }}月</option>
      </select>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  modelValue?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const years = Array.from({ length: 60 }, (_, i) => 1970 + i)
const months = Array.from({ length: 12 }, (_, i) => i + 1)

function parseYM(val: string | undefined): { y: number; m: number } | null {
  if (!val) return null
  // 兼容 YYYY.MM 和 YYYY-MM 两种格式
  const sep = val.includes('-') ? '-' : '.'
  const parts = val.split(sep)
  if (parts.length === 2) {
    const y = parseInt(parts[0])
    const m = parseInt(parts[1])
    if (!isNaN(y) && !isNaN(m)) return { y, m }
  }
  return null
}

function toYM(y: number, m: number): string {
  return `${y}.${String(m).padStart(2, '0')}`
}

const parsed = parseYM(props.modelValue)
const year = ref(parsed?.y || new Date().getFullYear())
const month = ref(parsed?.m || 1)

watch(() => props.modelValue, (v) => {
  const p = parseYM(v)
  if (p) { year.value = p.y; month.value = p.m }
})

function emitValue() {
  emit('update:modelValue', toYM(year.value, month.value))
}
</script>

<style scoped>
.custom-date-picker {
  width: 100%;
}

.select-row {
  display: flex;
  gap: 4px;
}

.range-select {
  flex: 1;
  min-width: 0;
  height: 32px;
  padding: 0 8px;
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  background: #fff;
  color: var(--el-text-color-primary);
  font-size: 13px;
  outline: none;
  cursor: pointer;
  transition: border-color 0.2s;
  -webkit-appearance: none;
  appearance: none;
}

.range-select:hover,
.range-select:focus {
  border-color: var(--el-color-primary);
}
</style>
