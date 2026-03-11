package com.example.demo.Mapper;

import com.example.demo.Data.PpTaskClaimHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigInteger;
import java.util.List;

@Mapper
public interface PpTaskClaimHistoryMapper {

    int insertByTaskId(@Param("taskId") BigInteger taskId);

    List<PpTaskClaimHistory> selectByTaskId(@Param("taskId") BigInteger taskId);
}
