package com.example.baseline.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/hello 的冒烟测试，覆盖真实 Spring MVC 链路与 Jackson 序列化结果。
 *
 * <p>领域：基线自检；关系：启动完整应用上下文，不 mock service，因此能同时验证 service 与 DTO。
 * 生命周期：每个测试类共享 Spring 上下文；无外部依赖，不访问网络或数据库。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class HelloControllerTest {

    /** 由测试切片注入的 MVC 测试客户端，用于发起不占用真实端口的 HTTP 请求。 */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 验证端点返回 200 且三个契约字段均存在、固定字段值正确。
     *
     * @throws Exception MockMvc 执行请求或解析响应失败时抛出，此时测试直接失败
     */
    @Test
    void helloReturnsContractFields() throws Exception {
        // timestamp 只校验非空而不校验具体值：其值随请求时刻变化，固定断言会造成偶发失败。
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello from Spring Boot"))
                .andExpect(jsonPath("$.service").value("review-escalation-backend"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
