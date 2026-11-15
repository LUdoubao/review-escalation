<script setup>
/**
 * 任务表单弹窗：新建与编辑共用一套表单，按 mode 决定标题与提交内容。
 * 边界：校验在前端做一次提示性拦截，最终仍以后端校验为准；本组件不发起请求，
 * 只把整理好的 payload 通过 submit 事件上抛，由页面交给 store 处理。
 */
import { reactive, ref, watch } from 'vue'
import { PRIORITY_OPTIONS } from '../stores/boardStore.js'
import { todayString } from '../utils/date.js'

const props = defineProps({
  /** 是否显示弹窗。 */
  open: { type: Boolean, default: false },
  /** 表单模式：create 新建 / edit 编辑。 */
  mode: { type: String, default: 'create' },
  /** 编辑时的原始任务；新建时为 null。 */
  task: { type: Object, default: null },
  /** 负责人候选。 */
  assignees: { type: Array, default: () => [] },
  /** 提交中标记，用于禁用按钮防止重复提交。 */
  submitting: { type: Boolean, default: false },
})

const emit = defineEmits(['submit', 'close'])

/** 表单字段；与后端 TaskCreateRequest / TaskUpdateRequest 的字段名保持一致。 */
const form = reactive({
  title: '',
  description: '',
  assignee: '',
  priority: 'MEDIUM',
  tag: '',
  dueDate: '',
})

/** 前端校验错误文案，空串表示无错误。 */
const error = ref('')

// 每次打开时按模式重置表单：编辑态回填原值，新建态清空，避免上一次的输入残留。
watch(
  () => props.open,
  (open) => {
    if (!open) {
      return
    }
    error.value = ''
    const source = props.task || {}
    form.title = source.title || ''
    form.description = source.description || ''
    form.assignee = source.assignee || ''
    form.priority = source.priority || 'MEDIUM'
    form.tag = source.tag || ''
    form.dueDate = source.dueDate || ''
  },
)

/**
 * 前端校验，返回第一条错误文案。
 *
 * @returns {string} 错误文案；空串表示通过
 */
function validate() {
  const title = form.title.trim()
  if (!title) {
    return '任务标题不能为空'
  }
  if (title.length > 80) {
    return '任务标题不能超过 80 个字符'
  }
  if (form.description.trim().length > 500) {
    return '描述不能超过 500 个字符'
  }
  if (form.assignee.trim().length > 40) {
    return '负责人不能超过 40 个字符'
  }
  if (form.tag.trim().length > 24) {
    return '标签不能超过 24 个字符'
  }
  // 日期按字符串比较：YYYY-MM-DD 的字典序与时间序一致，避免 new Date() 的时区换算歧义。
  // 编辑时若日期未改动则跳过校验：历史截止日早已过期，不应该因此连负责人和描述都改不了。
  const originalDueDate = props.mode === 'edit' && props.task ? (props.task.dueDate || '') : ''
  if (form.dueDate && form.dueDate !== originalDueDate && form.dueDate < todayString()) {
    return '截止日期不能早于今天'
  }
  return ''
}

/**
 * 提交表单：校验通过后组装 payload 上抛。
 *
 * @returns {void}
 */
function onSubmit() {
  const message = validate()
  if (message) {
    error.value = message
    return
  }
  error.value = ''
  // 文本字段直接提交 trim 后的值（可能为空串），由后端把空串归一为 null 完成清空；
  // 日期无法用空串表达，清空意图走 clearFields 显式声明，避免与「不修改」混为一谈。
  const payload = {
    title: form.title.trim(),
    description: form.description.trim(),
    assignee: form.assignee.trim(),
    priority: form.priority,
    tag: form.tag.trim(),
    dueDate: form.dueDate || null,
  }
  if (props.mode === 'edit' && props.task) {
    // 编辑时回传版本号，供后端做乐观锁校验。
    payload.version = props.task.version
    payload.clearFields = form.dueDate ? [] : ['dueDate']
  }
  emit('submit', payload)
}
</script>

<template>
  <div v-if="open" class="mask" role="dialog" aria-modal="true" :aria-label="mode === 'edit' ? '编辑任务' : '新建任务'">
    <div class="dialog">
      <header class="dialog__head">
        <h3 class="dialog__title">{{ mode === 'edit' ? '编辑任务' : '新建任务' }}</h3>
        <button class="dialog__close" type="button" aria-label="关闭" @click="emit('close')">×</button>
      </header>

      <form class="dialog__body" @submit.prevent="onSubmit">
        <label class="field">
          <span class="field__label">标题 <em>*</em></span>
          <input v-model="form.title" class="field__input" type="text" maxlength="80" placeholder="一句话描述这件事" />
        </label>

        <label class="field">
          <span class="field__label">描述</span>
          <textarea v-model="form.description" class="field__input field__input--area" maxlength="500" rows="3" />
        </label>

        <div class="field-row">
          <label class="field">
            <span class="field__label">负责人</span>
            <input v-model="form.assignee" class="field__input" type="text" maxlength="40" list="assignee-options" placeholder="未指派" />
            <datalist id="assignee-options">
              <option v-for="name in assignees" :key="name" :value="name" />
            </datalist>
          </label>

          <label class="field">
            <span class="field__label">优先级</span>
            <select v-model="form.priority" class="field__input">
              <option v-for="item in PRIORITY_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
        </div>

        <div class="field-row">
          <label class="field">
            <span class="field__label">标签</span>
            <input v-model="form.tag" class="field__input" type="text" maxlength="24" placeholder="如 前端 / 数据" />
          </label>

          <label class="field">
            <span class="field__label">截止日期</span>
            <input v-model="form.dueDate" class="field__input" type="date" />
          </label>
        </div>

        <p v-if="error" class="dialog__error" role="alert">{{ error }}</p>

        <footer class="dialog__foot">
          <button class="btn" type="button" :disabled="submitting" @click="emit('close')">取消</button>
          <button class="btn btn--primary" type="submit" :disabled="submitting">
            {{ submitting ? '提交中…' : '提交' }}
          </button>
        </footer>
      </form>
    </div>
  </div>
</template>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(16, 24, 40, 0.36);
  z-index: 20;
}

.dialog {
  width: 100%;
  max-width: 520px;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 12px 32px rgba(16, 24, 40, 0.16);
}

.dialog__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 8px;
}

.dialog__title {
  margin: 0;
  font-size: 16px;
  color: #1f2937;
}

.dialog__close {
  width: 26px;
  height: 26px;
  border: none;
  border-radius: 6px;
  background: none;
  color: #8a93a3;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}

.dialog__close:hover {
  background: #f2f5fa;
}

.dialog__body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 8px 20px 18px;
}

.field {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  gap: 4px;
}

.field-row {
  display: flex;
  gap: 12px;
}

.field__label {
  color: #6b7484;
  font-size: 12px;
}

.field__label em {
  color: #b42318;
  font-style: normal;
}

.field__input {
  height: 32px;
  padding: 0 10px;
  border: 1px solid #d7dee9;
  border-radius: 6px;
  background: #ffffff;
  color: #1f2937;
  font-size: 13px;
  font-family: inherit;
}

.field__input--area {
  height: auto;
  padding: 8px 10px;
  line-height: 1.6;
  resize: vertical;
}

.dialog__error {
  margin: 0;
  color: #b42318;
  font-size: 12px;
}

.dialog__foot {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  padding-top: 4px;
}

.btn {
  height: 32px;
  padding: 0 16px;
  border: 1px solid #d7dee9;
  border-radius: 6px;
  background: #ffffff;
  color: #475467;
  font-size: 13px;
  cursor: pointer;
}

.btn--primary {
  border-color: #2f6feb;
  background: #2f6feb;
  color: #ffffff;
}

.btn:disabled {
  border-color: #d7dee9;
  background: #eef2f7;
  color: #a3aab7;
  cursor: not-allowed;
}
</style>
