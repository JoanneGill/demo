package com.example.demo.Mapper;

import com.example.demo.Data.PpTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigInteger;

@Mapper
public interface PpTaskMapper {

    PpTask selectOneNotExecutedForUpdate(@Param("roomId") String roomId, @Param("deviceId") String deviceId);

    int incrementCompatedNumberIfNotFull(@Param("id") BigInteger id);

}
