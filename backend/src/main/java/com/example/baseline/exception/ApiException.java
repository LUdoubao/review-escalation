package com.example.baseline.exception;

import org.springframework.http.HttpStatus;

/**
 * 可预期的业务异常，携带机器可读的业务码与对应的 HTTP 状态。
 *
 * <p>领域：任务看板错误契约；关系：由 service 抛出，由 {@code GlobalExceptionHandler}
 * 转为 {@code ErrorResponse}，前端据 {@link #getCode()} 做分支处理。
 * 约定：仅用于「调用方可修正」的错误（参数非法、资源不存在、版本冲突）；
 * 程序缺陷不应抛此类异常，以免被当作正常业务分支吞掉。</p>
 *
 * <p>继承 RuntimeException 而不声明受检异常：service 方法本身不关心错误处理流程，
 * 强制捕获只会让调用链充满无意义的 throws 声明。</p>
 */
public class ApiException extends RuntimeException {

    /** 业务错误码，全局唯一，前端按此判断展示与重试策略。 */
    private final String code;

    /** 该错误对应的 HTTP 状态，由异常处理器直接采用。 */
    private final HttpStatus status;

    /**
     * @param code    业务错误码，非空
     * @param status  对应的 HTTP 状态，非空
     * @param message 面向使用者的提示文案
     */
    public ApiException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
