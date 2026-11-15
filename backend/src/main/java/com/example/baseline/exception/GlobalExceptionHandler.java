package com.example.baseline.exception;

import com.example.baseline.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.ZonedDateTime;

/**
 * 全局异常出口：把各类异常统一收敛为 {@code ErrorResponse}，保证前端只需处理一种错误形状。
 *
 * <p>领域：任务看板错误契约；关系：拦截 controller 与 service 抛出的异常，
 * 不参与业务判断，也不改变正常路径的返回值。
 * 分级：调用方可修正的错误返回 4xx 且不打错误日志；未预期异常返回 500 并记录堆栈，
 * 但响应体不包含堆栈信息，避免泄漏内部结构。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 响应体时间戳格式：服务器本地时区的 ISO-8601，与 TaskResponse.updatedAt 口径一致。 */
    private static final java.time.format.DateTimeFormatter TS = java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * 处理业务异常，直接采用异常携带的状态码与错误码。
     *
     * @param exception 业务异常
     * @return 带业务码的错误响应
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException exception) {
        return build(exception.getStatus(), exception.getCode(), exception.getMessage());
    }

    /**
     * 处理查询参数类型不匹配，例如 page=abc 或 size 传入非数字。
     *
     * @param exception 类型转换异常
     * @return 400 与 INVALID_PARAMETER
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        // 只回传参数名，不回传原始值：原始值可能包含超长或异常字符，直接回显会污染日志与页面。
        String name = exception.getName();
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "参数 " + name + " 格式不正确");
    }

    /**
     * 处理缺少必填查询参数。
     *
     * @param exception 缺参异常
     * @return 400 与 INVALID_PARAMETER
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException exception) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "缺少必填参数 " + exception.getParameterName());
    }

    /**
     * 处理请求体无法解析（JSON 语法错误、日期格式不符、字段类型不匹配）。
     *
     * @param exception 报文解析异常
     * @return 400 与 INVALID_REQUEST_BODY
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException exception) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY", "请求体格式不正确，请检查 JSON 与日期格式");
    }

    /**
     * 处理方法级参数校验失败（预留：当前校验在 service 内完成，此分支用于框架级校验注解）。
     *
     * @param exception 校验异常
     * @return 400 与 INVALID_PARAMETER
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidArgument(MethodArgumentNotValidException exception) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "请求参数校验未通过");
    }

    /**
     * 处理静态资源与未知路径，保持 404 语义不被兜底分支改写成 500。
     *
     * @param exception 资源不存在异常
     * @return 404 与 NOT_FOUND
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "接口路径不存在");
    }

    /**
     * 兜底处理未预期异常：记录完整堆栈，但只向前端返回通用提示。
     *
     * @param exception 未预期异常
     * @return 500 与 INTERNAL_ERROR
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        // 堆栈只进日志：前端拿到错误码即可，暴露内部类名与调用链会扩大攻击面。
        log.error("未预期的服务端异常", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "服务端处理失败，请稍后重试");
    }

    /**
     * 构造统一错误响应。
     *
     * @param status  HTTP 状态
     * @param code    业务错误码
     * @param message 提示文案
     * @return 带状态码的错误响应实体
     */
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message) {
        ErrorResponse body = new ErrorResponse(code, message, ZonedDateTime.now().format(TS));
        return ResponseEntity.status(status).body(body);
    }
}
