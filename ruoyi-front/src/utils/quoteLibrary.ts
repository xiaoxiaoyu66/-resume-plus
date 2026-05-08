/**
 * 简历评分语录库
 * 高分鼓励 / 低分调侃 / AI 生成
 */

// 高分语录（评分 >= 80）
const highScoreQuotes = [
  '当你觉得不行的时候，就去路上走，这样就是一个行人了',
  '你前途这么黑暗，是因为小时候把光借给了迪迦',
  '我的简历，面试邀请率100%，剩下的是HR自卑了不敢发',
  '如果三天内没收到面试邀请，建议检查HR是否失明',
  '这份简历要是不过，建议HR检查一下自己是不是真人',
  '转发这条简历到三个群，三天内必收到面试',
  '金简历一个宝贝',
  '绝命春招打最后一批hr的人来',
  '都是同龄人我原本没想酱胃大鸡',
  '简历已生成，HR看了连夜把公司过户给我'
]

// 低分语录（评分 < 60）
const lowScoreQuotes = [
  '拉完了牢弟，给我擦皮鞋',
  '菜就多去学习牢弟'
]

// 中等分数语录（60-79）
const midScoreQuotes = [
  '还行，但也就还行',
  'HR看了说：差强人意，字面意思',
  '距离offer就差一个"再改改"',
  '中规中矩，属于是HR不会直接pass但也不会眼前一亮'
]

/**
 * 随机取一句
 * @param {number} score 评分（0-100）
 * @returns {string}
 */
export function getRandomQuote(score) {
  let pool
  if (score >= 80) {
    pool = highScoreQuotes
  } else if (score >= 60) {
    pool = midScoreQuotes
  } else {
    pool = lowScoreQuotes
  }
  return pool[Math.floor(Math.random() * pool.length)]
}

/**
 * 构建 AI 生成新语录的 prompt
 * @param {number} score 评分
 * @returns {string}
 */
export function buildQuotePrompt(score) {
  const pool = score >= 80 ? highScoreQuotes : score >= 60 ? midScoreQuotes : lowScoreQuotes
  const examples = pool.join('\n')
  return `你是一个幽默的简历语录生成器。参考以下语录风格，生成一句新的调侃/鼓励性质的句子，用在简历评分后的展示场景。风格要接地气、带点自嘲和夸张。只返回句子本身，不要加引号或前缀。

参考风格：
${examples}`
}

/**
 * 获取金色祝福语（评分 >= 80 时导出展示）
 */
export const goldBlessing = '小何在此，祝您offer拿到手软'
