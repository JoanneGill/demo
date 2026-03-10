package com.example.demo.Controller;

import com.example.demo.Config.AjaxResult;
import com.example.demo.Config.Auth;
import com.example.demo.Data.Pager;
import com.example.demo.Data.PpTask;
import com.example.demo.Data.PpTaskClaim;
import com.example.demo.Mapper.PpTaskClaimMapper;
import com.example.demo.Mapper.PpTaskMapper;
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
    @Auth(user = "1000")
    @PostMapping("/list")
    public AjaxResult list(@RequestBody PpTask ppTask) {
        int page = ppTask.getPage() != null && ppTask.getPage() > 0 ? ppTask.getPage() : 1;
        int size = ppTask.getSize() != null && ppTask.getSize() > 0 ? ppTask.getSize() : 20;
        ppTask.setPage(page);
        ppTask.setSize(size);
        ppTask.setPageOffset((page - 1) * size);
        List<PpTask> data = ppTaskMapper.selectPpTaskList(ppTask);
        Integer total = ppTaskMapper.selectPpTaskListTotal(ppTask);
        Pager<PpTask> pager = new Pager<>();
        pager.setData(data);
        pager.setTotal(total);
        return AjaxResult.success(pager);
    }

    @Auth(user = "1000")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody PpTask ppTask) {
        int rows = ppTaskMapper.insertPpTask(ppTask);
        if (rows > 0) {
            return AjaxResult.success("新增成功", ppTask);
        }
        return AjaxResult.fail(-1, "新增失败");
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
