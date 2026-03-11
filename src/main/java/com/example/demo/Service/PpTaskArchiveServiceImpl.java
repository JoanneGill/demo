package com.example.demo.Service;

import com.example.demo.Data.PpTask;
import com.example.demo.Mapper.PpTaskClaimHistoryMapper;
import com.example.demo.Mapper.PpTaskClaimMapper;
import com.example.demo.Mapper.PpTaskHistoryMapper;
import com.example.demo.Mapper.PpTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;

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

    /**
     * 归档单个已完成的任务
     * 在同一事务中执行：
     * 1. INSERT INTO pp_task_history SELECT ... FROM pp_task WHERE id = taskId
     * 2. INSERT INTO pp_task_claim_history SELECT ... FROM pp_task_claim WHERE task_id = taskId
     * 3. DELETE FROM pp_task_claim WHERE task_id = taskId
     * 4. DELETE FROM pp_task WHERE id = taskId
     */
    @Transactional
    @Override
    public void archiveTask(PpTask task) {
        if (task == null) {
            log.warn("归档失败，任务不存在: {}", task);
            return;
        }
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

        log.info("任务归档完成: taskId={}", task);
    }
}
