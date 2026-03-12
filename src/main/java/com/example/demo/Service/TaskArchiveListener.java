package com.example.demo.Service;

import com.example.demo.Data.Vo.TaskArchiveEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TaskArchiveListener {
    @Autowired
    private PpTaskArchiveService ppTaskArchiveService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskArchiveEvent(TaskArchiveEvent event) {
        ppTaskArchiveService.archiveTask(event.getTaskId());
    }
}
