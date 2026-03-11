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
     * 1. 查询 pp_task 确认 status = 'DONE'
     * 2. INSERT INTO pp_task_history SELECT ... FROM pp_task WHERE id = taskId
     * 3. INSERT INTO pp_task_claim_history SELECT ... FROM pp_task_claim WHERE task_id = taskId
     * 4. DELETE FROM pp_task_claim WHERE task_id = taskId
     * 5. DELETE FROM pp_task WHERE id = taskId
     */
    @Transactional
    @Override
    public void archiveTask(BigInteger taskId) {
        PpTask task = ppTaskMapper.selectByIdForUpdate(taskId);
        if (task == null) {
            log.warn("归档失败，任务不存在: {}", taskId);
            return;
        }
        if (!"DONE".equals(task.getStatus())) {
            log.warn("归档跳过，任务状态非DONE: taskId={}, status={}", taskId, task.getStatus());
            return;
        }

        // 搬移到历史表
        ppTaskHistoryMapper.insertFromTask(taskId);
        ppTaskClaimHistoryMapper.insertByTaskId(taskId);

        // 删除原表数据
        ppTaskClaimMapper.deleteByTaskId(taskId);
        ppTaskMapper.deletePpTask(taskId);

        log.info("任务归档完成: taskId={}", taskId);
    }
}
