-- 任务看板表结构。脚本以 IF NOT EXISTS 幂等执行，应用每次启动都会跑一遍，
-- 因此这里的定义必须与 BoardTask 实体、TaskStatus / TaskPriority 枚举保持同源。
CREATE TABLE IF NOT EXISTS board_task (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务主键',
    title       VARCHAR(80)  NOT NULL               COMMENT '任务标题，去首尾空白后非空',
    description VARCHAR(500) DEFAULT NULL           COMMENT '任务描述',
    assignee    VARCHAR(40)  DEFAULT NULL           COMMENT '负责人显示名，NULL 表示未指派',
    priority    VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM' COMMENT '优先级：LOW / MEDIUM / HIGH / URGENT',
    status      VARCHAR(16)  NOT NULL DEFAULT 'TODO'   COMMENT '状态：TODO / DOING / REVIEW / DONE',
    tag         VARCHAR(24)  DEFAULT NULL           COMMENT '标签，参与关键字检索',
    due_date    DATE         DEFAULT NULL           COMMENT '截止日（本地日期，不含时区偏移）',
    sort_order  INT          NOT NULL DEFAULT 0     COMMENT '列内序号，从 0 起升序',
    version     INT          NOT NULL DEFAULT 1     COMMENT '乐观锁版本，每次成功更新 +1',
    created_at  DATETIME     NOT NULL               COMMENT '创建时刻（服务器本地时钟）',
    updated_at  DATETIME     NOT NULL               COMMENT '最近更新时刻（服务器本地时钟）',
    deleted     TINYINT      NOT NULL DEFAULT 0     COMMENT '逻辑删除标记：0 未删除，1 已删除',
    PRIMARY KEY (id),
    -- 看板按状态取列、列内按序号排序，联合索引让列查询走一次索引扫描。
    KEY idx_status_sort (status, sort_order),
    KEY idx_assignee (assignee),
    KEY idx_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务看板任务表';
