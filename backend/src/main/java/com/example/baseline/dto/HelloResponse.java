package com.example.baseline.dto;

/**
 * GET /api/hello 的响应体，是前后端之间唯一的跨栈数据契约。
 *
 * <p>领域：连通性验证；关系：由 HelloService 构造、由 HelloController 直接序列化返回，
 * 前端由 frontend/src/api/hello.js 消费并由 StatusCard.vue 渲染。
 * 不可变性：record 天然只读，单请求内构造一次后不再修改，无线程共享状态。</p>
 *
 * @param message   后端问候文本，非空固定文案，前端用于确认调用链已打通
 * @param service   返回该响应的服务逻辑名，非空固定值 review-escalation-backend
 * @param timestamp 服务端生成时刻，UTC 的 ISO-8601 字符串（如 2026-01-01T00:00:00Z），
 *                  由 Instant 直接输出，避免依赖本地时区；前端仅原样展示，不做解析或格式化
 */
public record HelloResponse(String message, String service, String timestamp) {
}
