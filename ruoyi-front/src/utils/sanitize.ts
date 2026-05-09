/**
 * 简易 HTML 安全过滤器
 * 防止 XSS 攻击：剥离脚本、事件处理器、javascript: 协议
 */

// 禁止的标签
const FORBIDDEN_TAGS = ['script', 'style', 'iframe', 'object', 'embed', 'form', 'input', 'textarea', 'select', 'button', 'link', 'meta', 'base']

// 禁止的属性前缀
const FORBIDDEN_ATTR_PREFIXES = ['on']

// 禁止的协议
const FORBIDDEN_PROTOCOLS = ['javascript:', 'data:', 'vbscript:']

export function sanitizeHtml(html: string): string {
  if (!html) return ''

  // 1. 用 DOMParser 解析
  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')

  // 2. 递归清理节点
  function cleanNode(node: Node): void {
    if (node.nodeType === Node.ELEMENT_NODE) {
      const el = node as HTMLElement

      // 移除禁止的标签
      if (FORBIDDEN_TAGS.includes(el.tagName.toLowerCase())) {
        el.remove()
        return
      }

      // 移除危险属性
      const attrs = Array.from(el.attributes)
      for (const attr of attrs) {
        const attrName = attr.name.toLowerCase()
        const attrValue = attr.value.toLowerCase()

        // 事件处理器: onclick, onerror, onload...
        if (FORBIDDEN_ATTR_PREFIXES.some(p => attrName.startsWith(p))) {
          el.removeAttribute(attr.name)
          continue
        }

        // 危险协议的链接
        if (['href', 'src', 'action', 'xlink:href'].includes(attrName)) {
          if (FORBIDDEN_PROTOCOLS.some(p => attrValue.startsWith(p))) {
            if (attrName === 'href') {
              el.setAttribute(attr.name, '#')
            } else {
              el.removeAttribute(attr.name)
            }
          }
        }
      }

      // 递归清理子节点
      Array.from(el.childNodes).forEach(cleanNode)
    } else if (node.nodeType === Node.TEXT_NODE) {
      // 文本节点安全，无需处理
    }
  }

  Array.from(doc.body.childNodes).forEach(cleanNode)

  return doc.body.innerHTML
}

// 更严格的版本：只允许安全的 HTML 标签子集（用于 resume 编辑器预览）
const SAFE_TAGS = new Set([
  'p', 'br', 'strong', 'b', 'em', 'i', 'u', 's', 'del', 'ins',
  'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'ul', 'ol', 'li',
  'blockquote', 'pre', 'code',
  'hr', 'div', 'span',
  'table', 'thead', 'tbody', 'tr', 'th', 'td',
  'a', 'img',
  'dl', 'dt', 'dd',
  'sub', 'sup', 'small', 'mark', 'abbr',
])

export function sanitizeHtmlStrict(html: string): string {
  if (!html) return ''

  const parser = new DOMParser()
  const doc = parser.parseFromString(html, 'text/html')

  function cleanNode(node: Node): void {
    if (node.nodeType === Node.ELEMENT_NODE) {
      const el = node as HTMLElement

      // 不在白名单中的标签 → 只保留其文本内容
      if (!SAFE_TAGS.has(el.tagName.toLowerCase())) {
        const text = el.textContent || ''
        const textNode = doc.createTextNode(text)
        el.parentNode?.replaceChild(textNode, el)
        return
      }

      // 清理属性
      const attrs = Array.from(el.attributes)
      for (const attr of attrs) {
        const attrName = attr.name.toLowerCase()
        const attrValue = attr.value.toLowerCase()

        if (FORBIDDEN_ATTR_PREFIXES.some(p => attrName.startsWith(p))
            || FORBIDDEN_PROTOCOLS.some(p => attrValue.startsWith(p))) {
          el.removeAttribute(attr.name)
        }
      }

      // 特定标签额外限制
      if (el.tagName.toLowerCase() === 'a') {
        // 只保留 href 属性
        if (!el.getAttribute('href') || el.getAttribute('href')!.startsWith('#')) {
          el.removeAttribute('href')
        }
      }
      if (el.tagName.toLowerCase() === 'img') {
        // img 只允许 src 和 alt
        el.removeAttribute('width')
        el.removeAttribute('height')
        el.removeAttribute('style')
      }

      Array.from(el.childNodes).forEach(cleanNode)
    }
  }

  Array.from(doc.body.childNodes).forEach(cleanNode)
  return doc.body.innerHTML
}
