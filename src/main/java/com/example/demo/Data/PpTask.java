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

    private Integer completedNumber;

    private Integer receivedNumber;

    private String personAddress;

    private String personName;

    private Integer integral;

    private Date beginTime;

    private Date completedTime;

    // 管理端需要的字段
    private String title;

    private Integer number;

    private Integer numberLeft;

    private Integer status;

    private String createTime;

    private String expireTime;

    // 分页参数
    private Integer page;

    private Integer size;

    private Integer pageSize;

}
