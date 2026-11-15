package com.example.baseline.domain;

/**
 * 任务优先级定义，用于看板卡片标识与列表视图的排序权重。
 *
 * <p>领域：任务看板；关系：被 {@link BoardTask} 以编码形式持久化，被 service 用于入参校验，
 * 被前端渲染为卡片角标。权重仅用于展示排序提示，不参与状态机判断。
 * 生命周期：枚举常量不可变、线程安全。</p>
 */
public enum TaskPriority {

    /** 低：可延后处理。 */
    LOW("低", 1),

    /** 中：默认优先级，新建任务未显式指定时落此档。 */
    MEDIUM("中", 2),

    /** 高：需要在当轮迭代内处理。 */
    HIGH("高", 3),

    /** 紧急：阻塞他人工作，需要立即处理。 */
    URGENT("紧急", 4);

    /** 中文显示名，供前端渲染。 */
    private final String label;

    /** 权重，数值越大越紧急；仅用于展示层的比较与排序提示。 */
    private final int weight;

    TaskPriority(String label, int weight) {
        this.label = label;
        this.weight = weight;
    }

    public String getCode() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public int getWeight() {
        return weight;
    }

    /**
     * 按编码解析优先级，用于校验来自请求体的字符串。
     *
     * @param code 优先级编码，为空时返回默认的 {@link #MEDIUM}
     * @return 命中的优先级枚举
     * @throws IllegalArgumentException 编码非空且不在枚举范围内
     */
    public static TaskPriority of(String code) {
        if (code == null || code.isBlank()) {
            return MEDIUM;
        }
        return TaskPriority.valueOf(code.trim().toUpperCase());
    }
}
