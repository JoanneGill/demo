package com.example.demo.Job;

import com.example.demo.Service.PpTaskDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PpTaskExpireJob {

    @Autowired
    private PpTaskDispatchService ppTaskDispatchService;

    @Scheduled(fixedDelayString = "${pptask.expireScanDelayMs:30000}")
    public void expireOverdueClaims() {
        try {
            ppTaskDispatchService.expireOverdueClaims();
        } catch (Exception e) {
            log.error("Error expiring overdue pptask claims", e);
        }
    }

}
