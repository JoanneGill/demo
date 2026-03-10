package com.example.demo.Controller;

import com.example.demo.Config.AjaxResult;
import com.example.demo.Config.Auth;
import com.example.demo.Data.Pager;
import com.example.demo.Data.PpTask;
import com.example.demo.Data.PpTaskClaim;
import com.example.demo.Mapper.PpTaskClaimMapper;
import com.example.demo.Mapper.PpTaskMapper;
import com.example.demo.Service.PpTaskDispatchServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/ppTask")
public class AdminPpTaskController {

    @Autowired
    PpTaskMapper ppTaskMapper;
    @Autowired
    PpTaskClaimMapper ppTaskClaimMapper;

    @Autowired
    private PpTaskDispatchServiceImpl ppTaskDispatchServiceImpl;

    @Auth(user = "1000")
    @PostMapping("/list")
    public AjaxResult list(@RequestBody PpTask ppTask) {
        List<PpTask> data = ppTaskMapper.selectPpTaskList(ppTask);
        Integer total = ppTaskMapper.selectPpTaskListTotal(ppTask);
        Pager<PpTask> pager = new Pager<>();
        pager.setData(data);
        pager.setTotal(total);
        return AjaxResult.success(pager);
    }

    /**
     * 新增 ppTask 任务
     * POST /admin/ppTask/add
     * Body: { "personAddress": "xxx", "number": 10, "integral": 5, "title": "xxx", "expireTime": "2026-03-11 00:00:00" }
     */

    @PostMapping("/add")
    public AjaxResult add(@RequestBody PpTask ppTask) {
        try {
            PpTask result = ppTaskDispatchServiceImpl.addPpTask(ppTask);
            return AjaxResult.success("新增成功", result);
        } catch (IllegalArgumentException e) {
            log.warn("新增 ppTask 参数错误: {}", e.getMessage());
            return AjaxResult.fail(-1, e.getMessage());
        } catch (Exception e) {
            log.error("新增 ppTask 异常:", e);
            return AjaxResult.fail(500, "系统异常，请稍后重试");
        }
    }

    @Auth(user = "1000")
    @PostMapping("/update")
    public AjaxResult update(@RequestBody PpTask ppTask) {
        if (ppTask.getId() == null) {
            return AjaxResult.fail(-1, "id不能为空");
        }
        int rows = ppTaskMapper.updatePpTask(ppTask);
        if (rows > 0) {
            return AjaxResult.success();
        }
        return AjaxResult.fail(-1, "修改失败");
    }

    @Auth(user = "1000")
    @PostMapping("/delete")
    public AjaxResult delete(@RequestBody PpTask ppTask) {
        if (ppTask.getId() == null) {
            return AjaxResult.fail(-1, "id不能为空");
        }
        int rows = ppTaskMapper.deletePpTask(ppTask.getId());
        if (rows > 0) {
            return AjaxResult.success();
        }
        return AjaxResult.fail(-1, "删除失败");
    }

    @Auth(user = "1000")
    @GetMapping("/claimList")
    public AjaxResult claimList(@RequestParam("taskId") BigInteger taskId) {
        List<PpTaskClaim> list = ppTaskClaimMapper.selectPpTaskClaimList(taskId);
        return AjaxResult.success(list);
    }

    @Auth(user = "1000")
    @PostMapping("/claim/updateStatus")
    public AjaxResult updateClaimStatus(@RequestBody PpTaskClaim ppTaskClaim) {
        if (ppTaskClaim.getId() == null) {
            return AjaxResult.fail(-1, "id不能为空");
        }
        String status = ppTaskClaim.getStatus();
        if (status == null || (!status.equals("FAILED") && !status.equals("EXPIRED"))) {
            return AjaxResult.fail(-1, "状态值不合法，只允许 FAILED 或 EXPIRED");
        }
        int rows = ppTaskClaimMapper.updatePpTaskClaimStatus(ppTaskClaim.getId(), status);
        if (rows > 0) {
            return AjaxResult.success();
        }
        return AjaxResult.fail(-1, "操作失败");
    }
}
