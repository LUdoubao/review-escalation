package com.example.baseline.dto;

import java.util.List;

/**
 * 看板单列出参：列标识 + 该列任务。
 *
 * <p>领域：任务看板聚合契约；关系：由 {@code TaskQueryService#board} 装配，
 * 前端按数组顺序渲染列，不再依赖本地枚举字典。</p>
 *
 * @param status 状态编码，取值见 TaskStatus
 * @param label  列标题中文名
 * @param total  该列任务总数（未截断），用于「显示 N 条中的前 M 条」提示
 * @param tasks  该列当前展示的任务，最多 8 条，按列内序号升序
 */
public record BoardColumnResponse(
        String status,
        String label,
        long total,
        List<TaskResponse> tasks) {
}
