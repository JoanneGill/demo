package com.example.demo.Mapper;

import com.example.demo.Data.PpTaskClaimHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigInteger;

import java.sql.Date;
import java.util.List;

@Mapper
public interface PpTaskClaimHistoryMapper {

    int insertByTaskId(@Param("taskId") BigInteger taskId);

    List<PpTaskClaimHistory> selectByTaskId(@Param("taskId") BigInteger taskId);


    @Select("SELECT COUNT(*) FROM pp_task_claim_history WHERE begin_time >= #{date} AND begin_time < DATE_ADD(#{date}, INTERVAL 1 DAY)")
    int countBeginTimeByDate(@Param("date") Date date);

    @Select("SELECT COUNT(*) FROM pp_task_claim_history WHERE card_no = #{cardNo} AND begin_time >= #{date} AND begin_time < DATE_ADD(#{date}, INTERVAL 1 DAY) AND status = 'success'")
    int countSuccessByDate(@Param("cardNo") String cardNo, @Param("date") Date date);

    @Select("SELECT COUNT(*) FROM pp_task_claim_history WHERE card_no = #{cardNo} AND begin_time >= #{date} AND begin_time < DATE_ADD(#{date}, INTERVAL 1 DAY) AND (status IS NULL OR status <> 'success')")
    int countFailureByDate(@Param("cardNo") String cardNo, @Param("date") Date date);
}



































