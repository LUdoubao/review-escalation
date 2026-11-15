package com.example.baseline.dto;

/**
 * 统一错误响应体，所有可预期错误与未预期异常都返回这一形状。
 *
 * <p>领域：跨栈错误契约；关系：由 {@code GlobalExceptionHandler} 构造，
 * 被 frontend/src/api/http.js 解析后抛出带业务码的 Error。</p>
 *
 * @param code      业务错误码，机器可读，如 TASK_NOT_FOUND
 * @param message   面向使用者的中文提示，可直接展示
 * @param timestamp 错误产生时刻，ISO-8601 字符串（服务器本地时区），便于与日志对齐
 */
public record ErrorResponse(String code, String message, String timestamp) {
}
