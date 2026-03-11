package com.example.demo.Mapper;

import com.example.demo.Data.PpTask;
import com.example.demo.Data.PpTaskHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigInteger;
import java.util.List;

@Mapper
public interface PpTaskHistoryMapper {

    int insertFromTask(@Param("taskId") BigInteger taskId);

    List<PpTaskHistory> selectHistoryList(@Param("pt") PpTaskHistory ppTaskHistory);

    Integer selectHistoryListTotal(@Param("pt") PpTaskHistory ppTaskHistory);
}
