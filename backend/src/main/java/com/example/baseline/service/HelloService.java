package com.example.baseline.service;

import com.example.baseline.dto.HelloResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 连通性问候信息的构造服务，是 controller 与响应契约之间唯一的业务逻辑承载层。
 *
 * <p>领域：联通性验证；关系：仅被 HelloController 调用，不访问数据库、缓存或远程服务。
 * 事务/线程：无状态单例，不开启事务；Instant.now() 线程安全，可安全处理并发请求。
 * 生命周期：由 Spring 容器管理为单例 Bean。</p>
 */
@Service
public class HelloService {

    /** 服务逻辑名，写入响应以便前端确认数据确实来自本后端；与 application.yml 的 spring.application.name 保持一致。 */
    private static final String SERVICE_NAME = "review-escalation-backend";

    /** 固定的问候文案，作为前后端联通成功的可见标记；不含请求参数，避免基线引入可变业务分支。 */
    private static final String GREETING = "Hello from Spring Boot";

    /**
     * 构造一次连通性响应。
     *
     * @return 新建的 HelloResponse，三个字段均非空；每次调用生成新的 timestamp
     * @throws 无受检异常；Instant.now() 依赖系统时钟，时钟异常属 JVM 级故障，不在此捕获
     */
    public HelloResponse buildGreeting() {
        // 使用 UTC Instant 的字符串形式而非 LocalDateTime：跨栈契约约定 UTC ISO-8601，
        // 若改用本地时间会让前端展示随部署时区漂移，且后续无法机械比对 C7 时区口径。
        return new HelloResponse(GREETING, SERVICE_NAME, Instant.now().toString());
    }
}
