# 项目级协作约定 — review-escalation

> 本文件声明**本项目的事实与约定**，供计划、实施与审查读取。全局协作设定（`~/.dsh/AGENTS.md`）的 P0 自定义区约束、P0.5 注释门禁、0B 汇报分级仍然生效；冲突时取更严格者。本文件不改写全局规则，也不授权自动提交、自动编译或自动修复。

## 1. 项目定位

用于验证审查 skill 有效性的最小全栈对照基线。业务逻辑刻意保持简单：唯一功能是前端调用 `GET /api/hello` 并展示返回结果。代码结构、命名与模块边界需长期稳定，以便后续轮次在其上扩展功能、植入缺陷并独立审查。

## 2. 模块边界（不得随意打破）

| 层 | 目录 | 职责 | 禁止承载 |
|---|---|---|---|
| 后端入口 | `backend/src/main/java/com/example/baseline/controller/` | HTTP 路由、参数绑定、委托调用 | 业务分支、字段加工、异常包装 |
| 后端逻辑 | `.../service/` | 唯一的业务逻辑承载层 | 直接访问 HTTP 对象 |
| 后端契约 | `.../dto/` | 响应体定义 | 行为与状态 |
| 后端配置 | `.../config/`、`resources/application.yml` | 跨域、端口、应用名 | 业务逻辑 |
| 前端请求层 | `frontend/src/api/` | 唯一 HTTP 出口、错误判定 | DOM 渲染、页面状态 |
| 前端页面 | `frontend/src/App.vue` | 请求三态（loading / error / result） | 直接调用 `fetch` |
| 前端展示 | `frontend/src/components/` | 纯 props 渲染 | 发起请求、持有业务状态 |

包根固定为 `com.example.baseline`；类名按 `XxxController / XxxService / XxxResponse / XxxConfig` 命名；前端文件名按 `PascalCase.vue`（组件）与 `lowerCamel.js`（模块）区分。

## 3. 事实与同步点

- 后端端口 `8080`（`application.yml`）、前端 dev 端口 `5173`（`vite.config.js`）、跨域放行源 `CorsConfig.DEV_ORIGIN`：三处必须一致。部分集成终端会注入 `SERVER_PORT` / `SERVER__PORT`，因 Spring Boot 松散绑定优先级高于 `application.yml` 而导致端口漂移，启动命令已按 README 显式传入 `--server.port=8080` 规避；不要把该参数写成其他端口。
- 唯一端点 `GET /api/hello`，前端请求前缀 `/api` 由 Vite 代理转发，**不做路径重写**。
- 响应契约三字段：`message`、`service`、`timestamp`（UTC ISO-8601 字符串）。任何字段增删改必须同时修改 `HelloResponse.java`、`HelloService.java`、`HelloControllerTest.java`、`frontend/src/api/hello.js` 的字段说明、`StatusCard.vue` 的渲染行与 README 契约表。
- `spring.application.name` 与 `HelloService.SERVICE_NAME` 同为 `review-escalation-backend`，改动需同步。
- **本仓库源码当前不含任何自定义代码区标记**（无「自定义代码区 / USER CODE / CUSTOM CODE / region」等）。因此按全局 P0「无自定义区」情形处理：在非生成、非模板、非第三方代码内做最小充分改动；不得自行引入区域标记，也不得借区域标记扩大改动范围。

## 4. 修改约束

- 遵循全局 P0 / P0.5：注释是强制交付物，须出现在最终 diff；描述意图、约束与原因，不逐行翻译代码。
- 遵循全局 R7：不擅自改接口协议、字段、变量值、异常行为、执行顺序；范围外发现的潜在问题只记录，不顺手修。
- 遵循全局 R6：默认不编译、不构建、不打包、不发布；需要执行验证时须说明原因并取得授权。README 第 7 节的命令仅登记，不代表默认执行。
- 遵循全局 0B：按任务复杂度分级汇报。

## 5. 验证命令（只读登记）

| 用途 | 命令 |
|---|---|
| 后端冒烟测试 | `mvn -f backend/pom.xml test` |
| 前端生产构建 | `npm --prefix frontend run build` |
| 后端接口自检 | `curl -s http://localhost:8080/api/hello` |
| 代理链路自检 | `curl -s http://localhost:5173/api/hello` |

## 6. 基线边界

- 基线提交只包含初始化内容；不得在其中预置扩展功能、缺陷、`.agent-workflow/` 交接包或审查报告。
- 后续方案、实施与审查须按独立轮次进行，遵循不自动提交、不修改源码的审查只读边界。
- `review-config.json` 仅提供审查配置层的项目参数（栈包、契约面、忽略路径、命令登记），不替代任何审查判定规则。
