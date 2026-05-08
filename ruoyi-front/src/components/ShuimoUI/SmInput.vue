<template>
  <div class="sm-input-wrapper">
    <label v-if="label" class="sm-input__label">{{ label }}</label>
    <div :class="['sm-input', { 'sm-input--focused': isFocused, 'sm-input--disabled': disabled }]">
      <textarea
        v-if="type === 'textarea'"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :rows="rows"
        @input="handleInput"
        @focus="isFocused = true"
        @blur="isFocused = false"
        @keydown="handleKeydown"
      />
      <input
        v-else
        :type="type"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        @input="handleInput"
        @focus="isFocused = true"
        @blur="isFocused = false"
        @keydown="handleKeydown"
      />
    </div>
    <div v-if="maxlength" class="sm-input__count">
      {{ modelValue?.length || 0 }} / {{ maxlength }}
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

/**
 * 水墨画风格输入框组件
 * @description 宣纸风格的输入框
 */
const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: 'text'
  },
  label: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  },
  rows: {
    type: Number,
    default: 3
  },
  maxlength: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'keydown', 'enter'])

const isFocused = ref(false)

function handleInput(e) {
  let value = e.target.value
  if (props.maxlength && value.length > props.maxlength) {
    value = value.slice(0, props.maxlength)
    e.target.value = value
  }
  emit('update:modelValue', value)
}

function handleKeydown(e) {
  emit('keydown', e)
  if (e.key === 'Enter' && !e.shiftKey) {
    emit('enter', e)
  }
}
</script>

<style scoped lang="scss">
@import './styles/variables.scss';
@import './styles/mixins.scss';

.sm-input-wrapper {
  width: 100%;
}

.sm-input__label {
  display: block;
  margin-bottom: 8px;
  font-size: $font-size-base;
  color: $ink-mid;
  font-weight: 500;
  font-family: $font-family;
}

.sm-input {
  position: relative;
  background: $paper-white;
  border: 1px solid $border-mid;
  border-radius: $radius-sm;
  transition: all $transition-fast;

  // 水墨晕染效果
  &::before {
    content: '';
    position: absolute;
    top: -1px;
    left: 20%;
    right: 20%;
    height: 2px;
    background: linear-gradient(90deg, transparent 0%, $ink-primary 50%, transparent 100%);
    opacity: 0;
    transition: opacity $transition-fast;
  }

  &--focused {
    border-color: $ink-primary;
    box-shadow: 0 0 0 3px rgba(14, 50, 101, 0.08);

    &::before {
      opacity: 0.3;
    }
  }

  &--disabled {
    background: $paper-mid;
    cursor: not-allowed;

    input, textarea {
      cursor: not-allowed;
      color: $ink-pale;
    }
  }

  input, textarea {
    width: 100%;
    padding: 10px 14px;
    font-size: $font-size-base;
    font-family: $font-family;
    color: $ink-deep;
    background: transparent;
    border: none;
    outline: none;
    resize: none;

    &::placeholder {
      color: $ink-pale;
    }
  }

  textarea {
    line-height: 1.7;
  }
}

.sm-input__count {
  text-align: right;
  font-size: $font-size-xs;
  color: $ink-pale;
  margin-top: 4px;
}
</style>
