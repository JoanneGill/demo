package com.example.demo.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * YL 商品按 Dy 池（rq / pp）同步上下架与库存时的设备阈值，见 {@code yl.device-policy}。
 */
@Component
@ConfigurationProperties(prefix = "yl.device-policy")
public class YlDevicePolicyProperties {

    private Rq rq = new Rq();
    private Pp pp = new Pp();

    public Rq getRq() {
        return rq;
    }

    public void setRq(Rq rq) {
        this.rq = rq;
    }

    public Pp getPp() {
        return pp;
    }

    public void setPp(Pp pp) {
        this.pp = pp;
    }

    public static class Rq {
        private int deviceLow = 400;
        private int deviceHigh = 1000;
        private int stockReserve = 300;

        public int getDeviceLow() {
            return deviceLow;
        }

        public void setDeviceLow(int deviceLow) {
            this.deviceLow = deviceLow;
        }

        public int getDeviceHigh() {
            return deviceHigh;
        }

        public void setDeviceHigh(int deviceHigh) {
            this.deviceHigh = deviceHigh;
        }

        public int getStockReserve() {
            return stockReserve;
        }

        public void setStockReserve(int stockReserve) {
            this.stockReserve = stockReserve;
        }
    }

    public static class Pp {
        private int deviceLow = 200;
        private int deviceHigh = 800;
        private int stockReserve = 150;

        public int getDeviceLow() {
            return deviceLow;
        }

        public void setDeviceLow(int deviceLow) {
            this.deviceLow = deviceLow;
        }

        public int getDeviceHigh() {
            return deviceHigh;
        }

        public void setDeviceHigh(int deviceHigh) {
            this.deviceHigh = deviceHigh;
        }

        public int getStockReserve() {
            return stockReserve;
        }

        public void setStockReserve(int stockReserve) {
            this.stockReserve = stockReserve;
        }
    }
}
