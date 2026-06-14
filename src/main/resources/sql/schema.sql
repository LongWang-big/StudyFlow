CREATE DATABASE IF NOT EXISTS studyflow DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE studyflow;

CREATE TABLE IF NOT EXISTS task (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    title           VARCHAR(100) NOT NULL COMMENT '任务标题',
    description     TEXT                  DEFAULT NULL COMMENT '任务描述',
    priority        VARCHAR(20)  NOT NULL COMMENT '优先级: HIGH, MEDIUM, LOW',
    status          VARCHAR(20)  NOT NULL DEFAULT 'TODO' COMMENT '状态: TODO, IN_PROGRESS, COMPLETED',
    deadline        DATETIME              DEFAULT NULL COMMENT '截止时间',
    reminder_policy VARCHAR(50)           DEFAULT NULL COMMENT '提醒策略',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习任务表';

CREATE TABLE IF NOT EXISTS study_time_record (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    task_id           BIGINT       NOT NULL COMMENT '关联任务 id',
    start_time        DATETIME     NOT NULL COMMENT '开始时间',
    end_time          DATETIME              DEFAULT NULL COMMENT '结束时间（未结束时为 NULL）',
    duration_minutes  DOUBLE                DEFAULT NULL COMMENT '学习时长（分钟，精确到小数点后 1 位）',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    PRIMARY KEY (id),
    KEY idx_task_id (task_id),
    CONSTRAINT fk_study_time_task FOREIGN KEY (task_id) REFERENCES task (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习时长记录表';
