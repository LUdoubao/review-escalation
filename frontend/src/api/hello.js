/**
 * 后端连通性接口请求层：集中维护 /api 前缀与 HTTP 错误处理，页面与组件不直接调用 fetch。
 * 与 backend/src/main/java/com/example/baseline/controller/HelloController.java 构成跨栈契约面。
 */

/** 接口前缀；开发态由 vite.config.js 代理到后端，生产部署需由同域网关提供相同前缀。 */
const API_BASE = '/api'

/**
 * 请求 GET /api/hello 并解析响应体。
 *
 * @returns {Promise<{message: string, service: string, timestamp: string}>} 后端响应契约对象
 * @throws {Error} 网络失败、HTTP 非 2xx 或响应体不是合法 JSON 时抛出，页面据此进入 error 状态
 */
export async function fetchHello() {
  const response = await fetch(`${API_BASE}/hello`, {
    method: 'GET',
    // 显式声明只接受 JSON，避免后端返回 HTML 错误页时被当作成功数据解析。
    headers: { Accept: 'application/json' },
  })

  // fetch 仅在断网等网络层错误时 reject，4xx/5xx 仍会 resolve；
  // 不显式判 ok 会把错误响应体当成正常契约数据交给渲染层，产生难以定位的展示异常。
  if (!response.ok) {
    throw new Error(`后端响应异常：HTTP ${response.status}`)
  }

  // 基线不做字段级校验：字段集合由跨栈契约保证，后续扩展时再在此层集中补校验。
  return response.json()
}
