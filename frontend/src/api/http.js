/**
 * 前端 HTTP 底座：统一 /api 前缀、JSON 编解码与错误归一化。
 * 页面与组件不直接调用 fetch，一切请求经由本模块，便于统一处理错误形状与鉴权扩展。
 */

/** 接口前缀；开发态由 vite.config.js 代理到后端，生产部署需由同域网关提供相同前缀。 */
const API_BASE = '/api'

/**
 * 发起一次 JSON 请求并解析响应。
 *
 * @param {string} path 以 / 开头的接口路径，不含 /api 前缀
 * @param {object} [options] 请求选项
 * @param {string} [options.method] HTTP 方法，默认 GET
 * @param {object} [options.body] 请求体，存在时按 JSON 序列化并附带 Content-Type
 * @param {AbortSignal} [options.signal] 取消信号，用于丢弃过期请求
 * @returns {Promise<any>} 解析后的响应体；204 无内容时返回 null
 * @throws {Error} 网络失败、HTTP 非 2xx 或响应体不是合法 JSON 时抛出；error.code 为后端业务码；
 *                 调用方主动取消时抛出 name 为 AbortError 的异常，由调用方自行忽略
 */
export async function request(path, { method = 'GET', body, signal } = {}) {
  const headers = { Accept: 'application/json' }
  if (body !== undefined && body !== null) {
    headers['Content-Type'] = 'application/json'
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    // 取消信号透传给 fetch：调用方切换筛选时能真正中止在途请求，而不是只丢弃响应。
    signal,
    body: body === undefined || body === null ? undefined : JSON.stringify(body),
  })

  // 删除接口以 204 表示成功且无响应体，提前返回可避免把空字符串当成 JSON 解析失败。
  if (response.status === 204) {
    return null
  }

  // 先取文本再尝试解析：后端在极端情况下可能返回代理生成的 HTML 错误页，
  // 直接 response.json() 会抛出难以定位的 SyntaxError。
  const raw = await response.text()
  let payload = null
  if (raw) {
    try {
      payload = JSON.parse(raw)
    } catch {
      payload = null
    }
  }

  if (!response.ok) {
    // 后端统一错误体为 { code, message, timestamp }，优先使用其中的可展示文案。
    const message = payload && payload.message
      ? payload.message
      : `后端响应异常：HTTP ${response.status}`
    const error = new Error(message)
    error.code = payload && payload.code ? payload.code : `HTTP_${response.status}`
    error.status = response.status
    throw error
  }

  return payload
}
