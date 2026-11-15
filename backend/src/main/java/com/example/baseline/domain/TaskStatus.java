package com.example.baseline.domain;

import java.util.List;

/**
 * 看板任务的状态列定义，是「列即状态」这一业务语义的唯一来源。
 *
 * <p>领域：任务看板；关系：被 {@link BoardTask} 以编码形式持久化，被查询与命令两侧 service
 * 用于校验目标状态，被前端按 {@link #getLabel()} 渲染列标题。
 * 顺序：声明顺序即看板列从左到右的渲染顺序，接口按此顺序输出列数组，前端不再自行排序。
 * 生命周期：枚举常量在类加载期创建，不可变、线程安全，可直接跨请求共享。</p>
 *
 * <p>编码一经上线即视为持久化契约的一部分：编码写入数据库 status 列，修改编码等同数据迁移。</p>
 */
public enum TaskStatus {

    /** 待办：任务刚创建或从其他列回退到此，尚未开始处理。 */
    TODO("待办", false),

    /** 进行中：已有人认领并开始处理。 */
    DOING("进行中", false),

    /** 待验收：处理完成，等待提交人确认。 */
    REVIEW("待验收", false),

    /** 已完成：验收通过，是流程的终态，不可再流转到其他列。 */
    DONE("已完成", true);

    /** 中文显示名，仅用于前端渲染列标题，不参与任何判断逻辑。 */
    private final String label;

    /**
     * 是否为终态。
     *
     * <p>终态一经到达即锁定，不再接受流转：否则已完成任务可以被任意移回待办，
     * 使完成率、各列计数与逾期统计同时回退，看板失去「进度」含义。
     * 该标记是状态机判断的唯一来源，前端也据此禁用移动入口（经 TaskResponse.terminal 下发）。</p>
     */
    private final boolean terminal;

    TaskStatus(String label, boolean terminal) {
        this.label = label;
        this.terminal = terminal;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    /**
     * 是否为流程终态。
     *
     * @return 终态返回 true；处于流程中的状态返回 false
     */
    public boolean isTerminal() {
        return terminal;
    }

    /**
     * 返回看板列的固定顺序，顺序与枚举声明一致。
     *
     * @return 不可变的状态列表，长度固定为 4；调用方不应修改
     */
    public static List<TaskStatus> ordered() {
        return List.of(TODO, DOING, REVIEW, DONE);
    }

    /**
     * 按编码解析状态，用于校验来自请求体的字符串。
     *
     * @param code 状态编码，允许传入首尾空白但区分大小写以外的大小写差异由调用方归一化
     * @return 命中的状态枚举
     * @throws IllegalArgumentException 编码为空或不在枚举范围内
     */
    public static TaskStatus of(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("状态编码不能为空");
        }
        return TaskStatus.valueOf(code.trim().toUpperCase());
    }
}
