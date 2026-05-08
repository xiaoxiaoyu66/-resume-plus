/**
 * SVG 雪碧图加载器
 * 自动加载所有 SVG 图标文件
 */

// 加载水墨主题图标
const importAll = (r) => {
  r.keys().forEach(r)
}

try {
  // 加载 icons/svg 目录下的所有 SVG 文件
  importAll(require.context('@/assets/icons/svg', true, /\.svg$/))
} catch (e) {
  console.warn('SVG 图标加载失败:', e)
}

// 导出图标名称列表，方便查看
export const iconList = [
  'chat', 'user', 'file', 'folder', 'upload', 'search',
  'delete', 'download', 'close', 'menu', 'settings', 'plus',
  'arrow-right', 'arrow-left', 'document', 'time', 'calendar',
  'send', 'attachment', 'more', 'check', 'warning', 'info',
  'home', 'logout', 'edit', 'copy', 'refresh'
]

export default {
  install(app) {
    // 注册全局属性
    app.config.globalProperties.$icons = iconList
  }
}
