package com.example.demo.Mapper;

import com.example.demo.Data.PpTaskClaim;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PpTaskClaimMapper {

    int insert(PpTaskClaim claim);

    PpTaskClaim selectByIdForUpdate(@Param("id") Long id);

    int markFinished(@Param("id") Long id);

    int markFailed(@Param("id") Long id);

    int expireOverdue();

}
