package com.example.demo.Mapper;


import com.example.demo.Data.DeviceData;
import com.example.demo.Data.TaskData;
import com.example.demo.Data.User;
import jakarta.websocket.server.PathParam;
import org.apache.ibatis.annotations.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Mapper
public interface TaskMapper {


    @Insert({"insert into taskTable (roomAddress,time,type,weight,number) values(#{roomAddress},#{time},#{type},#{weight},#{number})"})
    public boolean setTask(@Param("taskData")TaskData taskData);


    public Boolean insertDeviceData(@Param("deviceDataList") List<DeviceData> deviceDataList);


    public boolean updateUser(@Param("userList")List<User> userList );

   @Insert({"insert deviceData(cardNo, deviceId, deviceNickName, roomId, personName, state, startWorkingState, lastWorkingState, duration, `date`)" +
           "values(#{dv.cardNo},#{dv.deviceId},#{dv.deviceNickName},#{dv.roomId},#{dv.personName},#{dv.state},#{dv.startWorkingState},#{dv.lastWorkingState},#{dv.duration},now())"})
    Boolean insertDeviceDataOnce(@Param("dv") DeviceData deviceData);

     List<TaskData>  getTaskList(@Param("pageSize")Integer pageSize,@Param("size") Integer size );

     @Insert({"insert into taskTable (time, roomAddress, duration, videoName, number, roomId, state, integral, realDieTime," +
             "numberStatic,beginTimeFrom,beginTimeTo,creatIntegral,personAddress)" +
             "values (#{taskData.time},#{taskData.roomAddress},#{taskData.duration},#{taskData.videoName},#{taskData.number}," +
             "#{taskData.roomId},#{taskData.state},#{taskData.integral},#{taskData.realDieTime},#{taskData.numberStatic}," +
             "#{taskData.beginTimeFrom},#{taskData.beginTimeTo},#{taskData.creatIntegral},#{taskData.personAddress})"})
    boolean addTask(@Param("taskData") TaskData taskData);


    Boolean deleteHistoryTasks(@Param("taskDataList") List<TaskData> taskDataList);

    @Insert({"insert into temptasktable (id,time, roomAddress, duration, videoName, number, roomId, state, integral, realDieTime,numberStatic,beginTimeFrom,beginTimeTo,personAddress)" +
            "values (#{taskData.id},#{taskData.time},#{taskData.roomAddress},#{taskData.duration},#{taskData.videoName}," +
            "#{taskData.number},#{taskData.roomId},#{taskData.state},#{taskData.integral},#{taskData.realDieTime}," +
            "#{taskData.numberStatic},#{taskData.beginTimeFrom},#{taskData.beginTimeTo},#{taskData.personAddress})"})
    boolean addTempTask(@Param("taskData") TaskData taskData);


    @Select({"select * from temptasktable "})
    List<TaskData> selectAllTempTask();

    @Select({"select count(1) from temptasktable where id = #{id} "})
    Integer selectCountByIdTempTask(@Param("id")Integer id);

    @Select({"select * from temptasktable where  beginTimeFrom < #{endTime} "})
    List<TaskData> selectTempTaskByEndTime(@Param("endTime")Date endTime);


    @Delete({"delete from temptasktable where id= #{taskData.id}"})
    boolean deleteTempTask(@Param("taskData") TaskData taskData);

    Integer getTaskListCount();

    List<TaskData> getTempTaskList(@Param("pageSize") Integer pageSize, @Param("size") Integer size);

    Integer getTempTaskListCount();

    @Select({"select sum(creatIntegral) from tasktable where DATE(realDieTime) =#{searchDate}"})
    Integer getOneDayTotalIntegral(String searchDate);


    @Delete({"delete from taskTable where realDieTime < #{date}"})
    boolean deleteTaskByTime(@Param("date") String date);

}
