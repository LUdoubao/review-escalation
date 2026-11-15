/**
 * 日期展示工具。
 *
 * 口径说明：后端 dueDate 为本地日期字符串（YYYY-MM-DD），不带时区偏移；
 * 时间戳字段为带偏移的 ISO-8601 字符串。本模块只负责展示格式化，
 * 不判定业务含义——是否逾期由后端 TaskResponse.overdue 统一给出，
 * 前端不再自行比较日期，避免与后端在时区与「当天是否算逾期」上分叉。
 */

/**
 * 补零到两位。
 *
 * @param {number} value 待补零的数字
 * @returns {string} 两位字符串
 */
function pad(value) {
  return value < 10 ? `0${value}` : String(value)
}

/**
 * 格式化为 YYYY-MM-DD，用于卡片与表格展示。
 *
 * @param {string|null|undefined} value 日期字符串
 * @returns {string} 格式化结果；空值返回「未设置」，无法解析时原样返回
 */
export function formatDate(value) {
  if (!value) {
    return '未设置'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/**
 * 格式化为 YYYY-MM-DD HH:mm，用于展示后端聚合时刻等时间戳。
 *
 * @param {string|null|undefined} value 时间戳字符串
 * @returns {string} 格式化结果；空值返回空串
 */
export function formatDateTime(value) {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/**
 * 取服务器无关的本地当天日期字符串，用于表单的「不得早于今天」校验。
 *
 * @returns {string} YYYY-MM-DD
 */
export function todayString() {
  const now = new Date()
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}
