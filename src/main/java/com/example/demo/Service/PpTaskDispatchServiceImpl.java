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
import java.util.List;

import static com.example.demo.Config.ApplicationVariable.*;

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
    public PpTaskClaim claimOne(String deviceId, String deviceNickName,String cardNo) {
        PpTask task = ppTaskMapper.selectOneNotExecutedForUpdate(deviceId);
        if (task == null) {return null;}
        PpTaskClaim claim = new PpTaskClaim();
        claim.setTaskId(task.getId());
        claim.setDeviceId(deviceId);
        claim.setStatus(PP_TASK_CLAIM_STATUS_CLAIMED);
        claim.setRoomId(task.getRoomId());
        claim.setPersonAddress(task.getPersonAddress());
        claim.setIntegral(task.getIntegral());
        claim.setBeginTime(new Date());
        claim.setPpTask(task.toString());
        claim.setVideoName(task.getPersonName());
        claim.setDeviceId(deviceId);
        claim.setDeviceNickName(deviceNickName);
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
        if (PP_TASK_CLAIM_STATUS_SUCCESS.equals(claim.getStatus())) {
            return;
        }
        int updated = ppTaskMapper.updateCompletedTaskNumber(claim.getTaskId());
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
    public void finishFail(Long claimId, String deviceId,String msg) {
        PpTaskClaim claim = ppTaskClaimMapper.selectByIdForUpdate(claimId);
        if (claim == null) {
            throw new IllegalArgumentException("Claim not found: " + claimId);
        }
        if (!deviceId.equals(claim.getDeviceId())) {
            throw new IllegalArgumentException("Device mismatch for claim " + claimId);
        }
        if (PP_TASK_CLAIM_STATUS_FAILED.equals(claim.getStatus())) {
            return;
        }
        int updated = ppTaskMapper.updateCompletedTaskNumber(claim.getTaskId());
        if (updated == 0) {
            throw new IllegalStateException("Task already full, cannot increment compated_number for task " + claim.getTaskId());
        }
        int marked = ppTaskClaimMapper.markFailed(claimId,msg);
        if (marked == 0) {
            throw new IllegalStateException("Failed to mark claim as FINISHED (already expired or wrong status): " + claimId);
        }
    }

    /**
     * 定时任务调用：回收过期 claim
     *
     * 规则：
     * - lease_expire_time < now()
     * - status == CLAIMED 才允许过期（SUCCESS/FAILED 不处理）
     * - 过期成功后：pp_task.received_number - 1（回收占用名额）
     */
    @Override
    @Transactional
    public void expireOverdueClaims() {
        // 每次处理一小批，避免锁表/长事务（可按需要调大/调小）
        final int batchSize = 200;
        while (true) {
            // 1) 查一批“已过期且仍 CLAIMED”的 claim（这里用 for update 防并发重复处理）
            List<PpTaskClaim> overdue = ppTaskClaimMapper.selectOverdueClaimedForUpdate(batchSize, new Date());
            if (overdue == null || overdue.isEmpty()) {
                break;
            }
            for (PpTaskClaim c : overdue) {
                // 2) 先把 claim 标记为 EXPIRED（where status=CLAIMED 保证幂等）
                int marked = ppTaskClaimMapper.markExpired(c.getId(), "任务超时过期");
                if (marked == 0) {
                    continue; // 可能已被其他线程处理/已完成
                }
                // 3) 回收 pp_task.received_number（避免减成负数）
                marked =  ppTaskMapper.updateFailedTaskNumber(c.getTaskId());
                if (marked == 0) {
                    log.error("Task fail expired {} already full or no received_number to decrement when expiring claim {}, taskId={}", c.getTaskId(), c.getId(), c.getTaskId());
                }
            }
            // 如果不足 batchSize，说明基本处理完了
            if (overdue.size() < batchSize) {
                break;
            }
        }
    }

}
