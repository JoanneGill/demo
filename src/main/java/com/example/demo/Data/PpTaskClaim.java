package com.example.demo.Data;

import lombok.Data;

import java.math.BigInteger;

@Data
public class PpTaskClaim {
    private BigInteger id;
    private BigInteger taskId;
    private String cardNo;
    private String status;
    private String claimTime;
    private String finishTime;
}
