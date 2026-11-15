package com.example.baseline.service;

import com.example.baseline.domain.BoardTask;
import com.example.baseline.domain.TaskPriority;
import com.example.baseline.domain.TaskStatus;
import com.example.baseline.dto.BoardColumnResponse;
import com.example.baseline.dto.BoardResponse;
import com.example.baseline.dto.BoardSummaryResponse;
import com.example.baseline.dto.PageResponse;
import com.example.baseline.dto.TaskQuery;
import com.example.baseline.dto.TaskResponse;
import com.example.baseline.exception.ApiException;
import com.example.baseline.mapper.BoardTaskMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 看板查询侧服务：承担分页、看板聚合、汇总统计与出参装配，是只读链路的唯一业务层。
 *
 * <p>领域：任务看板；关系：被 {@code TaskController} 调用，向下依赖 {@code BoardTaskMapper}；
 * 同时向 {@code TaskBoardService} 提供 {@link #toResponse} 这一出参装配入口，
 * 保证命令侧与查询侧的字段口径只有一处实现。
 * 事务/线程：全部方法只读，不开启事务；无实例状态，可并发调用。
 * 生命周期：Spring 单例 Bean。</p>
 */
@Service
public class TaskQueryService {

    /** 每页条数缺省值，与 TaskQuery 的默认值保持一致。 */
    private static final int DEFAULT_PAGE_SIZE = 5;

    /** 每页条数上限：防止调用方传入极大值导致一次性扫表。 */
    private static final int MAX_PAGE_SIZE = 50;

    /** 看板单列最多返回的任务数：超出部分通过列头「共 N 条」提示，不阻塞首屏渲染。 */
    private static final int BOARD_COLUMN_LIMIT = 8;

    /** 时间戳格式：带时区偏移的 ISO-8601，与前端直接展示的字符串保持同构。 */
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final BoardTaskMapper boardTaskMapper;

    /** 业务时钟：所有「今天/现在」的判断都从它取，避免依赖容器 JVM 的默认时区。 */
    private final Clock clock;

    public TaskQueryService(BoardTaskMapper boardTaskMapper, Clock clock) {
        this.boardTaskMapper = boardTaskMapper;
        this.clock = clock;
    }

    /**
     * 按筛选条件分页查询任务。
     *
     * @param query 查询条件，页码从 1 开始
     * @return 当前页数据与分页元信息
     * @throws ApiException 页码小于 1 时抛出 TASK_PAGE_INVALID（400）
     */
    public PageResponse<TaskResponse> pageTasks(TaskQuery query) {
        // 页码是分页语义的起点：0 或负数无法换算成合法的跳过行数，直接拒绝而不是静默纠正，
        // 否则调用方会以为请求生效，实际拿到的是另一页数据。
        if (query.getPage() < 1) {
            throw new ApiException("TASK_PAGE_INVALID", HttpStatus.BAD_REQUEST, "页码必须从 1 开始");
        }
        int size = resolvePageSize(query.getSize());
        int offset = (query.getPage() - 1) * size;

        List<BoardTask> rows = boardTaskMapper.selectPageList(query, offset, size);
        long totalItems = boardTaskMapper.countByQuery(query);
        int totalPages = totalItems == 0 ? 0 : (int) ((totalItems + size - 1) / size);

        List<TaskResponse> items = rows.stream().map(this::toResponse).toList();
        return new PageResponse<>(items, totalItems, query.getPage(), size, totalPages);
    }

    /**
     * 聚合看板数据：按状态分列，列内按序号取前若干条。
     *
     * @param query 筛选条件，分页字段在此入口不生效
     * @return 列数组与聚合时刻
     */
    public BoardResponse board(TaskQuery query) {
        // 每列截断在数据库侧完成：窗口函数只返回各列前 N 条，避免把整表读进内存后再丢弃。
        List<BoardTask> rows = boardTaskMapper.selectBoardPage(query, BOARD_COLUMN_LIMIT);

        // 列头数量单独聚合：截断后的行数不等于该列命中总数，两者不能互相替代。
        Map<String, Long> totals = new HashMap<>();
        for (Map<String, Object> row : boardTaskMapper.selectColumnTotals(query)) {
            Object status = row.get("status");
            Object total = row.get("total");
            if (status != null && total instanceof Number number) {
                totals.put(String.valueOf(status), number.longValue());
            }
        }

        Map<String, List<BoardTask>> grouped = rows.stream()
                .collect(Collectors.groupingBy(BoardTask::getStatus, LinkedHashMap::new, Collectors.toList()));

        List<BoardColumnResponse> columns = new ArrayList<>();
        // 四列恒定输出：列是看板的结构而不是查询结果，空列必须保留，否则前端既看不到该列，
        // 也无法把卡片移入其中。列顺序以枚举声明为准，不受数据库返回顺序影响。
        for (TaskStatus status : TaskStatus.ordered()) {
            List<BoardTask> tasks = grouped.getOrDefault(status.getCode(), List.of());
            List<TaskResponse> limited = tasks.stream().map(this::toResponse).toList();
            long total = totals.getOrDefault(status.getCode(), 0L);
            columns.add(new BoardColumnResponse(status.getCode(), status.getLabel(), total, limited));
        }
        return new BoardResponse(columns, OffsetDateTime.now(clock).format(TIMESTAMP));
    }

    /**
     * 汇总统计：总数、各列数量、逾期数量与完成率。
     *
     * @return 汇总指标
     */
    public BoardSummaryResponse summary() {
        long total = boardTaskMapper.countAll();
        long todoCount = boardTaskMapper.countByStatus(TaskStatus.TODO.getCode());
        long doingCount = boardTaskMapper.countByStatus(TaskStatus.DOING.getCode());
        long reviewCount = boardTaskMapper.countByStatus(TaskStatus.REVIEW.getCode());
        long doneCount = boardTaskMapper.countByStatus(TaskStatus.DONE.getCode());

        // 逾期口径：有截止日、已过截止日、且未完成。已完成任务不再计入逾期（由 SQL 过滤），
        // 日期以业务时区的当天为准，截止日当天仍算有效期内。
        LocalDate today = LocalDate.now(clock);
        long overdueCount = boardTaskMapper.countOverdue(today);

        // 完成率按四舍五入取整到个位：先转浮点再乘 100，避免整数除法把非满值全部截成 0。
        int completionRate = total == 0 ? 0 : (int) Math.round(doneCount * 100.0 / total);

        return new BoardSummaryResponse(total, todoCount, doingCount, reviewCount, doneCount,
                overdueCount, completionRate);
    }

    /**
     * 取负责人候选，供筛选下拉与表单候选使用。
     *
     * @return 去重后的负责人列表，可能为空
     */
    public List<String> assignees() {
        return boardTaskMapper.selectAssignees();
    }

    /**
     * 按主键取单个任务详情。
     *
     * @param id 任务主键
     * @return 任务出参
     * @throws ApiException 任务不存在或已删除时抛出 TASK_NOT_FOUND（404）
     */
    public TaskResponse detail(Long id) {
        BoardTask task = boardTaskMapper.selectById(id);
        if (task == null) {
            throw new ApiException("TASK_NOT_FOUND", HttpStatus.NOT_FOUND, "任务不存在或已被删除");
        }
        return toResponse(task);
    }

    /**
     * 把持久化实体装配为跨栈出参，是编码到显示名的唯一转换点。
     *
     * @param task 持久化实体，非空
     * @return 任务出参；逾期标记按「有截止日且已过当天且未完成」计算
     */
    public TaskResponse toResponse(BoardTask task) {
        TaskStatus status = TaskStatus.of(task.getStatus());
        TaskPriority priority = TaskPriority.of(task.getPriority());
        // 逾期为派生字段，不入库：它随日期推移而变化，落库会产生需要定时刷新的脏数据。
        // 判定与 summary 使用同一时钟与同一口径（已完成不算逾期），两处结果不会互相矛盾。
        boolean overdue = task.getDueDate() != null
                && task.getDueDate().isBefore(LocalDate.now(clock))
                && status != TaskStatus.DONE;
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getAssignee(),
                priority.getCode(),
                priority.getLabel(),
                status.getCode(),
                status.getLabel(),
                status.isTerminal(),
                task.getTag(),
                task.getDueDate(),
                task.getSortOrder(),
                overdue,
                task.getVersion(),
                task.getUpdatedAt());
    }

    /**
     * 归一化每页条数。
     *
     * @param size 调用方传入的每页条数
     * @return 小于 1 时回落为默认值，大于上限时截断为上限，其余取值原样保留
     */
    private int resolvePageSize(int size) {
        if (size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
