package com.example.baseline.dto;

/**
 * 任务列表与看板共用的查询条件载体，由 Spring MVC 从查询串绑定。
 *
 * <p>领域：任务看板查询；关系：作为 {@code TaskQueryService} 的入参，也是
 * {@code BoardTaskMapper} 自定义 SQL 的条件来源。
 * 约束：本类只承载数据，不做归一化与校验；页码与每页条数的边界处理在 service 完成。</p>
 */
public class TaskQuery {

    /** 页码，从 1 开始；未传时默认第 1 页。 */
    private int page = 1;

    /** 每页条数；未传时默认 5，服务端会将上限截断到 50。 */
    private int size = 5;

    /** 状态筛选，取值见 TaskStatus；空表示不限。 */
    private String status;

    /** 优先级筛选，取值见 TaskPriority；空表示不限。 */
    private String priority;

    /** 负责人筛选，按名称精确匹配；空表示不限。 */
    private String assignee;

    /** 关键字，按标题或标签模糊匹配；空表示不限。 */
    private String keyword;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
