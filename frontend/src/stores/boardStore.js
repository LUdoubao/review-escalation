/**
 * 任务看板状态容器：模块级 reactive 单例，是看板与列表两个视图的唯一状态源。
 *
 * 分层约定：组件只接收 props 并向外 emit，不直接调用 api；所有请求与状态写回都发生在本文件。
 * 状态写回顺序：先置请求中标记 → 请求 → 成功写入数据 / 失败写入错误文案 → 复位标记。
 * 之所以把看板与列表的 loading 分成两个字段，是为了在视图切换时不让一方的刷新把另一方的
 * 渲染态清空，避免出现整页闪烁。
 */
import { reactive } from 'vue'
import {
  createTask,
  deleteTask,
  fetchAssignees,
  fetchBoard,
  fetchSummary,
  fetchTaskPage,
  moveTask,
  updateTask,
} from '../api/tasks.js'

/** 列表视图每页条数，与后端默认值一致；变更时需同步后端 resolvePageSize 的默认值。 */
export const DEFAULT_PAGE_SIZE = 5

/** 看板列的可选目标状态，供卡片上的「移动到」下拉与表单使用。 */
export const STATUS_OPTIONS = [
  { status: 'TODO', label: '待办' },
  { status: 'DOING', label: '进行中' },
  { status: 'REVIEW', label: '待验收' },
  { status: 'DONE', label: '已完成' },
]

/** 优先级选项，供筛选下拉与表单使用。 */
export const PRIORITY_OPTIONS = [
  { value: 'LOW', label: '低' },
  { value: 'MEDIUM', label: '中' },
  { value: 'HIGH', label: '高' },
  { value: 'URGENT', label: '紧急' },
]

/** 全局唯一状态对象；组件通过导入本对象读取，不在本地复制数据。 */
export const state = reactive({
  /** 当前视图：board 看板 / list 列表。 */
  view: 'board',
  /** 看板与列表共用的筛选条件，切换视图时保留。 */
  filters: { keyword: '', assignee: '', priority: '' },
  /** 看板列数据，结构为 { status, label, total, tasks }。 */
  columns: [],
  /** 看板聚合时刻，用于提示数据新鲜度。 */
  generatedAt: '',
  /** 列表当前页数据。 */
  items: [],
  /** 列表当前页码，从 1 开始。 */
  page: 1,
  /** 列表每页条数。 */
  size: DEFAULT_PAGE_SIZE,
  /** 列表命中总条数。 */
  totalItems: 0,
  /** 列表总页数，供分页器渲染。 */
  totalPages: 1,
  /** 汇总指标，null 表示尚未加载。 */
  summary: null,
  /** 负责人候选，供筛选与表单下拉。 */
  assignees: [],
  /** 看板请求中标记。 */
  boardLoading: false,
  /** 列表请求中标记。 */
  listLoading: false,
  /** 表单提交中标记，用于禁用提交按钮。 */
  submitting: false,
  /** 列表级错误文案，展示在列表区域。 */
  error: '',
  /** 操作级错误文案（移动、删除、提交失败），展示在页面顶部。 */
  actionError: '',
})

/** 看板请求序号：只允许最新一次请求的结果写回状态，防止慢响应覆盖新结果。 */
let boardRequestSeq = 0

/** 列表请求序号：与看板各自独立，两个视图互不干扰。 */
let listRequestSeq = 0

/** 在途的看板请求控制器：新请求发起前先取消上一个，避免过期请求继续占用服务端查询。 */
let boardAbortController = null

/** 在途的列表请求控制器，语义同上。 */
let listAbortController = null

/**
 * 判断异常是否为主动取消。
 *
 * @param {unknown} cause 捕获到的异常
 * @returns {boolean} 调用方 abort 导致的异常返回 true
 */
function isAbortError(cause) {
  return Boolean(cause) && cause.name === 'AbortError'
}

/**
 * 把任意异常转成可展示文案。
 *
 * @param {unknown} cause 捕获到的异常
 * @returns {string} 展示文案
 */
function toErrorMessage(cause) {
  return cause instanceof Error ? cause.message : '未知错误'
}

/**
 * 从看板列中按 id 查找卡片。
 *
 * @param {number} id 任务主键
 * @returns {{column: object, index: number, card: object}|null} 命中位置信息；未命中返回 null
 */
function findCard(id) {
  for (const column of state.columns) {
    const index = column.tasks.findIndex((task) => task.id === id)
    if (index >= 0) {
      return { column, index, card: column.tasks[index] }
    }
  }
  return null
}

/**
 * 刷新看板聚合数据。
 *
 * @returns {Promise<void>}
 */
export async function loadBoard() {
  // 取号后只认最新一次：筛选或视图切换会连续触发请求，旧响应后到时必须丢弃。
  const seq = ++boardRequestSeq
  // 同时中止上一个在途请求：序号守卫只解决「写回」，中止才让过期请求真正停止传输与解析。
  if (boardAbortController) {
    boardAbortController.abort()
  }
  const controller = new AbortController()
  boardAbortController = controller

  state.boardLoading = true
  state.error = ''
  try {
    const payload = await fetchBoard(state.filters, controller.signal)
    if (seq !== boardRequestSeq) {
      return
    }
    state.columns = payload.columns || []
    state.generatedAt = payload.generatedAt || ''
  } catch (cause) {
    // 主动取消不是错误：既不该写错误态，也不该抢走最新请求的加载态。
    if (isAbortError(cause) || seq !== boardRequestSeq) {
      return
    }
    state.columns = []
    state.error = toErrorMessage(cause)
  } finally {
    // 只有最新一次请求才有权复位加载态：否则先返回的旧请求会提前解锁按钮。
    if (seq === boardRequestSeq) {
      state.boardLoading = false
    }
  }
}

/**
 * 刷新列表当前页数据。
 *
 * @returns {Promise<void>}
 */
export async function loadList() {
  // 与看板同理：翻页、筛选、切换视图都会并发触发本方法，只有最新一次请求能写回。
  const seq = ++listRequestSeq
  if (listAbortController) {
    listAbortController.abort()
  }
  const controller = new AbortController()
  listAbortController = controller

  state.listLoading = true
  state.error = ''
  try {
    const payload = await fetchTaskPage({
      ...state.filters,
      page: state.page,
      size: state.size,
    }, controller.signal)
    if (seq !== listRequestSeq) {
      return
    }
    state.items = payload.items || []
    state.totalItems = payload.totalItems || 0
    // 总页数直接采用服务端计算结果：前端再算一遍会与服务端的取整口径分叉，
    // 总数不能被每页条数整除时还会少算一页、让末页无法翻到。
    state.totalPages = Math.max(1, payload.totalPages || 1)
  } catch (cause) {
    if (isAbortError(cause) || seq !== listRequestSeq) {
      return
    }
    state.items = []
    state.totalItems = 0
    state.totalPages = 1
    state.error = toErrorMessage(cause)
  } finally {
    if (seq === listRequestSeq) {
      state.listLoading = false
    }
  }
}

/**
 * 刷新汇总指标。
 *
 * @returns {Promise<void>}
 */
export async function loadSummary() {
  try {
    state.summary = await fetchSummary()
  } catch (cause) {
    // 汇总失败不阻塞主内容：指标条留空即可，主区域仍可正常操作。
    state.summary = null
    state.actionError = `汇总加载失败：${toErrorMessage(cause)}`
  }
}

/**
 * 刷新负责人候选。
 *
 * @returns {Promise<void>}
 */
export async function loadAssignees() {
  try {
    state.assignees = (await fetchAssignees()) || []
  } catch {
    state.assignees = []
  }
}

/**
 * 首次进入页面时并发拉取四份数据。
 *
 * @returns {Promise<void>}
 */
export async function refreshAll() {
  await Promise.all([loadBoard(), loadList(), loadSummary(), loadAssignees()])
}

/**
 * 切换视图；切到列表时立即拉取一次当前页。
 *
 * @param {'board'|'list'} view 目标视图
 * @returns {void}
 */
export function setView(view) {
  if (state.view === view) {
    return
  }
  state.view = view
  state.actionError = ''
  if (view === 'list') {
    loadList()
  }
}

/**
 * 更新筛选条件并刷新当前视图的数据。
 *
 * @param {object} patch 需要变更的筛选字段，可含 keyword / assignee / priority
 * @returns {void}
 */
export function changeFilter(patch) {
  Object.assign(state.filters, patch)
  state.actionError = ''
  // 筛选条件变化后回到第 1 页：页码是筛选结果的派生状态，留着旧页码会去请求一个
  // 新结果集里可能不存在的页，用户看到的是空列表而不是「筛选后没有结果」。
  state.page = 1
  if (state.view === 'list') {
    loadList()
  } else {
    loadBoard()
  }
}

/**
 * 清空全部筛选条件。
 *
 * @returns {void}
 */
export function resetFilters() {
  state.filters.keyword = ''
  state.filters.assignee = ''
  state.filters.priority = ''
  state.actionError = ''
  state.page = 1
  if (state.view === 'list') {
    loadList()
  } else {
    loadBoard()
  }
}

/**
 * 翻页。页码会被夹到 [1, totalPages] 区间内，避免越界请求。
 *
 * @param {number} next 目标页码
 * @returns {void}
 */
export function changePage(next) {
  const target = Math.min(Math.max(1, next), state.totalPages)
  if (target === state.page) {
    return
  }
  state.page = target
  loadList()
}

/**
 * 把卡片在本地列数据中挪位，用于提交前的即时反馈。
 *
 * @param {number} id 任务主键
 * @param {string} targetStatus 目标状态编码
 * @param {number|undefined} position 目标列内下标，未指定时追加到列尾
 * @returns {void}
 */
function applyLocalMove(id, targetStatus, position) {
  const found = findCard(id)
  if (!found) {
    return
  }
  const target = state.columns.find((column) => column.status === targetStatus)
  if (!target) {
    return
  }
  const [card] = found.column.tasks.splice(found.index, 1)
  card.status = targetStatus
  const at = position === undefined || position === null ? target.tasks.length : position
  target.tasks.splice(at, 0, card)
  found.column.total = Math.max(0, found.column.total - 1)
  target.total += 1
}

/**
 * 用服务端返回的最新卡片覆盖本地数据。
 *
 * @param {object} updated 服务端返回的任务出参
 * @returns {void}
 */
function replaceCard(updated) {
  const found = findCard(updated.id)
  if (!found) {
    return
  }
  Object.assign(found.card, updated)
}

/**
 * 移动任务：先本地挪位给即时反馈，再提交到后端，成功后用服务端结果校正。
 *
 * @param {number} id 任务主键
 * @param {string} targetStatus 目标状态编码
 * @param {number|undefined} position 目标列内下标
 * @returns {Promise<void>}
 */
export async function relocateTask(id, targetStatus, position) {
  const found = findCard(id)
  if (!found) {
    return
  }
  const version = found.card.version
  state.actionError = ''
  applyLocalMove(id, targetStatus, position)
  try {
    const updated = await moveTask(id, { targetStatus, position, version })
    replaceCard(updated)
    await loadSummary()
  } catch (cause) {
    state.actionError = `移动失败：${toErrorMessage(cause)}`
    // 乐观更新只是即时反馈，提交失败必须回退到服务端状态：
    // 否则卡片留在目标列、页面又提示失败，用户会基于错误的位置继续操作。
    await loadBoard()
  }
}

/**
 * 新建或编辑任务，成功后刷新看板、列表、汇总与负责人候选。
 *
 * @param {object} payload 表单数据
 * @param {number} [id] 任务主键；传入表示编辑，不传表示新建
 * @returns {Promise<boolean>} 是否提交成功
 */
export async function submitTask(payload, id) {
  state.submitting = true
  state.actionError = ''
  try {
    if (id) {
      await updateTask(id, payload)
    } else {
      await createTask(payload)
    }
    await Promise.all([loadBoard(), loadList(), loadSummary(), loadAssignees()])
    return true
  } catch (cause) {
    state.actionError = `保存失败：${toErrorMessage(cause)}`
    return false
  } finally {
    state.submitting = false
  }
}

/**
 * 删除任务，成功后刷新看板与汇总。
 *
 * @param {number} id 任务主键
 * @returns {Promise<boolean>} 是否删除成功
 */
export async function removeTask(id) {
  state.actionError = ''
  try {
    await deleteTask(id)
    await Promise.all([loadBoard(), loadList(), loadSummary()])
    return true
  } catch (cause) {
    state.actionError = `删除失败：${toErrorMessage(cause)}`
    return false
  }
}
