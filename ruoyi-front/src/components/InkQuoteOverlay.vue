<template>
  <transition name="ink-fade">
    <div v-if="visible" class="ink-float-characters" @click="dismiss">
      <!-- 长语录 → 双列 -->
      <div v-if="totalLength > 10" class="ink-two-cols">
        <div class="ink-col">
          <span
            v-for="(ch, i) in col1"
            :key="'l'+i"
            class="ink-char"
            :style="{ animationDelay: `${i * 0.08}s` }"
          >{{ ch }}</span>
        </div>
        <div class="ink-col-gap" />
        <div class="ink-col">
          <span
            v-for="(ch, i) in col2"
            :key="'r'+i"
            class="ink-char"
            :style="{ animationDelay: `${(col1.length + i) * 0.08}s` }"
          >{{ ch }}</span>
        </div>
      </div>
      <!-- 短语录 → 单列居中 -->
      <div v-else class="ink-single">
        <span
          v-for="(ch, i) in shownChars"
          :key="i"
          class="ink-char"
          :style="{ animationDelay: `${i * 0.08}s` }"
        >{{ ch }}</span>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { getRandomQuote } from '@/utils/quoteLibrary'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  score: { type: Number, default: 0 }
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(false)
const shownChars = ref<any[]>([])
const totalLength = ref(0)

const mid = computed(() => Math.ceil(totalLength.value / 2))
const col1 = computed(() => shownChars.value.slice(0, mid.value))
const col2 = computed(() => shownChars.value.slice(mid.value))

let typeTimer = null
let dismissTimer = null

watch(() => props.modelValue, (val) => {
  if (val) {
    const full = getRandomQuote(props.score)
    totalLength.value = full.length
    shownChars.value = []
    visible.value = true
    let i = 0
    typeTimer = setInterval(() => {
      if (i < full.length) {
        shownChars.value.push(full[i])
        i++
      } else {
        clearInterval(typeTimer)
        typeTimer = null
        dismissTimer = setTimeout(dismiss, 3000)
      }
    }, 80)
  } else {
    visible.value = false
  }
})

function dismiss() {
  clearInterval(typeTimer)
  typeTimer = null
  clearTimeout(dismissTimer)
  dismissTimer = null
  visible.value = false
  setTimeout(() => {
    emit('update:modelValue', false)
  }, 400)
}

onBeforeUnmount(() => {
  clearInterval(typeTimer)
  clearTimeout(dismissTimer)
})
</script>

<style scoped>
.ink-float-characters {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
  padding: 40px;
  animation: bgIn 0.6s ease-out;
  background: radial-gradient(ellipse at center, rgba(40, 40, 40, 0.06) 0%, transparent 70%);
}

/* 单列 */
.ink-single {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

/* 双列 */
.ink-two-cols {
  display: flex;
  align-items: flex-start;
  justify-content: center;
}

.ink-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.ink-col-gap {
  width: 60px;
  flex-shrink: 0;
}

.ink-char {
  font-family: 'STKaiti', 'KaiTi', '楷体', 'Noto Serif SC', serif;
  font-size: 48px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.4;
  opacity: 0;
  animation: charAppear 0.5s ease-out forwards;
  text-shadow: 2px 2px 8px rgba(0, 0, 0, 0.08);
  letter-spacing: 0.1em;
}

.ink-char:nth-child(odd) {
  transform-origin: left center;
}

.ink-char:nth-child(even) {
  transform-origin: right center;
}

@keyframes charAppear {
  0% {
    opacity: 0;
    transform: translateY(20px) scale(0.6);
    filter: blur(4px);
  }
  40% {
    opacity: 0.7;
    filter: blur(0);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
    filter: blur(0);
  }
}

@keyframes bgIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.ink-fade-enter-active { transition: opacity 0.3s; }
.ink-fade-leave-active { transition: opacity 0.6s; }
.ink-fade-enter-from,
.ink-fade-leave-to { opacity: 0; }
</style>
