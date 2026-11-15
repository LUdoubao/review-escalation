package com.example.baseline.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务的统一出参，看板、列表与详情三个入口共用同一形状，避免前端维护多套字段映射。
 *
 * <p>领域：任务看板跨栈契约；关系：由 service 从 {@code BoardTask} 装配，被
 * frontend/src/api/tasks.js 消费、由前端卡片与表格渲染。
 * 不可变性：record 只读，单次请求内构造一次。</p>
 *
 * <p>{@code status} 与 {@code priority} 同时给出编码与中文显示名：编码供前端做逻辑判断，
 * 显示名供直接渲染，前端不需要内置枚举字典。</p>
 *
 * @param id            任务主键
 * @param title         标题
 * @param description   描述，可能为 null
 * @param assignee      负责人，可能为 null（未指派）
 * @param priority      优先级编码，取值见 TaskPriority
 * @param priorityLabel 优先级中文名
 * @param status        状态编码，取值见 TaskStatus
 * @param statusLabel   状态中文名
 * @param terminal      是否为终态：true 表示已完成、不可再移动，前端据此禁用移动入口
 * @param tag           标签，可能为 null
 * @param dueDate       截止日，可能为 null；本地日期，不带时区偏移
 * @param sortOrder     列内序号，从 0 起
 * @param overdue       是否已逾期：有截止日、已过截止日且未完成
 * @param version       乐观锁版本，编辑与移动时需回传
 * @param updatedAt     最近更新时刻，服务器本地时钟
 */
public record TaskResponse(
        Long id,
        String title,
        String description,
        String assignee,
        String priority,
        String priorityLabel,
        String status,
        String statusLabel,
        boolean terminal,
        String tag,
        LocalDate dueDate,
        Integer sortOrder,
        boolean overdue,
        Integer version,
        LocalDateTime updatedAt) {
}
