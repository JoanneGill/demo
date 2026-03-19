package com.example.demo.Data;


import lombok.Data;

@Data
public class DeviceData {
    private String id; //设备当前任务id
    private String cardNo;
    private String deviceId;
    private String deviceNickName;
    private String roomId;
    private String ip;
    private String personName;
    private String screenImgUrl;
    private Integer todayTaskNumber;//今日领取任务数
    private Long todayTaskIntegral;//今日产生有效积分
    private Long state; // 最近一次在线时间 时间戳
    private Long ppClaimTime; //
    private String ppClaimState; //
    private String ppModel;
    private Long haveWorkTime; // 领取任务 时间戳
    private Long startWorkingState;  // 最近开始任务时间戳
    private Long lastWorkingState;  // 最后确认任务时间戳
    private Long duration;  // 有效任务时长
}
