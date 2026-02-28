package com.example.demo.Data;

import lombok.Data;

import java.math.BigInteger;

@Data
public class PpTask {
    private BigInteger id;
    private String roomId;
    private String personName;
    private String title;
    private Integer number;
    private Integer numberLeft;
    private Integer integral;
    private Integer status;
    private String createTime;
    private String expireTime;

    // pagination params
    private Integer page;
    private Integer size;

    public Integer getPageSize() {
        if (page == null || size == null || size <= 0 || page <= 0) {
            return null;
        }
        return (page - 1) * size;
    }
}
