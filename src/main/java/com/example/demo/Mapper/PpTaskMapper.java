package com.example.demo.Mapper;

import com.example.demo.Data.PpTask;
import com.example.demo.Data.PpTaskClaim;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigInteger;
import java.util.List;

@Mapper
public interface PpTaskMapper {

    PpTask selectOneNotExecutedForUpdate( @Param("deviceId") String deviceId);

    int updateCompletedTaskNumber(@Param("id") BigInteger id);

    int updateFailedTaskNumber(@Param("id") BigInteger id);

    // 管理端方法
    List<PpTask> selectPpTaskList(@Param("pt") PpTask ppTask);

    Integer selectPpTaskListTotal(@Param("pt") PpTask ppTask);

    int insertPpTask(@Param("pt") PpTask ppTask);

    int updatePpTask(@Param("pt") PpTask ppTask);

    int deletePpTask(@Param("id") BigInteger id);




    int updateReceivedTaskNumber(@Param("id") BigInteger id);

}
