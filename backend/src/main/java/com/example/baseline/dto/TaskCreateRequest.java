package com.example.baseline.dto;

import java.time.LocalDate;

/**
 * 新建任务的请求体，字段全部由调用方提供，服务端在 service 内逐项校验。
 *
 * <p>领域：任务看板命令侧；关系：{@code POST /api/tasks} 的入参，被
 * {@code TaskBoardService#create} 消费后转为 {@code BoardTask} 落库。
 * 不变量：{@code title} 去空白后非空且不超过 80 字符；{@code priority} 为空时按 MEDIUM 处理。</p>
 *
 * @param title       任务标题，必填
 * @param description 任务描述，可空，上限 500 字符
 * @param assignee    负责人显示名，可空，上限 40 字符
 * @param priority    优先级编码，可空，默认 MEDIUM
 * @param tag         标签，可空，上限 24 字符
 * @param dueDate     截止日，可空；非空时不得早于服务器当天
 */
public record TaskCreateRequest(
        String title,
        String description,
        String assignee,
        String priority,
        String tag,
        LocalDate dueDate) {
}
