<script setup>
/**
 * 页面容器：编排「看板 / 列表 / 联通性自检」三个区块，持有弹窗与自检区的局部状态。
 * 边界：业务状态全部来自 stores/boardStore.js，请求全部经由 api/；本文件只做视图切换与事件转发。
 * 保留项：页面底部的 /api/hello 自检区沿用基线实现，用于确认改造后联通链路仍然可用。
 */
import { computed, onMounted, ref } from 'vue'
import { fetchHello } from './api/hello.js'
import BoardColumn from './components/BoardColumn.vue'
import BoardStats from './components/BoardStats.vue'
import BoardToolbar from './components/BoardToolbar.vue'
import PaginationBar from './components/PaginationBar.vue'
import StateHint from './components/StateHint.vue'
import StatusCard from './components/StatusCard.vue'
import TaskFormDialog from './components/TaskFormDialog.vue'
import TaskTable from './components/TaskTable.vue'
import {
  changeFilter,
  changePage,
  loadBoard,
  loadList,
  refreshAll,
  relocateTask,
  removeTask,
  resetFilters,
  setView,
  state,
  submitTask,
} from './stores/boardStore.js'

/** 弹窗是否可见。 */
const dialogOpen = ref(false)
/** 弹窗模式：create / edit。 */
const dialogMode = ref('create')
/** 编辑中的任务快照；新建时为 null。 */
const editingTask = ref(null)

/** 当前视图对应的加载态：看板与列表各自独立，避免切换视图时互相干扰。 */
const currentLoading = computed(() => (state.view === 'board' ? state.boardLoading : state.listLoading))

/** 当前视图是否为空数据。 */
const isEmpty = computed(() => (state.view === 'board' ? state.columns.length === 0 : state.items.length === 0))

/** 基线自检区：请求进行中标记。 */
const helloLoading = ref(false)
/** 基线自检区：错误文案。 */
const helloError = ref('')
/** 基线自检区：最近一次成功响应。 */
const helloResult = ref(null)

/**
 * 发起一次联通性自检请求。
 *
 * @returns {Promise<void>}
 */
async function loadHello() {
  helloLoading.value = true
  helloError.value = ''
  try {
    helloResult.value = await fetchHello()
  } catch (cause) {
    helloResult.value = null
    helloError.value = cause instanceof Error ? cause.message : '未知错误'
  } finally {
    helloLoading.value = false
  }
}

/**
 * 打开新建弹窗。
 *
 * @returns {void}
 */
function openCreate() {
  dialogMode.value = 'create'
  editingTask.value = null
  dialogOpen.value = true
}

/**
 * 打开编辑弹窗。
 *
 * @param {object} task 待编辑任务
 * @returns {void}
 */
function openEdit(task) {
  dialogMode.value = 'edit'
  editingTask.value = task
  dialogOpen.value = true
}

/**
 * 关闭弹窗并清理编辑快照。
 *
 * @returns {void}
 */
function closeDialog() {
  dialogOpen.value = false
  editingTask.value = null
}

/**
 * 提交表单：成功后关闭弹窗，失败时保持弹窗以便修改。
 *
 * @param {object} payload 表单数据
 * @returns {Promise<void>}
 */
async function onSubmit(payload) {
  const id = editingTask.value ? editingTask.value.id : undefined
  const done = await submitTask(payload, id)
  if (done) {
    closeDialog()
  }
}

/**
 * 移动任务到目标列（追加到列尾）。
 *
 * @param {number} id 任务主键
 * @param {string} targetStatus 目标状态编码
 * @returns {void}
 */
function onMove(id, targetStatus) {
  relocateTask(id, targetStatus)
}

/**
 * 删除任务，二次确认后提交。
 *
 * @param {object} task 待删除任务
 * @returns {void}
 */
function onRemove(task) {
  if (window.confirm(`确认删除任务「${task.title}」？`)) {
    removeTask(task.id)
  }
}

/**
 * 在当前视图上重试加载。
 *
 * @returns {void}
 */
function retry() {
  if (state.view === 'board') {
    loadBoard()
  } else {
    loadList()
  }
}

// 首屏并发拉取看板、列表、汇总与负责人候选，同时触发一次基线自检。
onMounted(() => {
  refreshAll()
  loadHello()
})
</script>

<template>
  <main class="page">
    <header class="page__head">
      <div>
        <h1>任务看板</h1>
        <p class="subtitle">Vue 3 + Vite · Spring Boot 3 · MyBatis-Plus · MySQL</p>
      </div>
      <p v-if="state.generatedAt" class="page__fresh">数据更新于 {{ state.generatedAt.replace('T', ' ').slice(0, 19) }}</p>
    </header>

    <BoardStats :summary="state.summary" />

    <BoardToolbar
      :filters="state.filters"
      :assignees="state.assignees"
      :view="state.view"
      @change-filter="changeFilter"
      @reset="resetFilters"
      @create="openCreate"
      @change-view="setView"
    />

    <p v-if="state.actionError" class="page__alert" role="alert">{{ state.actionError }}</p>

    <StateHint
      :loading="currentLoading"
      :error="state.error"
      :empty="isEmpty"
      :empty-text="state.view === 'board' ? '当前筛选条件下没有任务，试试清空筛选' : '当前筛选条件下没有任务'"
      :loading-text="state.view === 'board' ? '正在加载看板…' : '正在加载列表…'"
      @retry="retry"
    />

    <section v-if="state.view === 'board' && !currentLoading && !state.error" class="board">
      <BoardColumn
        v-for="column in state.columns"
        :key="column.status"
        :column="column"
        @card-move="onMove"
        @card-edit="openEdit"
        @card-remove="onRemove"
      />
    </section>

    <section v-if="state.view === 'list' && !currentLoading && !state.error">
      <TaskTable :items="state.items" @edit="openEdit" @remove="onRemove" @move="onMove" />
      <PaginationBar
        :page="state.page"
        :total-pages="state.totalPages"
        :total-items="state.totalItems"
        :size="state.size"
        @change="changePage"
      />
    </section>

    <TaskFormDialog
      :open="dialogOpen"
      :mode="dialogMode"
      :task="editingTask"
      :assignees="state.assignees"
      :submitting="state.submitting"
      @submit="onSubmit"
      @close="closeDialog"
    />

    <section class="selfcheck">
      <header class="selfcheck__head">
        <h2>联通性自检</h2>
        <button class="refresh" type="button" :disabled="helloLoading" @click="loadHello">
          {{ helloLoading ? '请求中…' : '重新请求' }}
        </button>
      </header>
      <p class="selfcheck__desc">调用 GET /api/hello，确认改造后前后端链路与跨域配置仍然可用。</p>
      <StatusCard :loading="helloLoading" :error="helloError" :result="helloResult" />
    </section>
  </main>
</template>

<style scoped>
.page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 40px 24px 56px;
}

.page__head {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 20px;
}

h1 {
  margin: 0 0 6px;
  font-size: 24px;
  color: #1f2937;
}

.subtitle {
  margin: 0;
  color: #6b7484;
  font-size: 13px;
}

.page__fresh {
  margin: 0;
  color: #8a93a3;
  font-size: 12px;
}

.page__alert {
  margin: 0 0 12px;
  padding: 8px 12px;
  border: 1px solid #f3c6c0;
  border-radius: 6px;
  background: #fdf3f2;
  color: #b42318;
  font-size: 13px;
}

.board {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.selfcheck {
  margin-top: 40px;
  padding-top: 20px;
  border-top: 1px solid #e3e8ef;
}

.selfcheck__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.selfcheck__head h2 {
  margin: 0;
  font-size: 15px;
  color: #1f2937;
}

.selfcheck__desc {
  margin: 6px 0 0;
  color: #8a93a3;
  font-size: 12px;
}

.refresh {
  padding: 6px 16px;
  border: 1px solid #2f6feb;
  border-radius: 6px;
  background: #2f6feb;
  color: #ffffff;
  font-size: 13px;
  cursor: pointer;
}

.refresh:hover:not(:disabled) {
  background: #245bc9;
}

.refresh:disabled {
  border-color: #b9c6dc;
  background: #b9c6dc;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .board {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
