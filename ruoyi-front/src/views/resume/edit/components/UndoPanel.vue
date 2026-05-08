<template>
  <div class="undo-panel">
    <el-button
      class="undo-btn"
      :disabled="resumeStore.undoStack.length === 0"
      circle
      title="撤回 (Ctrl+Z)"
      @click="resumeStore.undo()"
    >
      <el-icon><RefreshLeft /></el-icon>
    </el-button>
    <el-button
      class="redo-btn"
      :disabled="resumeStore.redoStack.length === 0"
      circle
      title="重做 (Ctrl+Shift+Z)"
      @click="resumeStore.redo()"
    >
      <el-icon><RefreshRight /></el-icon>
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { useResumeStore } from '@/store/resume'
import { RefreshLeft, RefreshRight } from '@element-plus/icons-vue'

const resumeStore = useResumeStore()
</script>

<style scoped>
.undo-panel {
  position: fixed;
  bottom: 24px;
  right: 80px;
  z-index: 999;
  display: flex;
  gap: 6px;
}

.undo-btn,
.redo-btn {
  box-shadow: 0 2px 8px rgba(0,0,0,0.12);
  background: #fff;
  font-size: 16px;
}

.undo-btn:hover,
.redo-btn:hover {
  border-color: #409eff;
  color: #409eff;
}
</style>
