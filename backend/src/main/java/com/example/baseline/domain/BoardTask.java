package com.example.baseline.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 看板任务的持久化实体，对应 board_task 表。
 *
 * <p>领域：任务看板；关系：由 {@code BoardTaskMapper} 读写，由 service 组装为
 * {@code TaskResponse} 后返回前端；不直接出现在 HTTP 出参中，避免表结构泄漏到跨栈契约。
 * 事务/线程：每次请求独立创建实例，不在线程间共享；写操作受 {@link #version} 乐观锁保护。
 * 生命周期：随请求创建与丢弃，无状态缓存。</p>
 *
 * <p>状态与优先级以字符串编码落库（{@link TaskStatus} / {@link TaskPriority} 的 name()），
 * 解析与校验全部在 service 完成，实体本身不带业务分支。</p>
 */
@TableName("board_task")
public class BoardTask {

    /** 主键，数据库自增；新建时由 MyBatis-Plus 回填。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题，去首尾空白后非空，长度上限 80，是列表与看板卡片的主要辨识信息。 */
    private String title;

    /**
     * 描述，可为空，长度上限 500。
     *
     * <p>更新策略取 ALWAYS：置空时必须把 null 真正写进列，默认的 NOT_NULL 会把 null 字段从 SET 子句剔除，
     * 让「清空」退化为「不修改」。写操作前实体一律先按主键读出，因此不会误伤本次未涉及的字段。</p>
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    /**
     * 负责人显示名，可为空（未指派），长度上限 40；空串在写库前统一归一为 null。
     *
     * <p>更新策略取 ALWAYS，理由同 {@link #description}：允许把负责人改回未指派。</p>
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String assignee;

    /** 优先级编码，取值见 {@link TaskPriority}；非空，默认 MEDIUM。 */
    private String priority;

    /** 状态编码，取值见 {@link TaskStatus}；非空，默认 TODO。 */
    private String status;

    /**
     * 标签，可为空，长度上限 24；参与关键字检索。
     *
     * <p>更新策略取 ALWAYS，理由同 {@link #description}：允许把标签清空。</p>
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String tag;

    /**
     * 截止日（本地日期），可为空。仅比较日期，不带时区偏移，避免跨时区部署时日期漂移。
     *
     * <p>更新策略取 ALWAYS，理由同 {@link #description}：允许把截止日清空。</p>
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate dueDate;

    /** 列内序号，从 0 起升序；同一状态内唯一性由业务保证，数据库不设唯一约束以便批量顺延。 */
    private Integer sortOrder;

    /** 乐观锁版本，初始 1，每次成功更新由拦截器自增；用于检测并发覆盖。 */
    @Version
    private Integer version;

    /** 创建时刻，服务器本地时钟（Asia/Shanghai），写入后不再变更。 */
    private LocalDateTime createdAt;

    /** 最近更新时刻，服务器本地时钟；任何字段变更或状态流转都必须同步刷新。 */
    private LocalDateTime updatedAt;

    /** 逻辑删除标记：0 未删除、1 已删除；查询由 @TableLogic 自动过滤，物理删除不执行。 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
