package com.example.demo.common;

import java.util.Locale;

/**
 * Dy 后台任务类型：与 YL 配置里 {@code taskType} 对应。
 * <ul>
 *   <li>{@link #RQ} — 默认，原「入房/时长」类任务</li>
 *   <li>{@link #PP} — 新 pp 任务，走另一套下发与设备统计接口</li>
 * </ul>
 */
public enum DyTaskType {

    RQ("rq"),
    PP("pp");

    private final String code;

    DyTaskType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 从 config 读取，空或未知一律按 {@link #RQ}。
     */
    public static DyTaskType fromConfig(String raw) {
        if (raw == null) {
            return RQ;
        }
        String t = raw.trim().toLowerCase(Locale.ROOT);
        if ("pp".equals(t)) {
            return PP;
        }
        return RQ;
    }
}
