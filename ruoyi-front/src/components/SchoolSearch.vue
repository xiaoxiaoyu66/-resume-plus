<template>
  <el-select
    :model-value="modelValue"
    @update:model-value="handleSelect"
    filterable
    remote
    :remote-method="debouncedSearch"
    :loading="loading"
    allow-create
    reserve-keyword
    clearable
    placeholder="搜索学校名称（支持手动输入）"
    style="width: 100%"
    @clear="handleClear"
  >
    <el-option
      v-for="item in options"
      :key="item.id"
      :label="item.name"
      :value="item.name"
    >
      <div class="school-option">
        <span class="school-name">{{ item.name }}</span>
        <span class="school-meta">
          {{ item.city }}
          <template v-if="item.city && item.level"> · </template>
          {{ item.level !== '普通本科' ? item.level : '' }}
        </span>
      </div>
    </el-option>
  </el-select>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { searchSchools, type SchoolItem } from '@/api/school'

const props = defineProps<{
  modelValue: string
  schoolId?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'update:schoolId': [value: number | null]
}>()

const options = ref<SchoolItem[]>([])
const loading = ref(false)

let debounceTimer: ReturnType<typeof setTimeout> | null = null

function debouncedSearch(query: string) {
  if (debounceTimer) clearTimeout(debounceTimer)
  if (!query.trim()) {
    options.value = []
    return
  }
  debounceTimer = setTimeout(async () => {
    loading.value = true
    try {
      options.value = await searchSchools(query.trim())
    } finally {
      loading.value = false
    }
  }, 300)
}

function handleSelect(val: string | number) {
  // 用户从列表中选择
  const selected = options.value.find(o => o.name === val)
  if (selected) {
    emit('update:schoolId', selected.id)
  } else {
    // allow-create 手动输入
    emit('update:schoolId', null)
  }
  emit('update:modelValue', String(val))
}

function handleClear() {
  options.value = []
  emit('update:modelValue', '')
  emit('update:schoolId', null)
}
</script>

<style scoped>
.school-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 2px 0;
}

.school-name {
  font-size: 14px;
  color: #333;
}

.school-meta {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
}
</style>
