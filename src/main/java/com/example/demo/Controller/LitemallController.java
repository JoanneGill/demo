package com.example.demo.Controller;


import com.example.demo.Address.XiguaAddress;
import com.example.demo.Config.AjaxResult;
import com.example.demo.Data.PpTask;
import com.example.demo.Data.TaskData;
import com.example.demo.Mapper.TaskMapper;
import com.example.demo.Model.LitemallModel;
import com.example.demo.Model.TaskModel;
import com.example.demo.Service.BusinessException;
import com.example.demo.Service.LitemallService;
import com.example.demo.Service.PpTaskDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/litemall")
public class LitemallController {

    public static final String KEY_MD5 = "MD6";



    @Autowired
    TaskModel taskModel;

    @Autowired
    XiguaAddress xiguaAddress;

    @Autowired
    TaskMapper taskMapper;

    @Autowired
    LitemallModel litemallModel;

    @Autowired
    PpTaskDispatchService ppTaskDispatchServiceImpl;

    @Autowired
    LitemallService litemallService;

    /**
     *  litemall 商城自助下单任务
     * @param taskData
     * @return
     */


    @PostMapping("/setTask")
    public AjaxResult setTask(@RequestBody TaskData taskData ){
        try {
            litemallService.setTask(taskData);
            return AjaxResult.success();
        } catch (BusinessException e) {
            return AjaxResult.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("setTask 异常:", e);
            return AjaxResult.fail(404, "系统异常，请稍后重试");
        }
    }

    /*
    * 获取空闲设备数量
    *
    *
    * */
    @GetMapping("/devices")
    public AjaxResult getDevices(){
        return AjaxResult.success(litemallService.getDevices());
    }

    @PostMapping("/checkTask")
    public AjaxResult checkTask(@RequestBody TaskData taskData){
        try {
            litemallService.checkTask(taskData);
           return AjaxResult.success();
        }catch (BusinessException i){
            return AjaxResult.fail(i.getCode(),i.getMessage());
        }
        catch (Exception e) {
            log.error("新增 Task 异常:", e);
            return AjaxResult.fail(404, "系统异常，请稍后重试");
        }

    }


    @PostMapping("/setPpTask")
    public AjaxResult setPpTask(@RequestBody PpTask taskData){
        try {
            PpTask result = ppTaskDispatchServiceImpl.addPpTask(taskData);
            return AjaxResult.success("新增成功", result);
        } catch (BusinessException i){
            return AjaxResult.fail(i.getCode(),i.getMessage());
        }
        catch (Exception e) {
            log.error("新增 ppTask 异常:", e);
            return AjaxResult.fail(404, "系统异常，请稍后重试");
        }
    }
    /*
     * 获取空闲设备数量
     *
     *
     * */
    @GetMapping("/ppDevices")
    public AjaxResult getPpDevices(){
        return AjaxResult.success(ppTaskDispatchServiceImpl.waitDevices());
    }

    @PostMapping("/checkPpTask")
    public AjaxResult checkPpTask(@RequestBody TaskData taskData){
        try {
            litemallService.checkPpTask(taskData);
            return AjaxResult.success();
        }catch (BusinessException i){
            return AjaxResult.fail(i.getCode(),i.getMessage());
        }
        catch (Exception e) {
            log.error("新增 ppTask 异常:", e);
            return AjaxResult.fail(404, "系统异常，请稍后重试");
        }
    }



}
