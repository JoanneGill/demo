package com.example.demo.Controller;

import com.example.demo.Config.AjaxResult;
import com.example.demo.Data.PpTaskClaim;
import com.example.demo.Service.PpTaskDispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;

@RestController
@RequestMapping("/ppTask")
public class PpTaskController {

    @Autowired
    private PpTaskDispatchService ppTaskDispatchService;


    @GetMapping("/claim")
    public AjaxResult claim(@RequestParam String deviceId,
                            @RequestParam(required = false) String deviceNickName,
                            @RequestParam(required = false) String cardNo) {
        try {
            PpTaskClaim claim = ppTaskDispatchService.claimOne(deviceId,deviceNickName,cardNo);
            return AjaxResult.success(claim);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @GetMapping("/finish")
    public AjaxResult finish(@RequestParam BigInteger claimId,
                             @RequestParam String deviceId,
                             @RequestParam Boolean success,
                             @RequestParam(required = false) String msg,
                             @RequestParam(required = false) Integer diamond) {
        try {
            if (Boolean.TRUE.equals(success)) {
                ppTaskDispatchService.finishSuccess(claimId, deviceId,msg,diamond);
                return AjaxResult.success("FINISH_SUCCESS");
            } else {
                ppTaskDispatchService.finishFail(claimId, deviceId,msg,diamond);
                return AjaxResult.success("FINISH_FAIL");
            }
        } catch (Exception e) {
            // 失败时返回错误信息（你也可以把msg一起返回）
            return AjaxResult.error(e.getMessage());
        }
    }

}
