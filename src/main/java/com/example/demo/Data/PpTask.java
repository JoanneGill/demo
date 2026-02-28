package com.example.demo.Data;

import lombok.Data;

import java.math.BigInteger;
import java.util.Date;

@Data
public class PpTask {

    private BigInteger id;

    private String roomAddress;

    private String roomId;

    private Integer totalNumber;

    private Integer compatedNumber;

    private String personAddress;

    private String personName;

    private String videoName;

    private Integer integral;

    private Date beginTime;

    private Date dieTime;

}
