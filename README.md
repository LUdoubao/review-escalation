# review-escalation

用于验证「审查 skill」实际有用性、并为其后续自迭代提供**稳定基线**的最小全栈项目。

## 1. 定位与本次交付边界

本仓库是实验用的对照基线，不是业务系统。本轮交付范围严格限定为：

- 可直接启动、并完成前后端联调的最小全栈版本；
- 清晰的后端 / 前端目录结构、固定依赖与启动配置；
- 一个最小联通功能：前端调用后端 `GET /api/hello` 并在页面展示返回结果；
- 供后续方案、实施与审查读取的项目级配置层。

**本轮不包含**：扩展功能、故意植入的缺陷、`.agent-workflow/` 交接包、审查报告、任何已执行或预置的审查流程。这些均为后续在基线之上按轮次追加的内容。

## 2. 技术栈

| 层 | 选型 | 版本 | 说明 |
|---|---|---|---|
| 后端 | Spring Boot | 3.3.5 | Maven 单模块；`spring-boot-starter-web`、`spring-boot-starter-test` |
| 运行时 | JDK | 17 | `java.version=17` |
| 前端 | Vue | ^3.5.13 | 使用原生 JavaScript，不使用 TypeScript |
| 构建 | Vite | ^5.4.11 | `@vitejs/plugin-vue` ^5.2.1 |
| HTTP | 浏览器 `fetch` | 内置 | 不引入 axios 等额外请求库 |

环境要求：JDK 17、Maven 3.9+、Node 22+、npm 10+。

## 3. 目录结构

```
review-escalation/
├── AGENTS.md                  # 项目级协作约定（模块边界、命名、验证命令）
├── review-config.json         # 审查 skill 的项目参数（栈包、契约面、只读命令）
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/example/baseline/
│       │   │   ├── BaselineApplication.java   # 启动类 / 组件扫描根
│       │   │   ├── config/CorsConfig.java     # 开发态跨域
│       │   │   ├── controller/HelloController.java  # HTTP 入口
│       │   │   ├── service/HelloService.java   # 唯一业务逻辑层
│       │   │   └── dto/HelloResponse.java      # 跨栈响应契约
│       │   └── resources/application.yml       # 端口与应用名
│       └── test/java/com/example/baseline/controller/HelloControllerTest.java
└── frontend/
    ├── package.json
    ├── vite.config.js          # dev server 端口与 /api 代理
    ├── index.html
    └── src/
        ├── main.js             # 应用挂载
        ├── App.vue             # 页面容器 / 请求三态
        ├── api/hello.js        # 请求层（唯一 HTTP 出口）
        ├── components/StatusCard.vue  # 纯展示组件
        └── styles/main.css     # 全局基础样式
```

模块边界：后端 `controller → service → dto`，控制器不含业务分支；前端 `App.vue → api/hello.js → StatusCard.vue`，请求层是唯一 HTTP 出口，组件不发起请求。

## 4. 启动

两个终端分别执行。

后端（端口 8080）：

```bash
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.arguments=--server.port=8080
```

> 显式传入 `--server.port=8080` 是为了把端口固定为跨栈契约的一部分。部分 IDE 集成终端会注入 `SERVER_PORT` / `SERVER__PORT` 等环境变量，Spring Boot 松散绑定将其视为高于 `application.yml` 的配置源，可能把服务拉到随机端口甚至因端口占用而启动失败；命令行参数优先级高于环境变量，可消除该干扰。在确认无此类变量的普通终端中，`mvn -f backend/pom.xml spring-boot:run` 等价可用。

前端（端口 5173，首次需安装依赖）：

```bash
cd frontend
npm install
npm run dev
```

前端走 Vite 代理：`http://localhost:5173/api/hello` → `http://localhost:8080/api/hello`。后端 `CorsConfig` 另放行了 `http://localhost:5173` 的 GET/OPTIONS，以便改为直连后端时仍可联调。端口配置分布在 `backend/src/main/resources/application.yml`、`frontend/vite.config.js`、`CorsConfig.java` 三处，修改时必须同步。

## 5. 联调验收标准

可观察、可复核的三项：

1. 后端直连返回 HTTP 200 与三个字段：
   ```bash
   curl -s http://localhost:8080/api/hello
   # {"message":"Hello from Spring Boot","service":"review-escalation-backend","timestamp":"..."}
   ```
2. 经前端代理返回相同结构：
   ```bash
   curl -s http://localhost:5173/api/hello
   ```
3. 浏览器打开 `http://localhost:5173`，页面自动展示 `message / service / timestamp` 三行结果；点击「重新请求」后 `timestamp` 更新、无错误态；停止后端后点击按钮显示 `请求失败：...` 错误态且按钮恢复可用。

## 6. 接口契约

`GET /api/hello`，无请求参数，无鉴权，只读且无副作用。

| 字段 | 类型 | 含义 |
|---|---|---|
| `message` | string | 固定文案 `Hello from Spring Boot` |
| `service` | string | 固定值 `review-escalation-backend` |
| `timestamp` | string | 服务端生成时刻，UTC ISO-8601；前端仅原样展示，不解析 |

契约面两侧落点已登记在 `review-config.json` 的 `contract_surface`，供后续跨栈一致性检查机械对照；该文件只是配置，不构成审查流程的实现。

## 7. 验证命令

```bash
mvn -f backend/pom.xml test      # 后端冒烟测试
npm --prefix frontend run build  # 前端生产构建
```

`review-config.json` 的 `commands` 仅登记上述只读命令供审查方引用，不会由配置自动触发执行。

## 8. 基线使用约定

- 保持工作区干净：基线提交是后续工作流固定的 `base_commit`，不要把构建产物、依赖目录或本地实验结果混入基线。
- 后续扩展功能、缺陷植入与审查均应在**新的方案 / 实施 / 审查轮次**中进行，不在基线提交上直接叠加。
- `AGENTS.md` 与 `review-config.json` 为项目事实与参数的声明，若目录、端口、契约字段发生变化，须同步更新，否则后续方案与审查会基于过期事实。
