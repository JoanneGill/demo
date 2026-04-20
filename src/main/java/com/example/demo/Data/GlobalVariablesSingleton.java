package com.example.demo.Data;

import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

@Component
public class GlobalVariablesSingleton {

    private static GlobalVariablesSingleton instance = new GlobalVariablesSingleton();



   //任务列表
    private final ArrayList<TaskData> taskDataArrayList;
   //设备列表
    private final ArrayList<DeviceData> deviceDataArrayList;

   //用户列表
    private final ArrayList<User> users;

    private GlobalVariablesSingleton() {
        // 初始化全局变量
        taskDataArrayList = new ArrayList<TaskData>();
        users = new ArrayList<User>();
        deviceDataArrayList = new ArrayList<DeviceData>(1500);

    }

    public static GlobalVariablesSingleton getInstance() {
        return instance;
    }



    public ArrayList<TaskData> getTaskDataArrayList() {

        return taskDataArrayList;
    }

    public ArrayList<User> getUsers() {
        return users;
    }

    public ArrayList<DeviceData> getDeviceDataArrayList(){return deviceDataArrayList; }
}
