package com.example.baseline.service;

import com.example.baseline.domain.BoardTask;
import com.example.baseline.domain.TaskPriority;
import com.example.baseline.domain.TaskStatus;
import com.example.baseline.dto.TaskCreateRequest;
import com.example.baseline.dto.TaskMoveRequest;
import com.example.baseline.dto.TaskResponse;
import com.example.baseline.dto.TaskUpdateRequest;
import com.example.baseline.exception.ApiException;
import com.example.baseline.mapper.BoardTaskMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 看板命令侧服务：承担任务的新建、编辑、移动与删除，是写链路的唯一业务层。
 *
 * <p>领域：任务看板；关系：被 {@code TaskController} 调用，向下依赖 {@code BoardTaskMapper}，
 * 出参装配复用 {@code TaskQueryService#toResponse} 以保证与查询侧字段口径一致。
 * 事务：四个写方法均开启事务，任一环节抛出异常整体回滚；涉及同列多行改写的移动操作
 * 必须在一个事务内完成，避免中间态被并发读请求观察到。
 * 并发：所有写操作在读取后校验 {@code version}，更新影响行数为 0 时按冲突处理，
 * 保证两人同时编辑同一张卡片时后提交者收到明确失败而不是静默覆盖。
 * 生命周期：Spring 单例 Bean，无实例状态。</p>
 */
@Service
public class TaskBoardService {

    /** 标题最大长度，与 board_task.title 的列宽一致；超长在写库前拦下，避免数据库截断或报错。 */
    private static final int MAX_TITLE_LENGTH = 80;

    /** 描述最大长度，与 board_task.description 列宽一致。 */
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    /** 负责人最大长度，与 board_task.assignee 列宽一致。 */
    private static final int MAX_ASSIGNEE_LENGTH = 40;

    /** 标签最大长度，与 board_task.tag 列宽一致。 */
    private static final int MAX_TAG_LENGTH = 24;

    /** 允许通过 clearFields 显式置空的字段名，防止调用方传入任意字段名被静默忽略。 */
    private static final Set<String> CLEARABLE_FIELDS = Set.of("description", "assignee", "tag", "dueDate");

    private final BoardTaskMapper boardTaskMapper;

    private final TaskQueryService taskQueryService;

    /** 业务时钟：截止日校验与时间戳写入统一取它，不受容器 JVM 默认时区影响。 */
    private final Clock clock;

    public TaskBoardService(BoardTaskMapper boardTaskMapper, TaskQueryService taskQueryService, Clock clock) {
        this.boardTaskMapper = boardTaskMapper;
        this.taskQueryService = taskQueryService;
        this.clock = clock;
    }

    /**
     * 新建任务，落到待办列尾。
     *
     * @param request 新建请求
     * @return 新建后的任务出参，含数据库回填的主键与初始版本号
     * @throws ApiException 标题为空或超长、字段超长、优先级非法、截止日早于当天时抛出（400）
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskResponse create(TaskCreateRequest request) {
        String title = requireTitle(request.title());
        String description = normalizeOptional(request.description(), MAX_DESCRIPTION_LENGTH,
                "TASK_DESCRIPTION_TOO_LONG", "描述");
        String assignee = normalizeOptional(request.assignee(), MAX_ASSIGNEE_LENGTH,
                "TASK_ASSIGNEE_TOO_LONG", "负责人");
        String tag = normalizeOptional(request.tag(), MAX_TAG_LENGTH, "TASK_TAG_TOO_LONG", "标签");
        String priority = parsePriority(request.priority()).getCode();
        requireDueDate(request.dueDate());

        // 追加到列尾：先锁住目标列再读最大序号，使「读最大值 + 写入」在并发下串行，
        // 避免两个并发新建读到同一个最大值而写出重复序号。空列从 0 起算。
        boardTaskMapper.lockColumn(TaskStatus.TODO.getCode());
        Integer maxSortOrder = boardTaskMapper.selectMaxSortOrder(TaskStatus.TODO.getCode());
        int sortOrder = maxSortOrder == null ? 0 : maxSortOrder + 1;

        LocalDateTime now = LocalDateTime.now(clock);
        BoardTask entity = new BoardTask();
        entity.setTitle(title);
        entity.setDescription(description);
        entity.setAssignee(assignee);
        entity.setPriority(priority);
        entity.setStatus(TaskStatus.TODO.getCode());
        entity.setTag(tag);
        entity.setDueDate(request.dueDate());
        entity.setSortOrder(sortOrder);
        entity.setVersion(1);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setDeleted(0);
        boardTaskMapper.insert(entity);

        return taskQueryService.toResponse(entity);
    }

    /**
     * 局部更新任务字段，null 字段表示保持原值。
     *
     * @param id      任务主键
     * @param request 编辑请求
     * @return 更新后的任务出参，版本号已自增
     * @throws ApiException 任务不存在（404）、版本冲突（409）、字段非法（400）
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskResponse update(Long id, TaskUpdateRequest request) {
        BoardTask task = requireTask(id);
        requireVersion(task, request.version());

        // 置空走显式声明（clearFields），与「字段缺省=不修改」区分开：
        // 同一个 null 不再同时表达两种意图，调用方也没法靠「猜」把字段清掉或保住。
        Set<String> clears = parseClearFields(request.clearFields());

        if (request.title() != null) {
            task.setTitle(requireTitle(request.title()));
        }
        if (clears.contains("description")) {
            task.setDescription(null);
        } else if (request.description() != null) {
            task.setDescription(normalizeOptional(request.description(), MAX_DESCRIPTION_LENGTH,
                    "TASK_DESCRIPTION_TOO_LONG", "描述"));
        }
        if (clears.contains("assignee")) {
            task.setAssignee(null);
        } else if (request.assignee() != null) {
            task.setAssignee(normalizeOptional(request.assignee(), MAX_ASSIGNEE_LENGTH,
                    "TASK_ASSIGNEE_TOO_LONG", "负责人"));
        }
        if (request.priority() != null) {
            task.setPriority(parsePriority(request.priority()).getCode());
        }
        if (clears.contains("tag")) {
            task.setTag(null);
        } else if (request.tag() != null) {
            task.setTag(normalizeOptional(request.tag(), MAX_TAG_LENGTH, "TASK_TAG_TOO_LONG", "标签"));
        }
        if (clears.contains("dueDate")) {
            task.setDueDate(null);
        } else if (request.dueDate() != null) {
            // 只在日期真正变化时校验「不得早于今天」：逾期任务保留历史截止日不应妨碍修改其他字段，
            // 否则一旦过期就再也改不了负责人与描述。
            if (!request.dueDate().equals(task.getDueDate())) {
                requireDueDate(request.dueDate());
            }
            task.setDueDate(request.dueDate());
        }
        task.setUpdatedAt(LocalDateTime.now(clock));

        // 影响行数为 0 说明版本已被他人推进：此时内存中的实体已过期，不能再当作更新成功返回。
        if (boardTaskMapper.updateById(task) == 0) {
            throw versionConflict();
        }
        return taskQueryService.toResponse(task);
    }

    /**
     * 移动任务到目标列的指定位置，并把插入点之后的卡片依次顺延一位。
     *
     * @param id      任务主键
     * @param request 移动请求，position 为目标列内下标（0 基），为空表示追加到列尾
     * @return 移动后的任务出参，状态与序号均为新值
     * @throws ApiException 任务不存在（404）、目标状态非法（400）、版本冲突（409）
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskResponse move(Long id, TaskMoveRequest request) {
        BoardTask task = requireTask(id);
        TaskStatus targetStatus = parseStatus(request.targetStatus());
        requireVersion(task, request.version());

        // 同列且未指定位置：视为重复提交，直接返回当前状态，避免同一次拖拽产生两次位移。
        if (targetStatus.getCode().equals(task.getStatus()) && request.position() == null) {
            return taskQueryService.toResponse(task);
        }

        TaskStatus sourceStatus = TaskStatus.of(task.getStatus());
        boolean sameColumn = sourceStatus == targetStatus;

        // 终态锁定：已完成任务不再接受流转。允许回流会让完成率、各列计数与逾期统计同时回退，
        // 看板也就失去了「进度」含义。需要继续处理时应新建任务，而不是把旧任务拖回去。
        if (sourceStatus.isTerminal()) {
            throw new ApiException("TASK_STATUS_TERMINAL", HttpStatus.CONFLICT,
                    "任务已完成，不能再移动到其他列");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        // 按状态枚举顺序锁住涉及的列并取整列快照：取锁顺序与请求方向无关，
        // 反向并发移动会排队而不是互等；源列也纳入同一把锁，补齐序号时不会读到另一个视图。
        Map<String, List<BoardTask>> lockedColumns = lockColumns(sourceStatus, targetStatus);
        List<BoardTask> targetColumn = new ArrayList<>(lockedColumns.get(targetStatus.getCode()));
        // 同列移动时先把自身摘出，再按目标下标插回；否则自身也参与重排，落点会偏移一位。
        targetColumn.removeIf(item -> Objects.equals(item.getId(), task.getId()));

        int insertAt = clampPosition(request.position(), targetColumn.size());
        task.setStatus(targetStatus.getCode());
        task.setUpdatedAt(now);
        targetColumn.add(insertAt, task);

        // 一次性把整列序号重写为 0..n-1：不会出现并列序号，也不会留下空洞。
        applySortOrders(targetColumn, now);
        // 状态字段随 updateById 落库，同时承担乐观锁校验。
        if (boardTaskMapper.updateById(task) == 0) {
            throw versionConflict();
        }
        // 跨列移动后源列会留下序号空洞，用同一把锁内读到的快照补齐为连续值。
        if (!sameColumn) {
            List<BoardTask> sourceColumn = new ArrayList<>(lockedColumns.get(sourceStatus.getCode()));
            sourceColumn.removeIf(item -> Objects.equals(item.getId(), task.getId()));
            applySortOrders(sourceColumn, now);
        }
        return taskQueryService.toResponse(task);
    }

    /**
     * 按状态枚举声明顺序锁住涉及的列，并返回加锁后的整列快照。
     *
     * <p>顺序固定为枚举 ordinal，而不是「先源列再目标列」或「先目标列再源列」：
     * 若按请求方向取锁，A 移动 TODO→DOING 与 B 移动 DOING→TODO 会以相反顺序申请同一对行锁，
     * 互相等待后由 InnoDB 回滚一方并抛出死锁异常，表现为偶发 500。</p>
     *
     * @param statuses 本次移动涉及的列，可重复（同列移动只锁一次）
     * @return 状态编码 → 该列任务列表（已按 sort_order、id 升序，且持有行锁）
     */
    private Map<String, List<BoardTask>> lockColumns(TaskStatus... statuses) {
        Map<String, List<BoardTask>> locked = new LinkedHashMap<>();
        Arrays.stream(statuses)
                .distinct()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .forEach(status -> locked.put(status.getCode(),
                        boardTaskMapper.selectByStatusForUpdate(status.getCode())));
        return locked;
    }

    /**
     * 把一列任务的序号重写为 0..n-1 并落库。
     *
     * @param column 目标列任务，按期望顺序排列；空列表直接返回
     * @param now    统一写入的更新时刻
     */
    private void applySortOrders(List<BoardTask> column, LocalDateTime now) {
        if (column.isEmpty()) {
            return;
        }
        for (int index = 0; index < column.size(); index++) {
            BoardTask item = column.get(index);
            item.setSortOrder(index);
            item.setUpdatedAt(now);
        }
        boardTaskMapper.updateSortOrders(column, now);
    }

    /**
     * 把请求中的插入位置夹到合法区间。
     *
     * @param position 请求传入的 0 基下标，可为 null（表示列尾）
     * @param size     目标列（已摘除自身）的当前长度
     * @return 落在 [0, size] 内的合法下标
     */
    private int clampPosition(Integer position, int size) {
        if (position == null) {
            return size;
        }
        if (position < 0) {
            return 0;
        }
        return Math.min(position, size);
    }

    /**
     * 逻辑删除任务：仅置 deleted 标记，历史数据保留以便统计回溯。
     *
     * @param id 任务主键
     * @throws ApiException 任务不存在或已删除时抛出 TASK_NOT_FOUND（404）
     */
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        BoardTask task = requireTask(id);
        // deleteById 在 @TableLogic 下改写为 UPDATE deleted = 1，物理行不删除。
        boardTaskMapper.deleteById(task.getId());
    }

    /**
     * 读取任务并校验存在性。
     *
     * @param id 任务主键
     * @return 任务实体
     * @throws ApiException 任务不存在或已删除时抛出 TASK_NOT_FOUND（404）
     */
    private BoardTask requireTask(Long id) {
        BoardTask task = boardTaskMapper.selectById(id);
        if (task == null) {
            throw new ApiException("TASK_NOT_FOUND", HttpStatus.NOT_FOUND, "任务不存在或已被删除");
        }
        return task;
    }

    /**
     * 校验乐观锁版本。请求未带版本时跳过校验，兼容内部脚本与联调工具的直接调用。
     *
     * @param task    数据库中的当前任务
     * @param version 调用方读到的版本，可为 null
     * @throws ApiException 版本不一致时抛出 TASK_VERSION_CONFLICT（409）
     */
    private void requireVersion(BoardTask task, Integer version) {
        if (version != null && !version.equals(task.getVersion())) {
            throw versionConflict();
        }
    }

    /**
     * 构造版本冲突异常。
     *
     * @return 409 状态、TASK_VERSION_CONFLICT 错误码的业务异常
     */
    private ApiException versionConflict() {
        return new ApiException("TASK_VERSION_CONFLICT", HttpStatus.CONFLICT,
                "任务已被他人修改，请刷新后重试");
    }

    /**
     * 校验并归一化标题。
     *
     * @param raw 原始标题
     * @return 去除首尾空白后的标题
     * @throws ApiException 为空时 TASK_TITLE_REQUIRED，超长时 TASK_TITLE_TOO_LONG（均 400）
     */
    private String requireTitle(String raw) {
        String title = raw == null ? "" : raw.trim();
        if (title.isEmpty()) {
            throw new ApiException("TASK_TITLE_REQUIRED", HttpStatus.BAD_REQUEST, "任务标题不能为空");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new ApiException("TASK_TITLE_TOO_LONG", HttpStatus.BAD_REQUEST,
                    "任务标题不能超过 " + MAX_TITLE_LENGTH + " 个字符");
        }
        return title;
    }

    /**
     * 归一化可空文本字段：去首尾空白，空白串按未填写处理。
     *
     * @param raw      原始值
     * @param max      长度上限
     * @param errorCode 超长时的业务错误码
     * @param fieldName 字段中文名，用于拼装提示
     * @return 归一化后的值；原始值为空或纯空白时返回 null
     * @throws ApiException 超长时抛出 400
     */
    private String normalizeOptional(String raw, int max, String errorCode, String fieldName) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > max) {
            throw new ApiException(errorCode, HttpStatus.BAD_REQUEST,
                    fieldName + "不能超过 " + max + " 个字符");
        }
        return value;
    }

    /**
     * 校验截止日不得早于当天。截止日当天仍属有效期，故只拦截严格早于当天的情况。
     *
     * @param dueDate 截止日，可为 null
     * @throws ApiException 早于当天时抛出 TASK_DUE_DATE_INVALID（400）
     */
    private void requireDueDate(LocalDate dueDate) {
        if (dueDate != null && dueDate.isBefore(LocalDate.now(clock))) {
            throw new ApiException("TASK_DUE_DATE_INVALID", HttpStatus.BAD_REQUEST, "截止日期不能早于今天");
        }
    }

    /**
     * 解析并校验置空字段清单。
     *
     * <p>置空必须显式声明，避免把「字段缺省」误读为「清空」。清单中出现白名单之外的字段名时
     * 直接拒绝，防止调用方以为清空生效而实际被静默忽略。</p>
     *
     * @param fields 请求中的置空字段名列表，可为 null
     * @return 去重后的字段名集合；入参为空时返回空集合
     * @throws ApiException 字段名不在白名单内时抛出 TASK_CLEAR_FIELD_INVALID（400）
     */
    private Set<String> parseClearFields(List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String field : fields) {
            if (field == null || field.isBlank()) {
                continue;
            }
            String name = field.trim();
            if (!CLEARABLE_FIELDS.contains(name)) {
                throw new ApiException("TASK_CLEAR_FIELD_INVALID", HttpStatus.BAD_REQUEST,
                        "不支持置空的字段：" + name);
            }
            result.add(name);
        }
        return result;
    }

    /**
     * 解析优先级编码。
     *
     * @param code 优先级编码，可为空（按默认中优先级处理）
     * @return 优先级枚举
     * @throws ApiException 编码非法时抛出 TASK_PRIORITY_INVALID（400）
     */
    private TaskPriority parsePriority(String code) {
        try {
            return TaskPriority.of(code);
        } catch (IllegalArgumentException exception) {
            throw new ApiException("TASK_PRIORITY_INVALID", HttpStatus.BAD_REQUEST, "优先级取值不合法");
        }
    }

    /**
     * 解析状态编码。
     *
     * @param code 状态编码
     * @return 状态枚举
     * @throws ApiException 编码为空或非法时抛出 TASK_STATUS_INVALID（400）
     */
    private TaskStatus parseStatus(String code) {
        try {
            return TaskStatus.of(code);
        } catch (IllegalArgumentException exception) {
            throw new ApiException("TASK_STATUS_INVALID", HttpStatus.BAD_REQUEST, "目标状态取值不合法");
        }
    }
}
