-- pptask table
CREATE TABLE IF NOT EXISTS `pptask` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT,
    `room_id`      VARCHAR(64)   DEFAULT NULL,
    `person_name`  VARCHAR(255)  DEFAULT NULL,
    `title`        VARCHAR(255)  DEFAULT NULL,
    `number`       INT           DEFAULT 0,
    `number_left`  INT           DEFAULT 0,
    `integral`     INT           DEFAULT 0,
    `status`       INT           DEFAULT 1,
    `create_time`  DATETIME      DEFAULT NULL,
    `expire_time`  DATETIME      DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Migration: add admin columns to existing pptask table (safe to run on existing installs)
ALTER TABLE `pptask` ADD COLUMN IF NOT EXISTS `person_name`  VARCHAR(255) DEFAULT NULL;
ALTER TABLE `pptask` ADD COLUMN IF NOT EXISTS `title`        VARCHAR(255) DEFAULT NULL;
ALTER TABLE `pptask` ADD COLUMN IF NOT EXISTS `number`       INT          DEFAULT 0;
ALTER TABLE `pptask` ADD COLUMN IF NOT EXISTS `number_left`  INT          DEFAULT 0;
ALTER TABLE `pptask` ADD COLUMN IF NOT EXISTS `integral`     INT          DEFAULT 0;
ALTER TABLE `pptask` ADD COLUMN IF NOT EXISTS `status`       INT          DEFAULT 1;
ALTER TABLE `pptask` ADD COLUMN IF NOT EXISTS `create_time`  DATETIME     DEFAULT NULL;
ALTER TABLE `pptask` ADD COLUMN IF NOT EXISTS `expire_time`  DATETIME     DEFAULT NULL;

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
