package com.example.demo.Service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.example.demo.Address.XiguaAddress;
import com.example.demo.Config.YLApi;
import com.example.demo.Data.*;
import com.example.demo.Data.Vo.TaskArchiveEvent;
import com.example.demo.Mapper.PpTaskClaimMapper;
import com.example.demo.Mapper.PpTaskMapper;
import com.example.demo.Mapper.UserMapper;
import com.google.common.cache.Cache;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.example.demo.Config.ApplicationVariable.*;
import static com.google.common.cache.CacheBuilder.newBuilder;

@Slf4j
@Service
public class PpTaskDispatchServiceImpl implements PpTaskDispatchService {

    @Resource
    private PpTaskMapper ppTaskMapper;

    @Resource
    private PpTaskClaimMapper ppTaskClaimMapper;

    @Resource
    private XiguaAddress xiguaAddress;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Resource
    private UserMapper userMapper;

    @Resource
    private PpTaskArchiveService ppTaskArchiveService;


    List<DeviceData> deviceList = GlobalVariablesSingleton.getInstance().getDeviceDataArrayList();
    //在线设备对象列表
    List<DeviceData> deviceDataListGlobe = GlobalVariablesSingleton.getInstance().getDeviceDataArrayList();
    @Value("${pptask.leaseMinutes:5}")
    private int leaseMinutes;


    final Cache<String, Integer> checkCache = newBuilder()
                    .expireAfterWrite(30, TimeUnit.MINUTES)
                    .build();


    @Override
    @Transactional
    public PpTaskClaim claimOne(String deviceId, String deviceNickName,String cardNo,String devicePersonName) {
        long now = System.currentTimeMillis();
        PpTask task = ppTaskMapper.selectOneNotExecutedForUpdate(deviceId);
        if (task == null) {
            CompletableFuture.runAsync(() ->
                    deviceList.stream()
                            .filter(d -> deviceId.equals(d.getDeviceId()))
                            .findFirst()
                            .ifPresent(d -> d.setPpClaimTime(now)));
            return null;}
        else {
            CompletableFuture.runAsync(() ->
                    deviceList.stream()
                            .filter(d -> deviceId.equals(d.getDeviceId()))
                            .findFirst()
                            .ifPresent(d -> { d.setPpClaimTime(now); d.setPpClaimState(PP_TASK_CLAIM_STATUS_CLAIMED);}));
        }
        PpTaskClaim claim = new PpTaskClaim();
        claim.setTaskId(task.getId());
        claim.setDeviceId(deviceId);
        claim.setStatus(PP_TASK_CLAIM_STATUS_CLAIMED);
        claim.setRoomId(task.getRoomId());
        claim.setPersonAddress(task.getPersonAddress());
        claim.setIntegral(task.getIntegral());
        claim.setBeginTime(DateUtil.now());
        claim.setPpTask(task.toString());
        claim.setVideoName(task.getPersonName());
        claim.setDeviceId(deviceId);
        claim.setCardNo(cardNo);
        claim.setSec_uid(task.getSecUid());
        claim.setDeviceNickName(deviceNickName);
        claim.setDevicePersonName(devicePersonName);
        log.info(claim.toString());
        long expireMs = System.currentTimeMillis() + (long) leaseMinutes * 60 * 1000;
        claim.setLeaseExpireTime(DateUtil.date(expireMs).toString());
        ppTaskClaimMapper.insert(claim);
        int update = ppTaskMapper.updateReceivedTaskNumber(task.getId());
        if (update != 1){
            throw new IllegalArgumentException("Claim Device fail "+ deviceId);
        }
        return claim;
    }

    @Override
    @Transactional(rollbackFor = Exception.class,timeout = 3000)
    public void finishSuccess(BigInteger claimId, String deviceId,String msg,Integer diamond) {
        long now = System.currentTimeMillis();
        PpTaskClaim claim = ppTaskClaimMapper.selectByIdForUpdate(claimId);
            if (claim == null) {
                throw new IllegalArgumentException("Claim not found: " + claimId);
            }
        CompletableFuture.runAsync(() ->
                deviceList.stream()
                        .filter(d -> deviceId.equals(d.getDeviceId()))
                        .findFirst()
                        .ifPresent(d -> { d.setPpClaimTime(now); d.setPpClaimState(null); d.setDiamond(diamond);
                            d.setTodayPpTaskNumberSuccess(Objects.requireNonNullElse(d.getTodayPpTaskNumberSuccess(), 0) + 1);
                            d.setTodayPpTaskIntegral(Objects.requireNonNullElse(d.getTodayPpTaskIntegral(),0)+claim.getIntegral());}));
            if (!deviceId.equals(claim.getDeviceId())) {
                throw new IllegalArgumentException("Device mismatch for claim " + claimId);
            }
            if (!PP_TASK_CLAIM_STATUS_CLAIMED.equals(claim.getStatus())) {
                return;
            }
            int updated = ppTaskMapper.updateCompletedTaskNumber(claim.getTaskId());
            if (updated == 0) {
                throw new IllegalStateException("cannot increment completed_number for task " + claim.getTaskId());
            }
            int marked = ppTaskClaimMapper.markFinished(claimId,diamond,msg);
            if (marked == 0) {
                throw new IllegalStateException("Failed to mark claim as FINISHED (already expired or wrong status): " + claimId);
            }

        if (claim.getCardNo() != null && claim.getIntegral() != null && claim.getIntegral() > 0) {
            boolean ok = userMapper.addTempIntegral(claim.getCardNo(), claim.getIntegral());
            if (!ok) {
                log.error("finishSuccess add tempIntegral failed, cardNo={}, integral={}", claim.getCardNo(), claim.getCardNo());
            }
        }
            // 检查任务是否全部完成，若完成则归档
            // 事物提交后 异步归档，避免长事务和锁表
        eventPublisher.publishEvent(new TaskArchiveEvent(claim.getTaskId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class,timeout = 3000)
    public void finishFail(BigInteger claimId, String deviceId,String msg,Integer diamond,Boolean videoDieOut ) {
        long now = System.currentTimeMillis();
        CompletableFuture.runAsync(() ->
                deviceList.stream()
                        .filter(d -> deviceId.equals(d.getDeviceId()))
                        .findFirst()
                        .ifPresent(d -> { d.setPpClaimTime(now); d.setPpClaimState(null); if (diamond !=null){ d.setDiamond(diamond);}
                            d.setTodayPpTaskNumberFail(Objects.requireNonNullElse(d.getTodayPpTaskNumberFail(),0)+1);}));
        PpTaskClaim claim = ppTaskClaimMapper.selectByIdForUpdate(claimId);
        if (claim == null) {
            throw new IllegalArgumentException("Claim not found: " + claimId);
        }
        if (!deviceId.equals(claim.getDeviceId())) {
            throw new IllegalArgumentException("Device mismatch for claim " + claimId);
        }
        if (!PP_TASK_CLAIM_STATUS_CLAIMED.equals(claim.getStatus())) {
            return;
        }
        int updated = ppTaskMapper.updateFailedTaskNumber(claim.getTaskId());
        if (updated == 0) {
            throw new IllegalStateException("Task already full, cannot increment compated_number for task " + claim.getTaskId());
        }
        int marked = ppTaskClaimMapper.markFailed(claimId,msg);
        if (marked == 0) {
            throw new IllegalStateException("Failed to mark claim as FINISHED (already expired or wrong status): " + claimId);
        }
        if (videoDieOut != null && videoDieOut) {
            checkCache.asMap().merge(String.valueOf(claim.getTaskId()),1, Integer::sum);
            //删除任务 直播结束
            if (checkCache.getIfPresent(String.valueOf(claim.getTaskId())) != null && checkCache.getIfPresent(String.valueOf(claim.getTaskId())) > 3){
                ppTaskArchiveService.moveTasksToHistory(new ArrayList<>(List.of(claim.getTaskId())));
            }
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
                // 给机器加上 错误次数
                CompletableFuture.runAsync(() ->
                        deviceList.stream()
                                .filter(d -> c.getDeviceId().equals(d.getDeviceId()))
                                .findFirst()
                                .ifPresent(d -> { d.setPpClaimState(null); d.setTodayPpTaskNumberFail(Objects.requireNonNullElse(d.getTodayPpTaskNumberFail(),0)+1);}));
            }
            // 如果不足 batchSize，说明基本处理完了
            if (overdue.size() < batchSize) {
                break;
            }
        }
    }

    /**
     * 新增 ppTask 任务
     * <p>
     * 流程：
     * 1. 校验参数（number、integral、personAddress）
     * 2. 通过 personAddress 解析出 roomId 和主播名
     * 3. 检查是否有小黄车/连线
     * 4. 入库
     *
     * @param ppTask 前端传入的任务信息
     * @return 插入后带自增 id 的 PpTask（成功时），或抛异常
     */
    @Override
    @Transactional
    public PpTask addPpTask(PpTask ppTask) {

        // ========== 1. 参数校验 ==========
        if (ppTask.getTotalNumber() == null || ppTask.getTotalNumber() <= 0) {
            throw new BusinessException(404,"任务数量必须大于0");
        }

        if (ppTask.getIntegral() == null || ppTask.getIntegral() <= 0) {
            throw new BusinessException(404,"积分必须大于0");
        }

        if (ppTask.getPersonAddress() == null || ppTask.getPersonAddress().isBlank()) {
            throw new BusinessException(404,"主播个人主页地址不能为空");
        }

        // ========== 2. 解析直播间信息 ==========
        String secUid = xiguaAddress.getsecuidBypersonAddress(ppTask.getPersonAddress());
        if (secUid == null || secUid.isBlank()) {
            throw new BusinessException(404,"个人地址解析错误");
        }

        String taskInfoJson = xiguaAddress.getTaskInfoBySecUid(secUid);
        if (taskInfoJson == null || taskInfoJson.isBlank()) {
            throw new BusinessException(404,"获取任务信息失败");
        }

        JsonObject jsonElement = JsonParser.parseString(taskInfoJson).getAsJsonObject();
        String roomId = jsonElement.get("roomId").getAsString();
        if (roomId == null || roomId.isBlank()) {
            throw new BusinessException(404,"直播间地址解析错误");
        }

        // ========== 3. 检查小黄车/连线 ==========
        String yellowish = xiguaAddress.getYellowish(roomId);
        if ("yellow".equals(yellowish)) {
            throw new BusinessException(404, "禁止小黄车");
        }

        // ========== 4. 获取主播名 ==========
        String personName = xiguaAddress.getXiGuaName(roomId);
        if (personName == null || personName.isBlank()) {
            throw new BusinessException(404,"主播名字解析错误");
        }

        // ========== 5. 组装数据并入库 ==========
        ppTask.setRoomId(roomId);
        ppTask.setSecUid(secUid);
        ppTask.setPersonName(personName);
        ppTask.setStatus(PP_TASK_CLAIM_STATUS_CLAIMED);
        ppTask.setBeginTime(DateUtil.now());
        int rows = ppTaskMapper.insertPpTask(ppTask);
        if (rows <= 0) {
            throw new RuntimeException("新增 ppTask 失败");
        }

        log.info("新增 ppTask 成功: id={}, roomId={}, personName={}, number={}",
                ppTask.getId(), roomId, personName, ppTask.getTotalNumber());

        return ppTask;
    }



    @Override
    public Integer waitDevices(){
        long currentTime = System.currentTimeMillis();
        int waitDevices = 0;
        List<DeviceData> list = deviceDataListGlobe;
        if (list == null || list.isEmpty()) {
            return 0;
        }
        for (DeviceData d : list) {
            if (d == null) continue;
            Long claimTime = d.getPpClaimTime();
            if (claimTime == null) continue;
            String state = d.getPpClaimState();
            if (claimTime + 1000L * 60 > currentTime &&
                    (StrUtil.isEmptyIfStr(state) || state.isEmpty())) {
                waitDevices++;
            }
        }
        return waitDevices;

    }


}
