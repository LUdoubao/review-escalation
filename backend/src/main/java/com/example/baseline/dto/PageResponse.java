package com.example.baseline.dto;

import java.util.List;

/**
 * 通用分页出参，列表视图与后续任何分页接口共用。
 *
 * <p>领域：跨栈分页契约；关系：被 {@code TaskQueryService#pageTasks} 构造，
 * 由 frontend/src/api/tasks.js 消费并驱动分页器。</p>
 *
 * @param items      当前页数据，可能为空列表；不为 null
 * @param totalItems 符合筛选条件的总条数
 * @param page       当前页码，从 1 开始
 * @param size       每页条数，已按服务端上限截断后的实际值
 * @param totalPages 总页数；totalItems 为 0 时为 0
 * @param <T>        列表元素类型
 */
public record PageResponse<T>(
        List<T> items,
        long totalItems,
        int page,
        int size,
        int totalPages) {
}
