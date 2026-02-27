-- pptask table
CREATE TABLE IF NOT EXISTS `pptask` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `room_id`          VARCHAR(64)  DEFAULT NULL,
    `total_number`     INT          DEFAULT 0,
    `compated_number`  INT          DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- pptask_claim table
CREATE TABLE IF NOT EXISTS `pptask_claim` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `task_id`          BIGINT       NOT NULL,
    `device_id`        VARCHAR(128) NOT NULL,
    `device_nick_name` VARCHAR(255) DEFAULT NULL,
    `status`           VARCHAR(16)  NOT NULL DEFAULT 'CLAIMED',
    `claim_time`       DATETIME     NOT NULL,
    `finish_time`      DATETIME     DEFAULT NULL,
    `lease_expire_time` DATETIME    NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_device` (`task_id`, `device_id`),
    KEY `idx_task_status_lease` (`task_id`, `status`, `lease_expire_time`),
    KEY `idx_status_lease` (`status`, `lease_expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
