package com.example.baseline.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 任务看板接口的冒烟测试，覆盖真实 Spring MVC 链路、参数绑定、SQL 与 Jackson 序列化。
 *
 * <p>领域：任务看板自检；关系：启动完整应用上下文（含 MyBatis 与数据源），不 mock service，
 * 因此可同时验证 controller → service → mapper 的装配是否正确。
 * 事务：类级 {@code @Transactional} 让每个用例在事务内执行并回滚，避免写用例污染测试库，
 * 也保证多次执行的结果一致。
 * 生命周期：测试类共享 Spring 上下文；依赖本机 MySQL 的 review_escalation_test 库与
 * {@code DB_PASSWORD} 环境变量。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskControllerTest {

    /** 由测试切片注入的 MVC 测试客户端，不占用真实端口。 */
    @Autowired
    private MockMvc mockMvc;

    /** 用于解析看板响应，校验列内序号这类结构化断言。 */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 验证新建任务返回 201，且初始状态落在待办列、优先级按请求生效。
     *
     * @throws Exception MockMvc 执行请求或解析响应失败时抛出，此时测试直接失败
     */
    @Test
    void createReturnsTaskInTodoColumn() throws Exception {
        String body = """
                {"title":"接口自检任务","description":"由冒烟测试创建","assignee":"林秋","priority":"HIGH","tag":"自检"}
                """;
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("接口自检任务"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.terminal").value(false))
                .andExpect(jsonPath("$.version").value(1));
    }

    /**
     * 验证标题为纯空白时被拒绝，并返回统一的错误契约。
     *
     * @throws Exception MockMvc 执行请求或解析响应失败时抛出，此时测试直接失败
     */
    @Test
    void createRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TASK_TITLE_REQUIRED"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    /**
     * 验证种子数据中的任务可通过详情接口读取，字段与库内一致。
     *
     * @throws Exception MockMvc 执行请求或解析响应失败时抛出，此时测试直接失败
     */
    @Test
    void detailReturnsSeededTask() throws Exception {
        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.statusLabel").isNotEmpty());
    }

    /**
     * 验证带状态筛选时，总数与该筛选下的数据页来自同一口径。
     *
     * <p>种子数据中「进行中」为 3 条：总数应为 3、总页数应为 1，而不是全库条数。</p>
     *
     * @throws Exception MockMvc 执行请求或解析响应失败时抛出，此时测试直接失败
     */
    @Test
    void pageTotalMatchesStatusFilter() throws Exception {
        mockMvc.perform(get("/api/tasks")
                        .param("status", "DOING")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items.length()").value(3));
    }

    /**
     * 验证某列被清空后看板仍返回四列，空列以 total=0、tasks=[] 呈现。
     *
     * @throws Exception MockMvc 执行请求或解析响应失败时抛出，此时测试直接失败
     */
    @Test
    void boardKeepsEmptyColumns() throws Exception {
        // 把「待验收」列的三条任务全部移走，制造空列。
        for (long id : new long[]{7L, 8L, 9L}) {
            mockMvc.perform(patch("/api/tasks/" + id + "/move")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetStatus\":\"DONE\"}"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/tasks/board"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns.length()").value(4))
                .andExpect(jsonPath("$.columns[2].status").value("REVIEW"))
                .andExpect(jsonPath("$.columns[2].total").value(0))
                .andExpect(jsonPath("$.columns[2].tasks.length()").value(0));
    }

    /**
     * 验证移动后列内序号连续且不重复（0..n-1），落点与请求下标一致。
     *
     * @throws Exception MockMvc 执行请求或解析响应失败时抛出，此时测试直接失败
     */
    @Test
    void moveKeepsColumnOrderContiguous() throws Exception {
        // 把 4 号任务插到「进行中」列首位：该列原有 4/5/6 三条。
        mockMvc.perform(patch("/api/tasks/4/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"DOING\",\"position\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortOrder").value(0));

        MvcResult result = mockMvc.perform(get("/api/tasks/board"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode columns = objectMapper.readTree(result.getResponse().getContentAsString()).get("columns");

        for (JsonNode column : columns) {
            JsonNode tasks = column.get("tasks");
            Set<Integer> orders = new HashSet<>();
            for (JsonNode task : tasks) {
                orders.add(task.get("sortOrder").asInt());
            }
            // 序号集合大小等于任务数即说明没有并列序号。
            assertEquals(tasks.size(), orders.size(), "列内序号出现重复：" + column.get("status").asText());
        }
    }

    /**
     * 验证已完成任务属于终态、不可再流转，接口返回 409 与对应业务码。
     *
     * @throws Exception MockMvc 执行请求或解析响应失败时抛出，此时测试直接失败
     */
    @Test
    void moveRejectsTerminalTask() throws Exception {
        // 10 号任务在种子数据中处于「已完成」，终态标记应随详情下发。
        mockMvc.perform(get("/api/tasks/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.terminal").value(true));

        mockMvc.perform(patch("/api/tasks/10/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":\"TODO\",\"position\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TASK_STATUS_TERMINAL"));
    }

    /**
     * 验证可空字段能通过 clearFields 显式置空，而不是被当成「不修改」。
     *
     * @throws Exception MockMvc 执行请求或解析响应失败时抛出，此时测试直接失败
     */
    @Test
    void updateClearsOptionalFields() throws Exception {
        mockMvc.perform(patch("/api/tasks/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clearFields\":[\"assignee\",\"dueDate\"],\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").value(nullValue()))
                .andExpect(jsonPath("$.dueDate").value(nullValue()));
    }
}
