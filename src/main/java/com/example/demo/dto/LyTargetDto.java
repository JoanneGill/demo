package com.example.demo.dto;

import lombok.Data;

import java.util.List;

@Data
public class LyTargetDto {
    private String key;
    private String address;
    private String appId;
    private String appSecret;
    private List<LyGoodsDto> goods;
}

