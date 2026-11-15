package com.example.baseline.dto;

/**
 * 移动任务的请求体：目标列 + 列内插入位置。
 *
 * <p>领域：任务看板命令侧；关系：{@code PATCH /api/tasks/{id}/move} 的入参。
 * 位置语义：{@code position} 为目标列内的目标下标，从 0 开始；为空表示追加到列尾。
 * 并发：{@code version} 与编辑接口同义，用于检测并发覆盖。</p>
 *
 * @param targetStatus 目标状态编码，必填，取值见 TaskStatus
 * @param position     目标列内下标，可空；为空时追加到列尾
 * @param version      乐观锁版本，可空
 */
public record TaskMoveRequest(
        String targetStatus,
        Integer position,
        Integer version) {
}
