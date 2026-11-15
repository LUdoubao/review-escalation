package com.example.baseline.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 编辑任务的请求体，采用「字段缺省表示不修改、显式声明表示置空」的三态语义。
 *
 * <p>领域：任务看板命令侧；关系：{@code PATCH /api/tasks/{id}} 的入参。
 * 并发：{@code version} 为前端读到的任务版本，与服务端当前版本不一致时返回 409，
 * 避免两人同时编辑时后写覆盖先写。</p>
 *
 * <p>三态说明：<br>
 * ① 字段为 {@code null} 且未出现在 {@code clearFields} 中 —— 不修改该字段；<br>
 * ② 字段有值 —— 更新为该值；<br>
 * ③ 字段名出现在 {@code clearFields} 中 —— 强制置空，忽略同名字段的值。<br>
 * 把「置空」独立成清单，是因为单个 {@code null} 无法同时表达「不修改」与「清空」，
 * 混用会让调用方无法把已填写的负责人、描述、标签或截止日改回空。</p>
 *
 * @param title       新标题，null 表示不修改
 * @param description 新描述，null 表示不修改；空白串按未填写处理
 * @param assignee    新负责人，null 表示不修改；空白串按未填写处理
 * @param priority    新优先级，null 表示不修改
 * @param tag         新标签，null 表示不修改；空白串按未填写处理
 * @param dueDate     新截止日，null 表示不修改；仅在日期真正变化时校验「不得早于今天」
 * @param clearFields 需要置空的字段名清单，取值限 description / assignee / tag / dueDate
 * @param version     乐观锁版本，可空；为空时跳过版本校验（仅用于内部脚本调用）
 */
public record TaskUpdateRequest(
        String title,
        String description,
        String assignee,
        String priority,
        String tag,
        LocalDate dueDate,
        List<String> clearFields,
        Integer version) {
}
