package com.example.demo.Controller;


import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.demo.Address.XiguaAddress;
import com.example.demo.Config.AjaxResult;
import com.example.demo.Config.Auth;
import com.example.demo.Data.DeviceData;
import com.example.demo.Data.GlobalVariablesSingleton;
import com.example.demo.Data.TaskData;
import com.example.demo.Mapper.TaskMapper;
import com.example.demo.Model.LitemallModel;
import com.example.demo.Model.TaskModel;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RestController
@RequestMapping("/litemall")
public class LitemallController {

    public static final String KEY_MD5 = "MD6";


    //在线设备对象列表
    List<DeviceData> deviceDataListGlobe = GlobalVariablesSingleton.getInstance().getDeviceDataArrayList();
    //在线任务列表
    List<TaskData> taskDataList = GlobalVariablesSingleton.getInstance().getTaskDataArrayList();
    @Autowired
    TaskModel taskModel;

    @Autowired
    XiguaAddress xiguaAddress;

    @Autowired
    TaskMapper taskMapper;

    @Autowired
    LitemallModel litemallModel;


    /**
     *  litemall 商城自助下单任务
     * @param taskData
     * @return
     */


    @PostMapping("/setTask")
    public AjaxResult setTask(@RequestBody TaskData taskData ){

        if ( taskData.getToken() == null||!taskData.getToken().equals("asjdfbajskdfnkwe234123kljdfnkljsdgn2kwfdlknasdln")   ){
            return AjaxResult.fail(401,"参数出错");
        }


        //参数校验
        if ( taskData.number ==null || taskData.number.equals(0)||taskData.number<0){
            return AjaxResult.fail(402,"设备数出错");
        }
        if (taskData.duration == null){
            return AjaxResult.fail(403,"时常出错");
        }
        taskData.setCreatIntegral(0L);

        taskData.setNumberStatic(taskData.getNumber());

        BigDecimal duration= new  BigDecimal(taskData.getDuration()).multiply(new BigDecimal(60));

        taskData.setBeginTimeTo(DateUtil.offsetMinute(DateUtil.parse(taskData.getBeginTimeFrom()),duration.intValue()).toString());

        if ( DateUtil.between( DateUtil.date(),DateUtil.parse(taskData.beginTimeTo), DateUnit.MINUTE)<10){
            return AjaxResult.fail(404,"任务时间小于十分钟");
        }

        taskData.setDuration(duration.toString());


        //设置截止时间戳
        taskData.setTime(String.valueOf(DateUtil.parse(taskData.beginTimeTo).getTime()));

        log.error(taskData.getBeginTimeTo()+taskData.getBeginTimeFrom()+taskData.getDuration());

        if (taskData.integral ==null || taskData.integral <= 0){
            return AjaxResult.fail(405,"请输入任务每分钟积分");
        }

        if (taskData.getRoomAddress() !=null && !taskData.getRoomAddress().isEmpty()){
            //解析直播间主页
            String pageSource = xiguaAddress.getPageSource(taskData.getRoomAddress());
            if (pageSource == null || !pageSource.contains("fromshareroomid")){
                return AjaxResult.fail(409,"直播结束或出错");
            }
            //解析直播间roomId
            String roomId = xiguaAddress.getRoomIdByBrowser(pageSource);
            if (roomId == null || roomId.isBlank() ){
                return AjaxResult.fail(404,"地址解析错误");
            }
            //获取直播人名
            String videoName = xiguaAddress.getVideoNameByBrowser(pageSource);
            if (videoName == null || videoName.isBlank() ){
                return AjaxResult.fail(404,"直播人地址解析错误");
            }
            String xiguaName  = xiguaAddress.getXiGuaName(roomId);
            if (xiguaName == null || xiguaName.isEmpty() || xiguaName.isBlank()){
                return AjaxResult.fail(404,"xg地址解析错误");
            }
            taskData.setVideoNameXiGua(xiguaName);
            taskData.setRoomId(roomId);
            taskData.setVideoName(xiguaName);
        }
        else{
            if (taskData.getPersonAddress() !=null && !taskData.getPersonAddress().isEmpty()){
                //解析直播间roomId
                String sec_uid = xiguaAddress.getsecuidBypersonAddress(taskData.getPersonAddress());
                String roomId = xiguaAddress.getRoomIdByPersonAddress(sec_uid);

                if (roomId == null ||  roomId.isEmpty() || roomId.isBlank()){
                    return AjaxResult.fail(404,"地址解析错误");
                }

                String yellowish = xiguaAddress.getYellowish(roomId);
                if (yellowish == null || yellowish.isEmpty() || yellowish.isBlank() || "true".equals(yellowish)){
                    return AjaxResult.fail(404,"禁止小黄车");
                }

                taskData.setRoomId(roomId);

                String xiguaName = xiguaAddress.getXiGuaName(roomId);
                if (xiguaName == null || xiguaName.isEmpty() || xiguaName.isBlank()){
                    xiguaName = xiguaAddress.getXiGuaName(roomId);
                }
                if (xiguaName != null){
                    taskData.setVideoName(xiguaName);
                    taskData.setVideoNameXiGua(xiguaName);
                }
                if (xiguaName == null || xiguaName.isEmpty() || xiguaName.isBlank()){
                    return AjaxResult.fail(404,"名字解析错误");
                }
            } else { return AjaxResult.fail(404,"地址错误"); }
        }

        if (StrUtil.isEmptyIfStr(taskData.getRoomId()) || StrUtil.isEmptyIfStr(taskData.getVideoName()) || !NumberUtil.isNumber(taskData.getRoomId())){
            return AjaxResult.fail(408,"RoomId 出错");
        }

        taskData.setBeginTimeFrom(DateUtil.date(Calendar.getInstance()).toString());
        taskData.setId(IdUtil.randomUUID());
        taskModel.setTask(taskData);

        return  AjaxResult.success();
    }

    /*
    * 获取空闲设备数量
    *
    *
    * */
    @GetMapping("/devices")
    public AjaxResult getDevices(){
        Long currentTime = System.currentTimeMillis();
        Integer waitDevices = 0;

        for (int i = 0; i < deviceDataListGlobe.size(); i++) {
             if (deviceDataListGlobe.get(i).getState()+1000*60 > currentTime){
                waitDevices++;
            }
        }
        for (int i = 0; i < taskDataList.size(); i++) {
            waitDevices = waitDevices - taskDataList.get(i).getNumberStatic();
        }
        if (waitDevices<0){
            return AjaxResult.fail(404,"设备数出错");
        }
        log.info("workingDevices {}",waitDevices);
        return AjaxResult.success(waitDevices);
    }
    @PostMapping("/checkTask")
    public AjaxResult checkTask(@RequestBody TaskData taskData){
        if ( taskData.getToken() == null||!taskData.getToken().equals("asjdfbajskdfnkwe234123kljdfnkljsdgn2kwfdlknasdln")   ){
            return AjaxResult.fail(404,"参数出错");
        }


        //参数校验
        if ( taskData.number ==null || taskData.number.equals(0)||taskData.number<0){
            return AjaxResult.fail(404,"设备数出错");
        }
        if (taskData.duration == null){
            return AjaxResult.fail(404,"时常出错");
        }

        BigDecimal duration= new  BigDecimal(taskData.getDuration()).multiply(new BigDecimal(60));

        taskData.setBeginTimeTo(DateUtil.offsetMinute(DateUtil.parse(taskData.getBeginTimeFrom()),duration.intValue()).toString());

        if ( DateUtil.between( DateUtil.date(),DateUtil.parse(taskData.beginTimeTo), DateUnit.MINUTE)<10){
            return AjaxResult.fail(404,"任务时间小于十分钟");
        }

        Long currentTime = System.currentTimeMillis();
        Integer waitDevices = 0;

        for (int i = 0; i < deviceDataListGlobe.size(); i++) {
            if (deviceDataListGlobe.get(i).getState()+1000*60 > currentTime){
                waitDevices++;
            }
        }
        for (int i = 0; i < taskDataList.size(); i++) {
            waitDevices = waitDevices - taskDataList.get(i).getNumberStatic();
        }

//        List<TaskData> taskDataList1 =  litemallModel.getTempTaskByEndTime(DateUtil.parse(taskData.getBeginTimeTo()));
//        log.error("taskDataList1 {},endtime:{}",taskDataList1,taskData.getBeginTimeTo());
//        if(!taskDataList1.isEmpty()){
//            for (int i = 0; i < taskDataList1.size(); i++) {
//                waitDevices = waitDevices -taskDataList1.get(i).getNumberStatic();
//            }
//        }
        if (waitDevices - 200  < taskData.getNumber()){
            return AjaxResult.fail(404,"设备数不足"+waitDevices+"   "+taskData.getNumber());
        }

        return AjaxResult.success();
    }

    /***
     * MD5加密（生成唯一的MD5值）
     * @param data
     * @return
     * @throws Exception
     */
    public static byte[] encryMD5(byte[] data) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance(KEY_MD5);
        md5.update(data);
        return md5.digest();
    }
//
//    @GetMapping("/test")
//    public AjaxResult test(){
//
//        return AjaxResult.success(xiguaAddress.getRoomIdByPersonAddress("https://v.douyin.com/ikcRf7mg/"));
//
//    }










//    @GetMapping("/getRoomIdBy")
//    public String getRoomId(@PathParam("address") String address){
//        System.out.println("sssss");
//        return  xiguaAddress.getRoomIdByBrowser(address);
//    }











}
