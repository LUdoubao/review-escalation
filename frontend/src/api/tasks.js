/**
 * 任务看板接口层：唯一调用 /api/tasks 的模块，负责把页面参数拼成查询串、把路径拼全。
 * 与 backend/src/main/java/com/example/baseline/controller/TaskController.java 构成跨栈契约面。
 *
 * 任务对象字段：id / title / description / assignee / priority / priorityLabel /
 * status / statusLabel / terminal / tag / dueDate / sortOrder / overdue / version / updatedAt。
 * 其中 terminal 为「是否终态」：为 true 时任务已完成、不可再移动，前端据此禁用移动入口；
 * overdue 由服务端按业务时区派生，前端直接展示，不自行比较日期。
 */
import { request } from './http.js'

/**
 * 把筛选对象拼成查询串，空值不参与拼接，避免后端把空串当成有效筛选条件。
 *
 * @param {object} params 键值对形式的查询参数
 * @returns {string} 以 ? 起始的查询串；无有效参数时返回空串
 */
function toQueryString(params) {
  const search = new URLSearchParams()
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value === undefined || value === null) {
      return
    }
    const text = String(value).trim()
    if (text === '') {
      return
    }
    search.append(key, text)
  })
  const query = search.toString()
  return query ? `?${query}` : ''
}

/**
 * 分页查询任务列表。
 *
 * @param {object} params 查询参数，含 page / size / status / priority / assignee / keyword
 * @param {AbortSignal} [signal] 取消信号，切换筛选或页码时由 store 传入以中止在途请求
 * @returns {Promise<{items: object[], totalItems: number, page: number, size: number, totalPages: number}>}
 */
export function fetchTaskPage(params, signal) {
  return request(`/tasks${toQueryString(params)}`, { signal })
}

/**
 * 查询看板聚合数据。
 *
 * @param {object} params 筛选参数，含 keyword / assignee / priority
 * @param {AbortSignal} [signal] 取消信号，切换筛选或视图时由 store 传入
 * @returns {Promise<{columns: object[], generatedAt: string}>} 列数组与聚合时刻
 */
export function fetchBoard(params, signal) {
  return request(`/tasks/board${toQueryString(params)}`, { signal })
}

/**
 * 查询汇总指标。
 *
 * @returns {Promise<object>} 总数、各列数量、逾期数量与完成率
 */
export function fetchSummary() {
  return request('/tasks/summary')
}

/**
 * 查询负责人候选。
 *
 * @returns {Promise<string[]>} 负责人列表
 */
export function fetchAssignees() {
  return request('/tasks/assignees')
}

/**
 * 查询单个任务详情。
 *
 * @param {number|string} id 任务主键
 * @returns {Promise<object>} 任务出参
 */
export function fetchTask(id) {
  return request(`/tasks/${id}`)
}

/**
 * 新建任务。
 *
 * @param {object} payload 新建请求体
 * @returns {Promise<object>} 新建后的任务
 */
export function createTask(payload) {
  return request('/tasks', { method: 'POST', body: payload })
}

/**
 * 局部更新任务字段。
 *
 * @param {number|string} id 任务主键
 * @param {object} payload 编辑请求体，仅包含需要修改的字段
 * @returns {Promise<object>} 更新后的任务
 */
export function updateTask(id, payload) {
  return request(`/tasks/${id}`, { method: 'PATCH', body: payload })
}

/**
 * 移动任务到目标列的目标位置。
 *
 * @param {number|string} id 任务主键
 * @param {{targetStatus: string, position?: number, version?: number}} payload 移动请求体
 * @returns {Promise<object>} 移动后的任务
 */
export function moveTask(id, payload) {
  return request(`/tasks/${id}/move`, { method: 'PATCH', body: payload })
}

/**
 * 逻辑删除任务。
 *
 * @param {number|string} id 任务主键
 * @returns {Promise<null>} 成功时无响应体
 */
export function deleteTask(id) {
  return request(`/tasks/${id}`, { method: 'DELETE' })
}
