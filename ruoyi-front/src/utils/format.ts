/**
 * 文本转义（XSS 防护）
 */
export function escapeHtml(text) {
  if (!text) return ''
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

/**
 * 当前时间完整格式 YYYY-MM-DD HH:mm:ss（同时用于显示和日期分隔）
 */
export function getCurrentTime() {
  const now = new Date()
  const y = now.getFullYear()
  const M = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const h = String(now.getHours()).padStart(2, '0')
  const m = String(now.getMinutes()).padStart(2, '0')
  const s = String(now.getSeconds()).padStart(2, '0')
  return `${y}-${M}-${d} ${h}:${m}:${s}`
}
