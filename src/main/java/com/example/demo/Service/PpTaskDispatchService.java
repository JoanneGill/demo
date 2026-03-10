package com.example.demo.Service;

import com.example.demo.Data.PpTaskClaim;

public interface PpTaskDispatchService {

    PpTaskClaim claimOne(String deviceId,String deviceNickName , String cardNo);

    void finishSuccess(Long claimId, String deviceId);

    void finishFail(Long claimId, String deviceId,String msg);

    void expireOverdueClaims();

}
