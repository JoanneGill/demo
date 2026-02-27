package com.example.demo.Data;

import lombok.Data;

import java.util.Date;

@Data
public class PpTaskClaim {

    private Long id;

    private Long taskId;

    private String deviceId;

    private String deviceNickName;

    private String status;

    private Date claimTime;

    private Date finishTime;

    private Date leaseExpireTime;

}
