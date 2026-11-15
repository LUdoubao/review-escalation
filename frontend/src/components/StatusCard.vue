<script setup>
/**
 * 纯展示组件：只消费父组件传入的 loading / error / result 三态，不发起请求、不持有业务状态。
 * 与 App.vue 的边界：状态机归页面，渲染归本组件；后续扩展展示字段只改此文件。
 */
defineProps({
  /** 是否正在请求；为 true 时禁用重复提交入口由父组件处理，本组件只切换展示。 */
  loading: { type: Boolean, default: false },
  /** 错误文案；空字符串表示无错误。 */
  error: { type: String, default: '' },
  /** 后端响应对象；null 表示尚未取得数据。契约字段见 src/api/hello.js。 */
  result: { type: Object, default: null },
})
</script>

<template>
  <!-- aria-live 让加载态与错误态变化可被读屏软件播报；优先级按 loading > error > result > empty 固定。 -->
  <section class="card" aria-live="polite">
    <p v-if="loading" class="state state--loading">正在请求后端…</p>

    <p v-else-if="error" class="state state--error" role="alert">
      请求失败：{{ error }}
    </p>

    <!-- 使用 dl 逐字段展示，字段名与后端 DTO 同名，便于联调时逐项核对契约。 -->
    <dl v-else-if="result" class="result">
      <div class="result__row">
        <dt>message</dt>
        <dd>{{ result.message }}</dd>
      </div>
      <div class="result__row">
        <dt>service</dt>
        <dd>{{ result.service }}</dd>
      </div>
      <div class="result__row">
        <dt>timestamp</dt>
        <dd>{{ result.timestamp }}</dd>
      </div>
    </dl>

    <p v-else class="state state--empty">尚未取得后端数据</p>
  </section>
</template>

<style scoped>
.card {
  margin-top: 20px;
  padding: 20px 24px;
  border: 1px solid #e3e8ef;
  border-radius: 10px;
  background: #ffffff;
  min-height: 132px;
}

.state {
  margin: 0;
  line-height: 1.7;
  color: #5b6577;
}

.state--loading { color: #8a6d1f; }
.state--error { color: #b42318; }
.state--empty { color: #8a93a3; }

.result {
  margin: 0;
}

.result__row {
  display: flex;
  gap: 16px;
  padding: 7px 0;
  border-bottom: 1px dashed #edf0f5;
}

.result__row:last-child {
  border-bottom: none;
}

.result dt {
  flex: 0 0 96px;
  color: #6b7484;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 13px;
}

.result dd {
  margin: 0;
  color: #1f2937;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 13px;
  word-break: break-all;
}
</style>
