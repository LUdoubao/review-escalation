package com.example.baseline.controller;

import com.example.baseline.dto.HelloResponse;
import com.example.baseline.service.HelloService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 连通性接口控制器，仅暴露 /api/hello 一个只读端点。
 *
 * <p>领域：前后端联调；关系：接收 HTTP GET 后委托 HelloService，返回值由 Jackson 直接序列化，
 * 不在此层做字段加工、异常包装或状态判断。
 * 权限/幂等：匿名可读的只读 GET，无副作用；重复调用仅 timestamp 不同，不产生状态变更。</p>
 */
@RestController
@RequestMapping("/api")
public class HelloController {

    /** 问候信息构造服务；构造器注入保证依赖不可变且便于测试替换。 */
    private final HelloService helloService;

    /**
     * @param helloService 问候信息构造服务，由容器注入，不可为 null
     */
    public HelloController(HelloService helloService) {
        this.helloService = helloService;
    }

    /**
     * 返回后端连通性信息。
     *
     * @return HTTP 200 与 HelloResponse JSON；message / service 固定，timestamp 为请求处理时刻
     * @throws 无业务异常；容器级故障由 Spring Boot 默认错误处理返回 5xx
     */
    @GetMapping("/hello")
    public HelloResponse hello() {
        // 控制器保持最薄：任何业务分支、参数校验或错误码都应放在 service 层或独立配置中，
        // 这样后续扩展功能与植入缺陷时，契约变更点始终集中且可定位。
        return helloService.buildGreeting();
    }
}
