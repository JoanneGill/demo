package com.example.demo.Data;

import lombok.Data;

import java.math.BigInteger;
import java.util.Date;

@Data
public class PpTask {

    // 主键ID
    private BigInteger id;

    // 直播间地址
    private String roomAddress;

    // 房间ID
    private String roomId;

    // 总数量
    private Integer totalNumber;

    // 已完成数量
    private Integer completedNumber;

    // 已领取数量
    private Integer receivedNumber;

    // 直播间人员地址
    private String personAddress;

    // 直播间人员姓名
    private String personName;

    // 积分
    private Integer integral;

    // 开始时间
    private Date beginTime;

    // 完成时间
    private Date completedTime;

    // ========== 管理端需要的字段 ==========


      // 状态     public static final String PP_TASK_CLAIM_STATUS_CLAIMED = "claimed";    // 任务中
     //    public static final String PP_TASK_CLAIM_STATUS_SUCCESS = "success";   // 成功
   //    public static final String PP_TASK_CLAIM_STATUS_FAILED = "fail";      // 失败
     //    public static final String PP_TASK_CLAIM_STATUS_EXPIRED = "expired";  //超时
    private String status;

    // 创建时间
    private Date createTime;

    // 分页参数
    private Integer page;

    private Integer size;

    private Integer pageSize;
}
