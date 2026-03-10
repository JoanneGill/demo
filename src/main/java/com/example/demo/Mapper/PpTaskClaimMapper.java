package com.example.demo.Mapper;

import com.example.demo.Data.PpTaskClaim;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

@Mapper
public interface PpTaskClaimMapper {

    int insert(PpTaskClaim claim);

    PpTaskClaim selectByIdForUpdate(@Param("id") BigInteger id);

    int markFinished(@Param("id") BigInteger id);

    int markFailed(@Param("id") BigInteger id,@Param("msg") String msg);

    List<PpTaskClaim> selectOverdueClaimedForUpdate(@Param("limit") int limit, @Param("now") Date now);

    int markExpired(@Param("claimId") BigInteger claimId, @Param("msg") String msg);
}
