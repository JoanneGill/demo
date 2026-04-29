package com.example.demo.Service;

import com.example.demo.Config.YLApi;
import com.example.demo.Config.YlGoodsConfig;
import com.example.demo.Data.PpTask;
import com.example.demo.Mapper.PpTaskClaimHistoryMapper;
import com.example.demo.Mapper.PpTaskClaimMapper;
import com.example.demo.Mapper.PpTaskHistoryMapper;
import com.example.demo.Mapper.PpTaskMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.example.demo.Config.ApplicationVariable.PP_TASK_CLAIM_STATUS_SUCCESS;

@Slf4j
@Service
public class PpTaskArchiveServiceImpl implements PpTaskArchiveService {

    @Autowired
    private PpTaskMapper ppTaskMapper;

    @Autowired
    private PpTaskClaimMapper ppTaskClaimMapper;

    @Autowired
    private PpTaskHistoryMapper ppTaskHistoryMapper;

    @Autowired
    private PpTaskClaimHistoryMapper ppTaskClaimHistoryMapper;

    @Autowired
    private  YLmall yLmall;

    /**
     * 归档单个已完成的任务
     * 在同一事务中执行：
     * 1. INSERT INTO pp_task_history SELECT ... FROM pp_task WHERE id = taskId
     * 2. INSERT INTO pp_task_claim_history SELECT ... FROM pp_task_claim WHERE task_id = taskId
     * 3. DELETE FROM pp_task_claim WHERE task_id = taskId
     * 4. DELETE FROM pp_task WHERE id = taskId
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void archiveTask(BigInteger id) {
        try {
            PpTask task = ppTaskMapper.selectByIdForUpdate(id);
            if (task == null) {
                log.warn("归档失败，任务不存在: {}", task);
                return;
            }
            //异步同步 yl商城订单进度
            CompletableFuture.runAsync(() -> {
                try {
                    YLApi.ScheduleHandle scheduleHandle = new YLApi.ScheduleHandle();
                    scheduleHandle.setId(task.getYlOrderId());
                    scheduleHandle.setCurrent_num(task.getCompletedNumber());
                    yLmall.post(YLmall.findByAppId(yLmall.getTargets(),task.getYlAppId()), YLApi.OrderScheduleHandle, new Gson().toJsonTree(scheduleHandle) );
                } catch (Exception e) {
                    log.error("YLmall POST 异步调用失败: taskId={}, error={}", task.getId(), e.getMessage(), e);
                }
            }).orTimeout(5, TimeUnit.SECONDS);
            if (!PP_TASK_CLAIM_STATUS_SUCCESS.equals(task.getStatus())) {
                log.warn("归档跳过，任务状态非DONE: taskId={}, status={}", task.getId(), task.getStatus());
                return;
            }

            // 搬移到历史表
            ppTaskHistoryMapper.insertFromTask(task.getId());
            ppTaskClaimHistoryMapper.insertByTaskId(task.getId());

            // 删除原表数据
            ppTaskClaimMapper.deleteByTaskId(task.getId());
            ppTaskMapper.deletePpTask(task.getId(),null);
            //异步同步 yl商城订单进度 完成
            CompletableFuture.runAsync(() -> {
                try {
                    YLApi.StatusHandle statusHandle = new YLApi.StatusHandle();
                    statusHandle.setId(task.getYlOrderId());
                    statusHandle.setOld_status(YLApi.PROCESSING);
                    statusHandle.setNew_status(YLApi.COMPLETED);
                    yLmall.post(YLmall.findByAppId(yLmall.getTargets(),task.getYlAppId()), YLApi.OrderEditState, new Gson().toJsonTree(statusHandle));
                } catch (Exception e) {
                    log.error("YLmall POST 异步调用失败: taskId={}, error={}", task.getId(), e.getMessage(), e);
                }
            }).orTimeout(5, TimeUnit.SECONDS);
            log.info("任务归档完成: taskId={}", task);
        }
        catch (Exception e) {
            log.error("任务归档失败: taskId={}, error={}", id, e.getMessage(), e);
            throw new RuntimeException("任务归档失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveTasksToHistory(List<BigInteger> taskIds) {
        for (BigInteger taskId : taskIds) {
            try {
                PpTask task = ppTaskMapper.selectByIdForUpdate(taskId);
                if (task == null) {
                    throw new RuntimeException("任务不存在，ID: " + taskId);
                }
                // 2. 将任务主体插入历史表
                ppTaskHistoryMapper.insertFromTask(taskId);

                // 3. 将该任务的所有认领记录插入历史表
                ppTaskClaimHistoryMapper.insertByTaskId(taskId);

                // 4. 删除原表的认领记录
                ppTaskClaimMapper.deleteByTaskId(taskId);

                // 5. 删除原表任务
                ppTaskMapper.deletePpTask(taskId, null);

                log.error("任务已归档删除: taskId={}", taskId);
            }catch (Exception e) {
                log.error("任务归档失败: taskId={}, error={}", taskId, e.getMessage(), e);
                throw new RuntimeException("任务归档失败: " + e.getMessage(), e);
            }
        }
    }


}
