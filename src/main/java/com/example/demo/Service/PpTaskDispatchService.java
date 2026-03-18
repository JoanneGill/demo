package com.example.demo.Service;

import com.example.demo.Data.PpTaskClaim;

import java.math.BigInteger;

public interface PpTaskDispatchService {

    PpTaskClaim claimOne(String deviceId,String deviceNickName , String cardNo);

    void finishSuccess(BigInteger claimId, String deviceId,String msg,Integer diamond);

    void finishFail(BigInteger claimId, String deviceId, String msg, Integer diamond);

    void expireOverdueClaims();

}
