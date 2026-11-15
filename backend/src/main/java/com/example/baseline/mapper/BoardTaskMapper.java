package com.example.baseline.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.baseline.domain.BoardTask;
import com.example.baseline.dto.TaskQuery;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 看板任务的数据访问接口，继承 BaseMapper 获得按主键读写、逻辑删除与乐观锁更新能力。
 *
 * <p>领域：任务看板；关系：被 {@code TaskQueryService} 与 {@code TaskBoardService} 调用，
 * SQL 实现全部位于 resources/mapper/BoardTaskMapper.xml。
 * 约定：所有自定义查询在 SQL 中显式声明 deleted = 0，与 @TableLogic 的隐式过滤保持一致。
 * 线程：MyBatis 的 Mapper 接口由容器代理为无状态对象，可并发调用。</p>
 */
public interface BoardTaskMapper extends BaseMapper<BoardTask> {

    /**
     * 按筛选条件分页查询任务。
     *
     * @param query  筛选条件，status/priority/assignee/keyword 为空表示不限
     * @param offset 跳过行数，由 service 按 (页码 - 1) * 每页条数算出，非负
     * @param limit  本页最多返回行数
     * @return 本页任务，按状态列顺序、列内序号、主键升序排列；无命中返回空列表
     */
    List<BoardTask> selectPageList(@Param("q") TaskQuery query,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    /**
     * 统计符合筛选条件的任务总数，供分页器计算总页数。
     *
     * @param query 筛选条件
     * @return 命中行数，无命中返回 0
     */
    long countByQuery(@Param("q") TaskQuery query);

    /**
     * 查询看板候选集：每个状态列只取前 limit 条，截断在数据库侧完成。
     *
     * @param query 筛选条件
     * @param limit 每列最多返回的行数
     * @return 命中任务，按状态列顺序、列内序号升序排列
     */
    List<BoardTask> selectBoardPage(@Param("q") TaskQuery query, @Param("limit") int limit);

    /**
     * 统计各状态下的命中条数，供列头「共 N 条」使用。
     *
     * @param query 筛选条件
     * @return 每行为 { status, total }；某状态无命中时该行不存在
     */
    List<Map<String, Object>> selectColumnTotals(@Param("q") TaskQuery query);

    /**
     * 统计未删除任务总数。
     *
     * @return 任务总数
     */
    long countAll();

    /**
     * 统计指定状态的任务数。
     *
     * @param status 状态编码，取值见 TaskStatus
     * @return 该状态下的任务数
     */
    long countByStatus(@Param("status") String status);

    /**
     * 统计逾期任务数：有截止日、已过截止日且未完成。
     *
     * @param today 业务时区的当天日期，由 service 计算后传入，避免依赖数据库会话时区
     * @return 逾期任务数
     */
    long countOverdue(@Param("today") LocalDate today);

    /**
     * 取负责人候选列表，去重、去空并按名称升序。
     *
     * @return 负责人名称列表，可能为空
     */
    List<String> selectAssignees();

    /**
     * 取指定状态列内的最大序号。
     *
     * @param status 状态编码
     * @return 列内最大序号；该列为空时返回 null
     */
    Integer selectMaxSortOrder(@Param("status") String status);

    /**
     * 锁住指定状态列的全部行，用于新建任务前串行化「读最大序号 + 写入」。
     *
     * @param status 状态编码
     * @return 该列的主键列表；空列返回空列表（此时无行可锁）
     */
    List<Long> lockColumn(@Param("status") String status);

    /**
     * 取指定状态列的全部任务并加行锁，用于移动时计算插入位置与重排序号。
     *
     * <p>跨列移动会同时锁源列与目标列，调用方必须按状态枚举顺序依次加锁，
     * 否则反向并发移动会以相反顺序申请同一批行锁而死锁。</p>
     *
     * @param status 状态编码
     * @return 该列任务列表，按列内序号升序，可能为空
     */
    List<BoardTask> selectByStatusForUpdate(@Param("status") String status);

    /**
     * 批量改写列内序号：一条语句写完一整列的 0..n-1 序号。
     *
     * @param items     待改写任务，每项的 id 与 sortOrder 必填
     * @param updatedAt 统一写入的更新时刻
     * @return 实际更新行数
     */
    int updateSortOrders(@Param("items") List<BoardTask> items, @Param("updatedAt") LocalDateTime updatedAt);
}
