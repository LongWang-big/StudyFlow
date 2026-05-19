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
