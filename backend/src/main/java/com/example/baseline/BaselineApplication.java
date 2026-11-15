package com.example.baseline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 基线后端启动类，负责装配内嵌 Web 容器与 com.example.baseline 包下的全部组件。
 *
 * <p>领域：前后端联通性验证基线，不承载任何业务域逻辑。
 * 生命周期：随进程启动创建单例 Spring 容器，进程退出时由 JVM 关闭钩子释放。
 * 关系：组件扫描根包为 com.example.baseline，controller / service / config / dto 均位于其下。
 * 场景：本地以 8080 端口运行，供 Vite dev server 的 /api 代理调用。</p>
 */
@SpringBootApplication
public class BaselineApplication {

    /**
     * 应用入口，启动 Spring 容器并保持主线程直到容器关闭。
     *
     * @param args 原样透传给 Spring Boot 的命令行参数，基线版本不解析任何自定义参数
     */
    public static void main(String[] args) {
        SpringApplication.run(BaselineApplication.class, args);
    }
}
