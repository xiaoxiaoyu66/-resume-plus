/**
 * 水墨画风格 UI 组件库
 * Shuimo UI - 基于 Vue 3 的水墨画风格组件库
 *
 * 设计灵感：中国传统水墨画
 * 主色调：RGB(14, 50, 101) 靛蓝墨韵
 */

import SmButton from './SmButton.vue'
import SmCard from './SmCard.vue'
import SmInput from './SmInput.vue'
import SmMessage from './SmMessage.vue'
import SmLoading from './SmLoading.vue'

// 组件列表
const components = [
  SmButton,
  SmCard,
  SmInput,
  SmMessage,
  SmLoading
]

// 全局注册
const install = (app) => {
  components.forEach(component => {
    app.component(component.name || component.__name, component)
  })
}

// 按需导出
export {
  SmButton,
  SmCard,
  SmInput,
  SmMessage,
  SmLoading
}

// 默认导出
export default {
  install,
  ...components
}
