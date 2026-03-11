CREATE TABLE pp_task_history (
    id              BIGINT          NOT NULL,
    room_address    VARCHAR(500)    DEFAULT NULL,
    room_id         VARCHAR(100)    DEFAULT NULL,
    total_number    INT             DEFAULT 0,
    completed_number INT            DEFAULT 0,
    received_number INT             DEFAULT 0,
    person_address  VARCHAR(500)    DEFAULT NULL,
    person_name     VARCHAR(200)    DEFAULT NULL,
    integral        INT             DEFAULT 0,
    begin_time      VARCHAR(50)     DEFAULT NULL,
    completed_time  VARCHAR(50)     DEFAULT NULL,
    status          VARCHAR(20)     DEFAULT NULL,
    create_time     DATETIME        DEFAULT NULL,
    archived_time   DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PP任务历史归档表';

CREATE TABLE pp_task_claim_history (
    id               BIGINT          NOT NULL,
    task_id          BIGINT          DEFAULT NULL,
    device_id        VARCHAR(200)    DEFAULT NULL,
    device_nick_name VARCHAR(200)    DEFAULT NULL,
    lease_expire_time VARCHAR(50)    DEFAULT NULL,
    status           VARCHAR(20)     DEFAULT NULL,
    msg              VARCHAR(1000)   DEFAULT NULL,
    video_name       VARCHAR(200)    DEFAULT NULL,
    room_id          VARCHAR(100)    DEFAULT NULL,
    person_address   VARCHAR(500)    DEFAULT NULL,
    integral         INT             DEFAULT NULL,
    begin_time       VARCHAR(50)     DEFAULT NULL,
    completed_time   VARCHAR(50)     DEFAULT NULL,
    archived_time    DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
    PRIMARY KEY (id),
    KEY idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PP任务领取记录历史归档表';
