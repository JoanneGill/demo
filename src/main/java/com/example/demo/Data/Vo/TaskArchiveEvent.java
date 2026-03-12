package com.example.demo.Data.Vo;

import java.math.BigInteger;

public class TaskArchiveEvent {
    private final BigInteger taskId;

    public TaskArchiveEvent(BigInteger taskId) { this.taskId = taskId; }

    public BigInteger getTaskId() { return taskId; }
}