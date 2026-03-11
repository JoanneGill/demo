package com.example.demo.Data;

import lombok.Data;

import java.math.BigInteger;
import java.util.Date;

@Data
public class PpTaskClaimHistory {

    // 主键ID
    private BigInteger id;

    // 任务ID
    private BigInteger taskId;

    // 设备ID
    private String deviceId;

    // 设备昵称
    private String deviceNickName;

    // 租约过期时间
    private String leaseExpireTime;

    // 状态
    private String status;

    // 消息
    private String msg;

    // 视频名称
    private String videoName;

    // 房间ID
    private String roomId;

    // 人员地址
    private String personAddress;

    // 积分
    private Integer integral;

    // 开始时间
    private String beginTime;

    // 完成时间
    private String completedTime;

    // 归档时间
    private Date archivedTime;
}
