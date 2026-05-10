<template>
  <div class="custom-date-range">
    <div class="range-group">
      <div class="range-col">
        <span class="range-label">开始</span>
        <div class="select-row">
          <select v-model="startYear" class="range-select" @change="emitStart">
            <option v-for="y in years" :key="y" :value="y">{{ y }}年</option>
          </select>
          <select v-model="startMonth" class="range-select" @change="emitStart">
            <option v-for="m in months" :key="m" :value="m">{{ m }}月</option>
          </select>
        </div>
      </div>
      <span class="range-sep">—</span>
      <div class="range-col">
        <span class="range-label">结束</span>
        <div class="select-row">
          <template v-if="isOngoing">
            <div class="ongoing-text">至今</div>
          </template>
          <template v-else>
            <select v-model="endYear" class="range-select" @change="emitEnd">
              <option v-for="y in years" :key="y" :value="y">{{ y }}年</option>
            </select>
            <select v-model="endMonth" class="range-select" @change="emitEnd">
              <option v-for="m in months" :key="m" :value="m">{{ m }}月</option>
            </select>
          </template>
        </div>
      </div>
      <label :class="['ongoing-toggle', { active: isOngoing }]">
        <input type="checkbox" :checked="isOngoing" @change="toggleOngoing" />
        至今
      </label>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

interface RangeValue {
  start: string
  end: string
}

const props = defineProps<{
  modelValue?: RangeValue
}>()

const emit = defineEmits<{
  'update:modelValue': [value: RangeValue]
}>()

const years = Array.from({ length: 31 }, (_, i) => 2000 + i)
const months = Array.from({ length: 12 }, (_, i) => i + 1)

function parseYM(val: string | undefined): { y: number; m: number } | null {
  if (!val) return null
  const parts = val.split('.')
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

const startParsed = computed(() => parseYM(props.modelValue?.start))
const endParsed = computed(() => parseYM(props.modelValue?.end))

const startYear = ref(startParsed.value?.y || new Date().getFullYear())
const startMonth = ref(startParsed.value?.m || 1)
const endYear = ref(endParsed.value?.y || new Date().getFullYear())
const endMonth = ref(endParsed.value?.m || 1)
const isOngoing = computed(() => props.modelValue?.end === '至今')

watch(startParsed, (v) => {
  if (v) { startYear.value = v.y; startMonth.value = v.m }
})
watch(endParsed, (v) => {
  if (v) { endYear.value = v.y; endMonth.value = v.m }
})

function emitStart() {
  emit('update:modelValue', {
    start: toYM(startYear.value, startMonth.value),
    end: props.modelValue?.end || ''
  })
}

function emitEnd() {
  emit('update:modelValue', {
    start: props.modelValue?.start || '',
    end: toYM(endYear.value, endMonth.value)
  })
}

function toggleOngoing(e: Event) {
  const checked = (e.target as HTMLInputElement).checked
  if (checked) {
    emit('update:modelValue', { start: props.modelValue?.start || '', end: '至今' })
  } else {
    emit('update:modelValue', { start: props.modelValue?.start || '', end: '' })
  }
}
</script>

<style scoped>
.custom-date-range {
  width: 100%;
}

.range-group {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.range-col {
  flex: 1;
  min-width: 0;
}

.range-label {
  display: block;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
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

.range-sep {
  flex-shrink: 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  padding-bottom: 8px;
  user-select: none;
}

.ongoing-text {
  height: 32px;
  display: flex;
  align-items: center;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 500;
  padding: 0 4px;
}

.ongoing-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  padding: 0 10px;
  height: 32px;
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  cursor: pointer;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  transition: all 0.15s;
  user-select: none;
  margin-bottom: 0;
}

.ongoing-toggle:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

.ongoing-toggle.active {
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
  color: #fff;
}

.ongoing-toggle input {
  display: none;
}
</style>
