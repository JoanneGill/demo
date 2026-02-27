package com.example.demo.Service;

import com.example.demo.Data.PpTask;
import com.example.demo.Data.PpTaskClaim;
import com.example.demo.Mapper.PpTaskClaimMapper;
import com.example.demo.Mapper.PpTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Service
public class PpTaskDispatchServiceImpl implements PpTaskDispatchService {

    @Autowired
    private PpTaskMapper ppTaskMapper;

    @Autowired
    private PpTaskClaimMapper ppTaskClaimMapper;

    @Value("${pptask.leaseMinutes:10}")
    private int leaseMinutes;

    @Override
    @Transactional
    public PpTaskClaim claimOne(String roomId, String deviceId, String deviceNickName) {
        PpTask task = ppTaskMapper.selectOneNotExecutedForUpdate(roomId, deviceId);
        if (task == null) {
            return null;
        }
        PpTaskClaim claim = new PpTaskClaim();
        claim.setTaskId(task.getId());
        claim.setDeviceId(deviceId);
        claim.setDeviceNickName(deviceNickName);
        claim.setStatus("CLAIMED");
        long expireMs = System.currentTimeMillis() + (long) leaseMinutes * 60 * 1000;
        claim.setLeaseExpireTime(new Date(expireMs));
        try {
            ppTaskClaimMapper.insert(claim);
        } catch (DuplicateKeyException e) {
            log.warn("Device {} already claimed task {}", deviceId, task.getId());
            return null;
        }
        return claim;
    }

    @Override
    @Transactional
    public void finishSuccess(Long claimId, String deviceId) {
        PpTaskClaim claim = ppTaskClaimMapper.selectByIdForUpdate(claimId);
        if (claim == null) {
            throw new IllegalArgumentException("Claim not found: " + claimId);
        }
        if (!deviceId.equals(claim.getDeviceId())) {
            throw new IllegalArgumentException("Device mismatch for claim " + claimId);
        }
        if ("FINISHED".equals(claim.getStatus())) {
            return;
        }
        int updated = ppTaskMapper.incrementCompatedNumberIfNotFull(claim.getTaskId());
        if (updated == 0) {
            throw new IllegalStateException("Task already full, cannot increment compated_number for task " + claim.getTaskId());
        }
        int marked = ppTaskClaimMapper.markFinished(claimId);
        if (marked == 0) {
            throw new IllegalStateException("Failed to mark claim as FINISHED (already expired or wrong status): " + claimId);
        }
    }

    @Override
    @Transactional
    public void finishFail(Long claimId, String deviceId) {
        PpTaskClaim claim = ppTaskClaimMapper.selectByIdForUpdate(claimId);
        if (claim == null) {
            throw new IllegalArgumentException("Claim not found: " + claimId);
        }
        if (!deviceId.equals(claim.getDeviceId())) {
            throw new IllegalArgumentException("Device mismatch for claim " + claimId);
        }
        if ("FAILED".equals(claim.getStatus())) {
            return;
        }
        ppTaskClaimMapper.markFailed(claimId);
    }

    @Override
    @Transactional
    public void expireOverdueClaims() {
        int count = ppTaskClaimMapper.expireOverdue();
        if (count > 0) {
            log.info("Expired {} overdue pptask claims", count);
        }
    }

}
