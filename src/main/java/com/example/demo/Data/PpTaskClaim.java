package com.example.demo.Data;

import lombok.Data;

import java.math.BigInteger;
import java.util.Date;

@Data
public class PpTaskClaim  {

    // 主键ID
    private BigInteger id;

    // 任务ID
    private BigInteger taskId;

    // 设备ID
    private String deviceId;

    // 设备昵称
    private String deviceNickName;

    // 领取时间
    private Date claimTime;

    // 完成时间
    private Date finishTime;

    // 租约过期时间
    private Date leaseExpireTime;

    // 状态     public static final String PP_TASK_CLAIM_STATUS_CLAIMED = "claimed";    // 任务中
    //    public static final String PP_TASK_CLAIM_STATUS_SUCCESS = "success";   // 成功
    //    public static final String PP_TASK_CLAIM_STATUS_FAILED = "fail";      // 失败
    //    public static final String PP_TASK_CLAIM_STATUS_EXPIRED = "expired";  //超时
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
    private Date beginTime;

    // 完成时间
    private Date completedTime;

    // 全部信息toString
    private String ppTask;
}
