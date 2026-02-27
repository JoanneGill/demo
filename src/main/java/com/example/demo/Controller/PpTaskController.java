package com.example.demo.Controller;

import com.example.demo.Config.AjaxResult;
import com.example.demo.Data.PpTaskClaim;
import com.example.demo.Service.PpTaskDispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ppTask")
public class PpTaskController {

    @Autowired
    private PpTaskDispatchService ppTaskDispatchService;

    @GetMapping("/claim")
    public AjaxResult claim(@RequestParam String deviceId,
                            @RequestParam(required = false) String deviceNickName,
                            @RequestParam(required = false) String roomId) {
        try {
            PpTaskClaim claim = ppTaskDispatchService.claimOne(roomId, deviceId, deviceNickName);
            return AjaxResult.success(claim);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PostMapping("/finishSuccess")
    public AjaxResult finishSuccess(@RequestParam Long claimId,
                                    @RequestParam String deviceId) {
        try {
            ppTaskDispatchService.finishSuccess(claimId, deviceId);
            return AjaxResult.success(null);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PostMapping("/finishFail")
    public AjaxResult finishFail(@RequestParam Long claimId,
                                 @RequestParam String deviceId) {
        try {
            ppTaskDispatchService.finishFail(claimId, deviceId);
            return AjaxResult.success(null);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

}
