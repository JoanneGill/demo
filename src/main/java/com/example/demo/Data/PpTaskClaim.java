package com.example.demo.Data;

import lombok.Data;

import java.math.BigInteger;
import java.util.Date;

@Data
public class PpTaskClaim  {

    private BigInteger id;

    private BigInteger taskId;

    private String deviceId;

    private String deviceNickName;

    private Date leaseExpireTime;

    private String status;

    private String msg;

    private String videoName;

    private String roomId;

    private String personAddress;

    private Integer integral;

    private Date beginTime;

    private Date completedTime;

    private String ppTask; // 全部信息toString

}
