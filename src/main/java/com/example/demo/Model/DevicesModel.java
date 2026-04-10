package com.example.demo.Model;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.example.demo.Config.AjaxResult;
import com.example.demo.Data.DeviceData;
import com.example.demo.Data.GlobalVariablesSingleton;
import com.example.demo.Data.TaskData;
import com.example.demo.Mapper.TaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class DevicesModel {

    @Autowired
    TaskMapper taskMapper;
    List<DeviceData> deviceDataList = GlobalVariablesSingleton.getInstance().getDeviceDataArrayList();
    //在线任务列表
    List<TaskData> taskDataList = GlobalVariablesSingleton.getInstance().getTaskDataArrayList();


    public List<DeviceData> findDevices(String cardNo,String state ){

        Long now = System.currentTimeMillis();

        List<DeviceData> userDeviceDataList = new CopyOnWriteArrayList<>();

        //全部设备
        if (state.equals("all") ){
            deviceDataList.stream().parallel().forEach( deviceData -> {
                        if (cardNo.equals(deviceData.getCardNo())){
                            userDeviceDataList.add(deviceData);
                        }
                    }
            );

        }

        //在线设备
        if (state.equals("online")){
            deviceDataList.stream().parallel().forEach( deviceData -> {
                        if (cardNo.equals(deviceData.getCardNo())&& deviceData.getState()+1000*100 > now){
                            userDeviceDataList.add(deviceData);
                        }
                    }
            );
        }
        //任务中
        if (state.equals("working")){
            deviceDataList.stream().parallel().forEach( deviceData -> {
                        if (cardNo.equals(deviceData.getCardNo())&& deviceData.getState()+1000*100 > now && deviceData.getRoomId()!= null&& !deviceData.getRoomId().isEmpty() ){
                            userDeviceDataList.add(deviceData);
                        }
                    }
            );
        }

        //离线设备
        if (state.equals("offline")){
            deviceDataList.stream().parallel().forEach( deviceData -> {
                        if (cardNo.equals(deviceData.getCardNo())&& deviceData.getState()+1000*100 < now){
                            userDeviceDataList.add(deviceData);
                        }
                    }
            );
        }
       return userDeviceDataList;
    }
public boolean  checkDeviceNumberAllTime(TaskData taskData){

    Long currentTime = System.currentTimeMillis();
    Integer waitDevices = 0;

    for (int i = 0; i < deviceDataList.size(); i++) {
        if (deviceDataList.get(i).getState()+1000*60 > currentTime){
            waitDevices++;
        }
    }

    for (int i = 0; i < taskDataList.size(); i++) {
        waitDevices = waitDevices - taskDataList.get(i).getNumberStatic();
    }

    List<TaskData> taskDataList1 =  taskMapper.selectTempTaskByEndTime(DateUtil.parse(taskData.getBeginTimeTo()));

    if(!taskDataList1.isEmpty()){
        for (int i = 0; i < taskDataList1.size(); i++) {
            waitDevices = waitDevices -taskDataList1.get(i).getNumberStatic();
        }
    }
    if (waitDevices - 200  < taskData.getNumber()){
        return false;
    }
    return true;
}
public boolean  checkNowDeviceNumber(TaskData taskData){

        Long currentTime = System.currentTimeMillis();
        Integer waitDevices = 0;

        for (int i = 0; i < deviceDataList.size(); i++) {
            if (deviceDataList.get(i).getState()+1000*60 > currentTime){
                waitDevices++;
            }
        }

        for (int i = 0; i < taskDataList.size(); i++) {
            waitDevices = waitDevices - taskDataList.get(i).getNumberStatic();
        }

        if (waitDevices - 100  < taskData.getNumber()){
            return false;
        }

        return true;
    }

}
