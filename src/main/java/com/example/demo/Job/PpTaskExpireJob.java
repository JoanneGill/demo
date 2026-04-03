package com.example.demo.Job;

import com.example.demo.Service.PpTaskDispatchService;
import com.example.demo.Service.YlOrderBusinessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PpTaskExpireJob {

    @Autowired
    private PpTaskDispatchService ppTaskDispatchService;


    @Autowired
    private YlOrderBusinessService ylOrderBusinessService;
    @Scheduled(fixedDelayString = "${pptask.expireScanDelayMs:30000}")
    public void expireOverdueClaims() {
        try {
            ppTaskDispatchService.expireOverdueClaims();
        } catch (Exception e) {
            log.error("Error expiring overdue pptask claims", e);
        }
    }


    /**
     * 查询 YL 订单并执行发货/取消逻辑。
     */
    @Scheduled(fixedDelay = 20 * 1000)
    public void checkYLOrder() {
        ylOrderBusinessService.processPaidOrdersAndFulfill();
    }

    /**
     * 根据 Dy 设备数调整 YL 商品状态，并按小时更新绑定商品价格。
     */
//    @Scheduled(fixedDelay = 10 * 1000)
    public void checkGoodsDevice() {
        ylOrderBusinessService.syncGoodsStockAndHourlyPrice();
    }













}
