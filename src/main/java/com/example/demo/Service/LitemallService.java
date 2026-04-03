package com.example.demo.Service;


import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.example.demo.Address.XiguaAddress;
import com.example.demo.Config.AjaxResult;
import com.example.demo.Data.DeviceData;
import com.example.demo.Data.GlobalVariablesSingleton;
import com.example.demo.Data.PpTask;
import com.example.demo.Data.TaskData;
import com.example.demo.Model.TaskModel;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

@Service
@Slf4j
public class LitemallService {

    //在线设备对象列表
    List<DeviceData> deviceDataListGlobe = GlobalVariablesSingleton.getInstance().getDeviceDataArrayList();
    //在线任务列表
    List<TaskData> taskDataList = GlobalVariablesSingleton.getInstance().getTaskDataArrayList();

    @Autowired
    XiguaAddress xiguaAddress;

    @Autowired
    TaskModel taskModel;

    @Autowired PpTaskDispatchService ppTaskDispatchServiceImpl;


    public Boolean checkTask(TaskData taskData){

        //参数校验
        if ( taskData.number ==null || taskData.number.equals(0)||taskData.number<0){
            throw new BusinessException(404,"设备数出错");
        }
        if (taskData.duration == null || StrUtil.isEmptyIfStr(taskData.duration)){
            throw new BusinessException(404,"设备时长出错");
        }
        BigDecimal duration= new  BigDecimal(taskData.getDuration()).multiply(new BigDecimal(60));
        taskData.setBeginTimeTo(DateUtil.offsetMinute(DateUtil.parse(taskData.getBeginTimeFrom()),duration.intValue()).toString());
        if ( DateUtil.between( DateUtil.date(),DateUtil.parse(taskData.beginTimeTo), DateUnit.MINUTE)<10){
            throw new BusinessException(404,"任务时间小于十分钟");
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
        if (waitDevices - 200  < taskData.getNumber()){
            throw new BusinessException(404,"设备数不足"+waitDevices+"   "+taskData.getNumber());
        }
        return true;
    }


    public Boolean checkPpTask(TaskData taskData){

        //参数校验
        if ( taskData.number ==null || taskData.number.equals(0)||taskData.number<0){
            throw new BusinessException(404,"设备数出错");
        }
        Integer waitDevices = ppTaskDispatchServiceImpl.waitDevices();
        if (waitDevices < taskData.getNumber()){
            throw new BusinessException(404,"设备数不足"+waitDevices+"   "+taskData.getNumber());
        }
        return true;
    }

    public Integer getDevices() {
        Long currentTime = System.currentTimeMillis();
        Integer waitDevices = 0;

        for (int i = 0; i < deviceDataListGlobe.size(); i++) {
            if (deviceDataListGlobe.get(i).getState() + 1000 * 60 > currentTime) {
                waitDevices++;
            }
        }
        for (int i = 0; i < taskDataList.size(); i++) {
            waitDevices = waitDevices - taskDataList.get(i).getNumberStatic();
        }
        if (waitDevices < 0) {
           throw new BusinessException(404,"设备数不足"+waitDevices);
        }
        return waitDevices;

    }

    public Integer getPpDevices() {
         return  ppTaskDispatchServiceImpl.waitDevices();
    }

    public boolean setTask(TaskData taskData) {

        //参数校验
        if ( taskData.number == null || taskData.number.equals(0) || taskData.number < 0){
            throw new BusinessException(402,"设备数出错");
        }
        if (taskData.duration == null || StrUtil.isEmptyIfStr(taskData.getDuration())){
            throw new BusinessException(403,"时常出错");
        }
        taskData.setCreatIntegral(0L);

        taskData.setNumberStatic(taskData.getNumber());

        BigDecimal duration = new BigDecimal(taskData.getDuration()).multiply(new BigDecimal(60));

        taskData.setBeginTimeTo(DateUtil.offsetMinute(DateUtil.parse(taskData.getBeginTimeFrom()), duration.intValue()).toString());

        if ( DateUtil.between(DateUtil.date(), DateUtil.parse(taskData.beginTimeTo), DateUnit.MINUTE) < 10){
            throw new BusinessException(404,"任务时间小于十分钟");
        }

        taskData.setDuration(duration.toString());

        //设置截止时间戳
        taskData.setTime(String.valueOf(DateUtil.parse(taskData.beginTimeTo).getTime()));

        log.info(taskData.getBeginTimeTo() + taskData.getBeginTimeFrom() + taskData.getDuration());

        if (taskData.integral == null || taskData.integral <= 0){
            throw new BusinessException(405,"请输入任务每分钟积分");
        }

        if (taskData.getRoomAddress() != null && !taskData.getRoomAddress().isEmpty()){
            //解析直播间主页
            String pageSource = xiguaAddress.getPageSource(taskData.getRoomAddress());
            if (pageSource == null || !pageSource.contains("fromshareroomid")){
                throw new BusinessException(409,"直播结束或出错");
            }
            //解析直播间roomId
            String roomId = xiguaAddress.getRoomIdByBrowser(pageSource);
            if (roomId == null || roomId.isBlank() ){
                throw new BusinessException(404,"地址解析错误");
            }
            //获取直播人名
            String videoName = xiguaAddress.getVideoNameByBrowser(pageSource);
            if (videoName == null || videoName.isBlank() ){
                throw new BusinessException(404,"直播人地址解析错误");
            }
            String xiguaName  = xiguaAddress.getXiGuaName(roomId);
            if (xiguaName == null || xiguaName.isEmpty() || xiguaName.isBlank()){
                throw new BusinessException(404,"xg地址解析错误");
            }
            taskData.setVideoNameXiGua(xiguaName);
            taskData.setRoomId(roomId);
            taskData.setVideoName(xiguaName);
        } else {
            if (taskData.getPersonAddress() != null && !taskData.getPersonAddress().isEmpty()){
                //解析直播间roomId
                String sec_uid = xiguaAddress.getsecuidBypersonAddress(taskData.getPersonAddress());
                String jsonStr = xiguaAddress.getTaskInfoBySecUid(sec_uid);
                JsonObject jsonElement = null;

                if (jsonStr != null && !jsonStr.isEmpty()) {
                    try {
                        jsonElement = JsonParser.parseString(jsonStr).getAsJsonObject();
                    } catch (Exception e) {
                        log.warn("解析json失败: {}", jsonStr, e); // 记录解析失败内容
                        throw new BusinessException(404,"地址解析错误1");
                    }
                } else {
                    log.warn("getTaskInfoBySecUid返回空: sec_uid={}", sec_uid);
                    throw new BusinessException(404,"地址解析错误2");
                }

                String roomId = jsonElement.get("roomId").getAsString();
                String uid = jsonElement.get("uid").getAsString();
                if (roomId == null || roomId.isEmpty() || roomId.isBlank()){
                    throw new BusinessException(404,"地址解析错误3");
                }

                String yellowish = xiguaAddress.getYellowish(roomId);
                if ("yellow".equals(yellowish)){
                    throw new BusinessException(405,"禁止小黄车");
                } else if ("connect".equals(yellowish)){
                    throw new BusinessException(404,"禁止连线");
                }

                taskData.setRoomId(roomId);
                taskData.setUid(sec_uid);
                taskData.setUid(uid);
                String xiguaName = xiguaAddress.getXiGuaName(roomId);
                if (xiguaName == null || xiguaName.isEmpty() || xiguaName.isBlank()){
                    xiguaName = xiguaAddress.getXiGuaName(roomId);
                }
                if (xiguaName != null){
                    taskData.setVideoName(xiguaName);
                    taskData.setVideoNameXiGua(xiguaName);
                }
                if (xiguaName == null || xiguaName.isEmpty() || xiguaName.isBlank()){
                    throw new BusinessException(404,"名字解析错误");
                }
            } else {
                throw new BusinessException(404,"地址错误");
            }
        }

        if (StrUtil.isEmptyIfStr(taskData.getRoomId()) || StrUtil.isEmptyIfStr(taskData.getVideoName()) || !NumberUtil.isNumber(taskData.getRoomId())){
            throw new BusinessException(408,"RoomId 出错");
        }

        taskData.setBeginTimeFrom(DateUtil.date(Calendar.getInstance()).toString());
        taskData.setId(IdUtil.randomUUID());
        taskModel.setTask(taskData);

        return true;
    }

    public boolean setPpTask(TaskData taskData){
        PpTask ppTask = new PpTask();
        ppTask.setBeginTime(taskData.getBeginTimeFrom());
        ppTask.setPersonAddress(taskData.getPersonAddress());
        ppTask.setIntegral(taskData.getIntegral());
        ppTask.setYlGoodId(taskData.getYlGoodId());
        ppTask.setYlOrderId(taskData.getYlOrderId());
        ppTask.setYlAppId(taskData.getYlAppId());
        if (taskData.getNumber() != null) {
            ppTask.setTotalNumber(taskData.getNumber());
        }
        ppTaskDispatchServiceImpl.addPpTask(ppTask);
        return true;
    }

}
