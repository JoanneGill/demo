package com.example.demo.Model;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.example.demo.Config.AjaxResult;
import com.example.demo.Data.DeviceData;
import com.example.demo.Data.GlobalVariablesSingleton;
import com.example.demo.Data.TaskData;
import com.example.demo.Data.User;
import com.example.demo.Mapper.TaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReferenceArray;

@Service
@Slf4j
public class TaskModel {

@Autowired
    TaskMapper taskMapper;

List<TaskData> taskDataListGlobal = GlobalVariablesSingleton.getInstance().getTaskDataArrayList();

List<User> userListGlobal = GlobalVariablesSingleton.getInstance().getUsers();

List<DeviceData> deviceDataListGlobal = GlobalVariablesSingleton.getInstance().getDeviceDataArrayList();

    public TaskData getBestTask(String cardNo,String deviceId){


    for (int i =0; i<taskDataListGlobal.size();i++){
        if (taskDataListGlobal.get(i).number> 0){
            taskDataListGlobal.get(i).number=taskDataListGlobal.get(i).number-1;

            return taskDataListGlobal.get(i);

        }
    }
            return null;

    }
    /**
     * 返回成功
     * @param roomId
     * @return boolean
     * 安全删除任务的总方法入口
     */
    @Transactional
    public Boolean deleteTaskByRoomId(String roomId) {
        //持久化数据

        //1 将有关任务的设备列表信息存入mysql

        //这里使用线程安全的list

        List<DeviceData> deviceDataList = new CopyOnWriteArrayList<>();
       // 找出当前正在执行设备
        GlobalVariablesSingleton.getInstance().getDeviceDataArrayList().stream().forEach(deviceData -> {
            if (deviceData.getRoomId() != null && deviceData.getRoomId().equals(roomId)) {
                deviceDataList.add(deviceData);
            }
        });

        if (deviceDataList.size() !=0){
            taskMapper.insertDeviceData(deviceDataList);
        }

        GlobalVariablesSingleton.getInstance().getDeviceDataArrayList().stream().forEach(deviceData -> {
            if (deviceData.getRoomId() != null && deviceData.getRoomId().equals(roomId)) {
               deviceData.setRoomId("0");
            }
        });

        //2 将用户积分相关缓存存入Mysql   此处如果用户手机过多 高并发可能带来积分计算错误问题 概率低
        if (updateUser()){
        //3 任务列表存入历史任务  删除缓存任务缓存列表
        List<TaskData> tasks = GlobalVariablesSingleton.getInstance().getTaskDataArrayList();
        for (int i=tasks.size()-1;i>=0;i--){
            if (roomId.equals(tasks.get(i).getRoomId())){

                tasks.get(i).setRealDieTime(DateTime.now().toLocalDateTime().toString());

                taskMapper.addTask(tasks.get(i));

                tasks.remove(i);

            }
        }
        }
        return true;

    }

    @Transactional
    public Boolean deleteTaskById(String id) {

        List<DeviceData> deviceDataList = new CopyOnWriteArrayList<>();
        // 找出当前正在执行设备
        GlobalVariablesSingleton.getInstance().getDeviceDataArrayList().stream().forEach(deviceData -> {
            if (deviceData.getRoomId() != null && deviceData.getId().equals(id)) {
                deviceDataList.add(deviceData);
            }
        });

        if (!deviceDataList.isEmpty()){
            taskMapper.insertDeviceData(deviceDataList);
        }

        GlobalVariablesSingleton.getInstance().getDeviceDataArrayList().stream().forEach(deviceData -> {
            if (deviceData.getId() != null && deviceData.getId().equals(id)) {
                deviceData.setRoomId("0");
                deviceData.setId("0");
            }
        });

        //2 将用户积分相关缓存存入Mysql   此处如果用户手机过多 高并发可能带来积分计算错误问题 概率低
        if (updateUser()){
            //3 任务列表存入历史任务  删除缓存任务缓存列表
            List<TaskData> tasks = GlobalVariablesSingleton.getInstance().getTaskDataArrayList();
            for (int i=tasks.size()-1;i>=0;i--){
                if (id.equals(tasks.get(i).getId())){

                    tasks.get(i).setRealDieTime(DateTime.now().toLocalDateTime().toString());

                    taskMapper.addTask(tasks.get(i));

                    tasks.remove(i);

                }
            }
        }
        return true;








    }

    /*
    * 将缓存用户信息和数据库中的信息进行同步  就是给数据库总积分加上缓存积分 然后缓存中减掉加上的积分   其他不管
    * */
     @Transactional
    public Boolean updateUser(){
        //深复制 复制一份当前 全部用户列表 存进mysql
        if (!GlobalVariablesSingleton.getInstance().getUsers().isEmpty()) {

            List<User> users = ObjectUtil.cloneByStream(GlobalVariablesSingleton.getInstance().getUsers());

            //把缓存积分加入数据库缓存积分字段
            log.info("user:{}", users);

            taskMapper.updateUser(users);

            //减去已经入库的缓存积分
            List<User> userList = GlobalVariablesSingleton.getInstance().getUsers();

            for (int i = 0; i < userList.size(); i++) {
                for (int j = 0; j < users.size(); j++) {
                    if (userList.get(i).getCardNo().equals(users.get(j).getCardNo())) {
                            userList.get(i).setTempIntegral(userList.get(i).getTempIntegral() - users.get(j).getTempIntegral());
                            log.info(" DB userListTempIntegral");
                    }
                }
            }
        }
        return true;

    }

    /**
     * 返回成功
     * @param taskData
     * @return boolean
     * 执行任务分配总方法
     */
    public Boolean setTask(TaskData taskData){

//        Long timeNow =System.currentTimeMillis();
//        //初始化 当缓存用户列表为0
//        if (userListGlobal.size()== 0){
//            taskDataListGlobal.add(taskData);
//            return  true;
//        }
//        //开始执行分配任务表
//        //统计当前所有用户空闲设备数量
//        Long now = System.currentTimeMillis();
//        Integer allWaitDevices = 0;
//        for (int i = 0; i < userListGlobal.size(); i++) {
//            userListGlobal.get(i).setWaitDevices(0); // 清除缓存
//            for (int j = 0; j < deviceDataListGlobal.size(); j++) {
//                if (now <deviceDataListGlobal.get(j).getState()+40*1000&& (StrUtil.isEmptyIfStr(deviceDataListGlobal.get(j).getId()) || "0".equals(deviceDataListGlobal.get(j).getId()) )){ //在线且空闲
//                    allWaitDevices++;
//                    userListGlobal.get(i).setWaitDevices(userListGlobal.get(i).getWaitDevices()+1);
//                }
//            }
//        }
//
//        //如果当前空闲设备小于任务总量  直接加进先到先得任务列表
//        if (allWaitDevices+50<taskData.getNumber()){
//            taskDataListGlobal.add(taskData);
//            return  true;
//        }
//
//        //如果当前总空闲设备大于任务总数+50
//        for (int i = 0; i < userListGlobal.size(); i++) {
//
//            if (userListGlobal.get(i).getWaitDevices().equals(0)){
//                continue;
//            }
//            //计算用户分配任务数 用户空闲设备占总空闲设备
//            Integer tasks = (userListGlobal.get(i).getWaitDevices()/allWaitDevices)*taskData.getNumber();
//
//            taskData.setNumber(taskData.getNumber()-tasks);
//            for (int j = 0; j < deviceDataListGlobal.size(); j++) {
//                if (now <deviceDataListGlobal.get(j).getState()+45*1000&& StrUtil.isEmptyIfStr(deviceDataListGlobal.get(j).getId())&&tasks>0){ //在线且空闲
//                    deviceDataListGlobal.get(j).setRoomId(taskData.roomId);
//                    deviceDataListGlobal.get(j).setId(taskData.id);
//                    deviceDataListGlobal.get(j).setHaveWorkTime(timeNow);
//                    deviceDataListGlobal.get(j).setTodayTaskNumber(deviceDataListGlobal.get(j).getTodayTaskNumber()+1);
//                    tasks--;
//                }
//            }
//
//            taskData.setNumber(taskData.getNumber()+tasks);
//        }

        taskDataListGlobal.add(taskData);

        return true;

    }





}
