package com.example.demo.Service;

import com.example.demo.Data.PpTaskClaim;

public interface PpTaskDispatchService {

    PpTaskClaim claimOne(String roomId, String deviceId, String deviceNickName);

    void finishSuccess(Long claimId, String deviceId);

    void finishFail(Long claimId, String deviceId);

    void expireOverdueClaims();

}
