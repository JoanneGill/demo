package com.example.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class LyGoodsDto {
    private Integer id;
    /**
     * rq / pp / future扩展值；允许为空，后端按默认 rq 处理
     */
    private String dyPool;
    /**
     * 长度 24，可为 null；元素允许为 null
     */
    private List<BigDecimal> hourlyPrices;
    /**
     * 长度 24，可为 null；元素允许为 null
     */
    private List<BigDecimal> hourlyIntegrals;
}

