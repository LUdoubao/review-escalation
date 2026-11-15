package com.example.baseline.controller;

import com.example.baseline.dto.BoardResponse;
import com.example.baseline.dto.BoardSummaryResponse;
import com.example.baseline.dto.PageResponse;
import com.example.baseline.dto.TaskCreateRequest;
import com.example.baseline.dto.TaskMoveRequest;
import com.example.baseline.dto.TaskQuery;
import com.example.baseline.dto.TaskResponse;
import com.example.baseline.dto.TaskUpdateRequest;
import com.example.baseline.service.TaskBoardService;
import com.example.baseline.service.TaskQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 任务看板的 HTTP 入口，只负责路由、参数绑定、状态码与 delegate 调用。
 *
 * <p>领域：任务看板；关系：查询类请求委托 {@code TaskQueryService}，写类请求委托
 * {@code TaskBoardService}；本身不含业务分支、字段加工与异常包装——校验在 service，
 * 异常到响应体的转换在 {@code GlobalExceptionHandler}。
 * 权限：本地演示服务未引入鉴权，全部端点匿名可访问；跨域方法集合由 {@code CorsConfig} 约束。
 * 幂等：GET 与 DELETE 幂等；PATCH 移动在「同列且未指定位置」时幂等；POST 每次创建新任务。</p>
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskQueryService taskQueryService;

    private final TaskBoardService taskBoardService;

    public TaskController(TaskQueryService taskQueryService, TaskBoardService taskBoardService) {
        this.taskQueryService = taskQueryService;
        this.taskBoardService = taskBoardService;
    }

    /**
     * 分页查询任务列表。
     *
     * @param query 查询条件，由查询串绑定；页码从 1 开始
     * @return 当前页数据与分页元信息
     */
    @GetMapping
    public PageResponse<TaskResponse> page(TaskQuery query) {
        return taskQueryService.pageTasks(query);
    }

    /**
     * 查询看板聚合数据（四列及各列任务）。
     *
     * @param query 筛选条件，分页字段在此入口不生效
     * @return 列数组与聚合时刻
     */
    @GetMapping("/board")
    public BoardResponse board(TaskQuery query) {
        return taskQueryService.board(query);
    }

    /**
     * 查询汇总指标。
     *
     * @return 总数、各列数量、逾期数量与完成率
     */
    @GetMapping("/summary")
    public BoardSummaryResponse summary() {
        return taskQueryService.summary();
    }

    /**
     * 查询负责人候选。
     *
     * @return 去重后的负责人列表
     */
    @GetMapping("/assignees")
    public List<String> assignees() {
        return taskQueryService.assignees();
    }

    /**
     * 查询单个任务详情。
     *
     * @param id 任务主键
     * @return 任务出参
     */
    @GetMapping("/{id}")
    public TaskResponse detail(@PathVariable Long id) {
        return taskQueryService.detail(id);
    }

    /**
     * 新建任务，成功返回 201 与完整任务体。
     *
     * @param request 新建请求
     * @return 新建后的任务
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@RequestBody TaskCreateRequest request) {
        return taskBoardService.create(request);
    }

    /**
     * 局部更新任务字段。
     *
     * @param id      任务主键
     * @param request 编辑请求，null 字段表示不修改
     * @return 更新后的任务
     */
    @PatchMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @RequestBody TaskUpdateRequest request) {
        return taskBoardService.update(id, request);
    }

    /**
     * 移动任务到目标列的目标位置。
     *
     * @param id      任务主键
     * @param request 移动请求
     * @return 移动后的任务
     */
    @PatchMapping("/{id}/move")
    public TaskResponse move(@PathVariable Long id, @RequestBody TaskMoveRequest request) {
        return taskBoardService.move(id, request);
    }

    /**
     * 逻辑删除任务，成功返回 204 无响应体。
     *
     * @param id 任务主键
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id) {
        taskBoardService.remove(id);
    }
}
