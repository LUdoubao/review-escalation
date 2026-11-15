package com.example.baseline.dto;

/**
 * 看板汇总出参，用于页面顶部的指标条。
 *
 * <p>领域：任务看板统计；关系：{@code GET /api/tasks/summary} 的响应体。
 * 口径：{@code overdueCount} 只统计未完成任务中已过截止日的数量；
 * {@code completionRate} 为 0-100 的整数百分比，四舍五入到个位。</p>
 *
 * @param total          未删除任务总数
 * @param todoCount      待办列任务数
 * @param doingCount     进行中列任务数
 * @param reviewCount    待验收列任务数
 * @param doneCount      已完成列任务数
 * @param overdueCount   逾期任务数
 * @param completionRate 完成率百分比，0-100 的整数
 */
public record BoardSummaryResponse(
        long total,
        long todoCount,
        long doingCount,
        long reviewCount,
        long doneCount,
        long overdueCount,
        int completionRate) {
}
