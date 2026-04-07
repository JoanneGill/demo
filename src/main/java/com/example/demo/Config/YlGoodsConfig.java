package com.example.demo.Config;



import com.example.demo.common.DyTaskType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

/**
 * 单个 YL 商品在本地 config 中的定义：{@code id} 与 YL 商品 id 一致；
 * {@code dyPool} 决定该商品走 Dy 的 rq 还是 pp 接口（查设备、校验任务、下发任务均按此池）。
 * <p>
 * 配置示例（推荐按小时合并写）：
 * <pre>{@code
 * "goods": [{
 *   "id": 1001,
 *   "dyPool": "rq",
 *   "hourlyPricesIntegrals": {
 *     "*": {"hourlyPrices": 10, "hourlyIntegrals": 30}
 *   }
 * }]
 * }</pre>
 * 亦可使用 {@code "0"}…{@code "23"} 为键逐小时配置，或用数组 {@code [{"0":{...}},{"1":{...}}]}。
 * 仍兼容旧字段 {@code hourlyPrices} / {@code hourlyIntegrals}（24 长度数组或单值填充）。
 * {@code dyPool} 可省略，省略时按 {@code rq}。兼容旧字段名 {@code taskType}，解析时等同 {@code dyPool}。
 */
public final class YlGoodsConfig {

    private static final int HOURS = 24;

    private final int ylGoodsId;
    /** rq / pp，null 表示默认 rq */
    private final String dyPool;
    private final BigDecimal[] hourlyPrices;
    private final BigDecimal[] hourlyIntegrals;

    public YlGoodsConfig(int ylGoodsId, String dyPool, BigDecimal[] hourlyPrices, BigDecimal[] hourlyIntegrals) {
        this.ylGoodsId = ylGoodsId;
        this.dyPool = dyPool;
        this.hourlyPrices = hourlyPrices != null ? hourlyPrices : new BigDecimal[HOURS];
        this.hourlyIntegrals = hourlyIntegrals != null ? hourlyIntegrals : new BigDecimal[HOURS];
    }

    public int getYlGoodsId() {
        return ylGoodsId;
    }

    public String getDyPool() {
        return dyPool;
    }

    public BigDecimal[] getHourlyPrices() {
        return hourlyPrices != null ? Arrays.copyOf(hourlyPrices, hourlyPrices.length) : new BigDecimal[HOURS];
    }

    public BigDecimal[] getHourlyIntegrals() {
        return hourlyIntegrals != null ? Arrays.copyOf(hourlyIntegrals, hourlyIntegrals.length) : new BigDecimal[HOURS];
    }

    public DyTaskType getDyTaskType() {
        return DyTaskType.fromConfig(dyPool);
    }

    public BigDecimal priceAt(LocalDateTime when) {
        int idx = when.getHour();
        if (hourlyPrices == null || idx < 0 || idx >= hourlyPrices.length) {
            return null;
        }
        return hourlyPrices[idx];
    }

    public BigDecimal integralAt(LocalDateTime when) {
        int idx = when.getHour();
        if (hourlyIntegrals == null || idx < 0 || idx >= hourlyIntegrals.length) {
            return BigDecimal.ZERO;
        }
        BigDecimal v = hourlyIntegrals[idx];
        return v != null ? v : BigDecimal.ZERO;
    }

    public boolean hasAnyPrice() {
        if (hourlyPrices == null) {
            return false;
        }
        for (BigDecimal p : hourlyPrices) {
            if (p != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        YlGoodsConfig that = (YlGoodsConfig) o;
        return ylGoodsId == that.ylGoodsId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ylGoodsId);
    }
}
