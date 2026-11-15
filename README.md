# review-escalation

用于验证「审查 skill」实际有用性、并为其后续自迭代提供**稳定基线**的最小全栈项目。

## 1. 定位与交付边界

本仓库是实验用的对照基线，不是业务系统。

- 第一轮交付：可直接启动的前后端联通版本，唯一功能是前端调用 `GET /api/hello` 并展示返回结果。
- 第二轮交付：在联通版本之上新增**任务看板**业务模块，覆盖前端交互、后端接口、数据流转与状态管理全链路；`/api/hello` 链路保持不变，继续作为联通性自检入口。
- 后续轮次（审查、修复、方案迭代）在已落地代码之上按独立轮次进行，不在本文件描述。

## 2. 技术栈

| 层 | 选型 | 版本 | 说明 |
|---|---|---|---|
| 后端 | Spring Boot | 3.3.5 | Maven 单模块；`spring-boot-starter-web`、`spring-boot-starter-test` |
| 运行时 | JDK | 17 | `java.version=17` |
| 持久化 | MyBatis-Plus | 3.5.9 | `mybatis-plus-spring-boot3-starter`；XML 手写 SQL + `BaseMapper` |
| 数据库 | MySQL | 8.0 | 库 `review_escalation`（演示）、`review_escalation_test`（测试），字符集 `utf8mb4` |
| 驱动 | mysql-connector-j | 由 Spring Boot 管理 | `runtime` 作用域 |
| 前端 | Vue | ^3.5.13 | 使用原生 JavaScript，不使用 TypeScript |
| 构建 | Vite | ^5.4.11 | `@vitejs/plugin-vue` ^5.2.1 |
| HTTP | 浏览器 `fetch` | 内置 | 不引入 axios；统一封装在 `src/api/http.js` |
| 状态 | Vue `reactive` | 内置 | 模块级单例 store，不引入 Pinia |

环境要求：JDK 17、Maven 3.9+、Node 22+、npm 10+、MySQL 8.0（本机 3306 可用）。

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
│       │   │   ├── BaselineApplication.java      # 启动类 / 组件扫描根 / MapperScan
│       │   │   ├── config/CorsConfig.java        # 开发态跨域
│       │   │   ├── config/MybatisPlusConfig.java # 乐观锁插件装配
│       │   │   ├── controller/HelloController.java   # 连通性入口
│       │   │   ├── controller/TaskController.java    # 任务看板入口（9 个路由）
│       │   │   ├── domain/TaskStatus.java        # 看板列枚举
│       │   │   ├── domain/TaskPriority.java      # 优先级枚举
│       │   │   ├── domain/BoardTask.java         # board_task 实体
│       │   │   ├── dto/                          # 跨栈契约（响应体与请求体）
│       │   │   ├── exception/                    # 业务异常与全局异常出口
│       │   │   ├── mapper/BoardTaskMapper.java   # 数据访问接口
│       │   │   ├── service/HelloService.java     # 连通性逻辑
│       │   │   ├── service/TaskQueryService.java # 查询侧：分页/聚合/汇总
│       │   │   └── service/TaskBoardService.java # 命令侧：新建/编辑/移动/删除
│       │   └── resources/
│       │       ├── application.yml               # 端口、数据源、ORM、SQL 初始化
│       │       ├── schema.sql                    # board_task 建表（幂等）
│       │       ├── data.sql                      # 演示数据（INSERT IGNORE）
│       │       └── mapper/BoardTaskMapper.xml    # 看板 SQL
│       └── test/
│           ├── java/com/example/baseline/controller/  # 冒烟测试
│           └── resources/application.yml         # 测试库数据源覆盖
└── frontend/
    ├── package.json
    ├── vite.config.js          # dev server 端口与 /api 代理
    ├── index.html
    └── src/
        ├── main.js             # 应用挂载
        ├── App.vue             # 页面容器：看板 / 列表 / 联通性自检
        ├── api/http.js         # fetch 封装与错误归一化
        ├── api/hello.js        # 连通性请求
        ├── api/tasks.js        # 任务看板请求
        ├── stores/boardStore.js # 模块级状态单例
        ├── utils/date.js       # 日期展示与逾期判定
        ├── utils/debounce.js   # 检索防抖
        ├── components/         # 纯 props 渲染组件
        └── styles/main.css     # 全局基础样式
```

模块边界：
- 后端 `controller → service → mapper → MySQL`，控制器不含业务分支，业务规则集中在两个 service。
- 前端 `App.vue → stores/boardStore.js → api/tasks.js → api/http.js`，请求层是唯一 HTTP 出口，组件不发起请求、不持有跨组件状态。

## 4. 启动

### 4.1 准备数据库

只需创建库，表与演示数据由应用启动时的 `schema.sql` / `data.sql` 幂等创建：

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS review_escalation DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS review_escalation_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
```

数据源地址写在 `backend/src/main/resources/application.yml`，**账号与口令不入库**：用户名取 `DB_USERNAME`（缺省 `root`），口令取 `DB_PASSWORD`，仓库内只保留占位符且不为口令设默认值。启动前必须先导出 `DB_PASSWORD`，否则应用与测试会因占位符无法解析而在启动阶段直接失败（不会出现「口令为空却连上库」的假成功）。测试库配置在 `backend/src/test/resources/application.yml`，同样从环境变量读取口令。

手工导入 `data.sql` 时须带 `--default-character-set=utf8mb4`，否则中文会按控制台默认编码写入而出现乱码。

### 4.2 后端（端口 8080）

```bash
export DB_PASSWORD='你的 MySQL 口令'   # 必填，不写入仓库；DB_USERNAME 缺省为 root
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.arguments=--server.port=8080
```

> 显式传入 `--server.port=8080` 是为了把端口固定为跨栈契约的一部分。部分 IDE 集成终端会注入 `SERVER_PORT` / `SERVER__PORT` 等环境变量，Spring Boot 松散绑定将其视为高于 `application.yml` 的配置源，可能把服务拉到随机端口甚至因端口占用而启动失败；命令行参数优先级高于环境变量，可消除该干扰。

### 4.3 前端（端口 5173）

```bash
cd frontend
npm install
npm run dev
```

前端走 Vite 代理：`http://localhost:5173/api/**` → `http://localhost:8080/api/**`，不做路径重写。后端 `CorsConfig` 另放行了 `http://localhost:5173` 的 `GET/POST/PATCH/DELETE/OPTIONS`，以便改为直连后端时仍可联调。端口配置分布在 `application.yml`、`vite.config.js`、`CorsConfig.java` 三处，修改时必须同步。

## 5. 联调验收标准

可观察、可复核的六项：

1. 后端直连返回 HTTP 200 与三个字段：
   ```bash
   curl -s http://localhost:8080/api/hello
   # {"message":"Hello from Spring Boot","service":"review-escalation-backend","timestamp":"..."}
   ```
2. 经前端代理返回相同结构：
   ```bash
   curl -s http://localhost:5173/api/hello
   ```
3. 看板接口返回四列，列顺序固定：
   ```bash
   curl -s http://localhost:8080/api/tasks/board
   # columns[0..3].status = TODO / DOING / REVIEW / DONE
   ```
4. 列表接口分页可用：
   ```bash
   curl -s "http://localhost:8080/api/tasks?page=1&size=5"
   # items 长度 ≤ 5，page=1，size=5
   ```
5. 新建与校验：
   ```bash
   curl -s -X POST http://localhost:8080/api/tasks -H "Content-Type: application/json" -d '{"title":"冒烟任务","priority":"HIGH"}'
   # 201，status=TODO；title 传 "   " 时 400 且 code=TASK_TITLE_REQUIRED
   ```
6. 浏览器打开 `http://localhost:5173`：看板四列渲染出演示任务；「列表」视图出现分页条；输入关键字后列表刷新；停止后端再操作出现错误态并可重试；页面底部「联通性自检」仍展示 `message / service / timestamp`。

## 6. 接口契约

### 6.1 连通性

`GET /api/hello`，无请求参数，无鉴权，只读且无副作用。

| 字段 | 类型 | 含义 |
|---|---|---|
| `message` | string | 固定文案 `Hello from Spring Boot` |
| `service` | string | 固定值 `review-escalation-backend` |
| `timestamp` | string | 服务端生成时刻，UTC ISO-8601；前端仅原样展示，不解析 |

### 6.2 任务看板

| 方法 | 路径 | 入参 | 出参 |
|---|---|---|---|
| GET | `/api/tasks` | `page,size,status,priority,assignee,keyword` | `{ items, totalItems, page, size, totalPages }` |
| GET | `/api/tasks/board` | `keyword,assignee,priority` | `{ columns: [{ status, label, total, tasks }], generatedAt }` |
| GET | `/api/tasks/summary` | 无 | `{ total, todoCount, doingCount, reviewCount, doneCount, overdueCount, completionRate }` |
| GET | `/api/tasks/assignees` | 无 | `string[]` |
| GET | `/api/tasks/{id}` | 路径 `id` | 任务对象 |
| POST | `/api/tasks` | 任务新建体 | 任务对象（201） |
| PATCH | `/api/tasks/{id}` | 任务编辑体（含 `version`、可选 `clearFields`） | 任务对象 |
| PATCH | `/api/tasks/{id}/move` | `{ targetStatus, position?, version? }` | 任务对象 |
| DELETE | `/api/tasks/{id}` | 路径 `id` | 空（204） |

任务对象字段：`id / title / description / assignee / priority / priorityLabel / status / statusLabel / terminal / tag / dueDate / sortOrder / overdue / version / updatedAt`。`status` 取 `TODO|DOING|REVIEW|DONE`，`priority` 取 `LOW|MEDIUM|HIGH|URGENT`；`terminal` 为终态标记，`DONE` 为唯一终态。

编辑语义为三态：字段缺省 = 不修改；字段有值 = 更新；字段名列入 `clearFields` = 置空（`description / assignee / tag / dueDate`）。`overdue` 由服务端按业务时区派生，前端直接采用，不自行比较日期。

移动语义：`DONE` 为终态，**不可再移动到其他列**（`PATCH /api/tasks/{id}/move` 返回 409 `TASK_STATUS_TERMINAL`）；需要继续处理时新建任务。前端对终态卡片与表格行不渲染移动入口。

错误体统一为 `{ code, message, timestamp }`，`code` 为机器可读业务码（如 `TASK_NOT_FOUND`、`TASK_VERSION_CONFLICT`、`TASK_STATUS_TERMINAL`、`TASK_CLEAR_FIELD_INVALID`）。

### 6.3 字段同步点

任何契约字段增删改必须同时修改：`dto/TaskResponse.java`、`service/TaskQueryService.java`（`toResponse`）、`frontend/src/api/tasks.js`（字段注释）、`frontend/src/components/TaskCard.vue` 与 `TaskTable.vue`（渲染行）、本文件第 6.2 节。

契约面两侧落点登记在 `review-config.json` 的 `contract_surface`，供跨栈一致性检查机械对照；该文件只是配置，不构成审查流程的实现。

## 7. 验证命令

```bash
export DB_PASSWORD='你的 MySQL 口令'   # 测试库同样从环境变量读取口令
mvn -f backend/pom.xml test      # 后端测试（8 个用例，依赖本机 MySQL 的 review_escalation_test 库）
npm --prefix frontend run build  # 前端生产构建
```

`review-config.json` 的 `commands` 仅登记上述只读命令供审查方引用，不会由配置自动触发执行。

## 8. 基线使用约定

- 保持工作区干净：不要把构建产物、依赖目录或本地实验结果混入提交。
- 后续扩展、缺陷定位与审查均应在**新的方案 / 实施 / 审查轮次**中进行，不在既有提交上直接叠加无关改动。
- `AGENTS.md` 与 `review-config.json` 为项目事实与参数的声明，若目录、端口、数据源、契约字段发生变化，须同步更新，否则后续方案与审查会基于过期事实。
- 演示数据可随时重建：清空 `board_task` 后重新执行 `data.sql` 即可回到初始 14 条。
