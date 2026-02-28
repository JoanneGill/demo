package com.example.demo.Data;

import lombok.Data;

import java.math.BigInteger;
import java.util.Date;

@Data
public class PpTaskClaim {

    private BigInteger id;

    private BigInteger taskId;

    private String deviceId;

    private String deviceNickName;

    private Date leaseExpireTime;

}
