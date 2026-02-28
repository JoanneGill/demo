package com.example.demo.Mapper;

import com.example.demo.Data.PpTask;
import com.example.demo.Data.PpTaskClaim;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigInteger;
import java.util.List;

@Mapper
public interface PpTaskMapper {

    List<PpTask> selectPpTaskList(@Param("pt") PpTask ppTask);

    Integer selectPpTaskListTotal(@Param("pt") PpTask ppTask);

    int insertPpTask(@Param("pt") PpTask ppTask);

    int updatePpTask(@Param("pt") PpTask ppTask);

    int deletePpTask(@Param("id") BigInteger id);

    List<PpTaskClaim> selectPpTaskClaimList(@Param("taskId") BigInteger taskId);

    int updatePpTaskClaimStatus(@Param("id") BigInteger id, @Param("status") String status);
}
