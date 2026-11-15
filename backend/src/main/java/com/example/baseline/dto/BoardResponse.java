package com.example.baseline.dto;

import java.util.List;

/**
 * 看板聚合出参，一次请求返回全部列，前端单次渲染即可完成整块看板。
 *
 * <p>领域：任务看板聚合契约；关系：{@code GET /api/tasks/board} 的响应体。
 * 顺序：{@code columns} 的顺序即列渲染顺序，由后端按状态枚举声明顺序输出。</p>
 *
 * @param columns     看板列数据
 * @param generatedAt 聚合时刻，ISO-8601 字符串（服务器本地时区），供前端展示刷新时间
 */
public record BoardResponse(List<BoardColumnResponse> columns, String generatedAt) {
}
