package com.example.demo.Service;

import com.example.demo.Data.PpTask;

import java.math.BigInteger;
import java.util.List;

public interface PpTaskArchiveService {
    void archiveTask(BigInteger id);

    /**
     * 将指定任务及其认领记录移动到历史表，并从原表删除（不限状态）
     * @param taskIds 任务ID列表
     */
    void moveTasksToHistory(List<BigInteger> taskIds);
}
