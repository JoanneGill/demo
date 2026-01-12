package com.example.demo.Controller;


import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;

import com.example.demo.Address.XiguaAddress;
import com.example.demo.Config.AjaxResult;

import com.example.demo.Config.Auth;
import com.example.demo.Config.File;
import com.example.demo.Data.*;
import com.example.demo.Mapper.TaskMapper;
import com.example.demo.Mapper.UserMapper;
import com.example.demo.Model.TaskModel;
import com.example.demo.common.IpUtil;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.websocket.server.PathParam;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RestController
@RequestMapping("/Task")
public class TaskController {
@Autowired
    XiguaAddress xiguaAddress;
@Autowired
    TaskMapper taskMapper;
@Autowired
    TaskModel  taskModel;
@Autowired
    UserMapper userMapper;
@Autowired
    File file;
@Autowired
    IpUtil ipUtil;
//在线任务列表
final List<TaskData> taskDataList = GlobalVariablesSingleton.getInstance().getTaskDataArrayList();

//在线设备对象列表
final List<DeviceData> deviceDataList = GlobalVariablesSingleton.getInstance().getDeviceDataArrayList();

//用户列表
final List<User> userListGlobal = GlobalVariablesSingleton.getInstance().getUsers();

//线程锁
boolean lock = false;
// 每个任务在最近 20 秒内最多领取次数限制（20s -> 10 次）
private final Map<String, Deque<Long>> taskClaimTimestamps = new HashMap<>();

    @GetMapping("/getTask")
    public AjaxResult getTask(@PathParam("cardNo") String cardNo, @PathParam("personName") String personName, @PathParam("time") String time,
                              @PathParam("deviceId") String deviceId, @PathParam("deviceNickName") String deviceNickName,
                              @PathParam("mid") String mid,@PathParam("roomId") String roomId,@PathParam("id") String id,
                               HttpServletRequest httpServletRequest){

        Long timeNow = System.currentTimeMillis();
        //1校验数据md5
        String md5 = SecureUtil.md5(cardNo+personName+time+deviceId+deviceNickName+"sb1314520sbNB$HHHHHHHH");

          if (!md5.equals(mid)){
              log.error(" error md5 find  check  !!!!!!!!!!");
              return  AjaxResult.fail(-1,"?????你在做什么,唱歌");
          }
        if(!(timeNow+70000>=Long.parseLong(time)&&(timeNow-70000)<Long.parseLong(time))){
            log.error("timeError：{}，{}",timeNow,time);
            return  AjaxResult.fail(-1,"?????你在做什么,唱歌");
        }
        //脚本请求接受任务
        //更新设备状态
        int has = 0; //设备是否 在设备列表
        int deviceIndex = 0; //记录设备在设备列表的索引
        TaskData  taskData = null; //要分配的任务
        boolean hasUser = false;//设备用户是否在用户列表中
        if (!userListGlobal.isEmpty()){
            for (int i = 0; i < userListGlobal.size(); i++) {
                if (userListGlobal.get(i).getCardNo().equals(cardNo) && userListGlobal.get(i).getState().equals(1)){  //在账户中 且账户状态正常
                    hasUser =true;
                    break;
                }
            }
        }
        if (!hasUser && !lock  ){
            //当前设备  无用户拥有
            // 从数据库里取出用户信息 存入用户列表
            //锁住
                lock =true;
                User user = userMapper.selectMyInfoByCardNo(cardNo);
                //初始化用户 不需要缓存积分
                user.setTempIntegral(0L);
                log.info("addUserList:{}",user);
                if (user == null || user.getState().equals(0) ){
                    return AjaxResult.fail(-1,"请先注册，或填写正确用户名");
                }

            boolean insite = true;

            for (User value : userListGlobal) {
                if (user.getCardNo().equals(value.getCardNo())) {
                    insite = false;
                    break;
                }
            }
            if (insite){
                userListGlobal.add(user);
            }
           lock =false;
        }

        //是否已在 在线设备列表
        for (int i=0;i<deviceDataList.size();i++){
            //设备再设备列表中
            if (deviceDataList.get(i).getDeviceId().equals(deviceId)){  //设备在列表中
                has = 1;
                deviceIndex = i;
                //更新状态时间
                deviceDataList.get(i).setState(System.currentTimeMillis());
                deviceDataList.get(i).setDeviceNickName(deviceNickName);
                deviceDataList.get(i).setPersonName(personName);
                deviceDataList.get(i).setCardNo(cardNo);
                deviceDataList.get(i).setIp( ipUtil.getIpAddr3(httpServletRequest));
                deviceDataList.get(i).setHaveWorkTime(timeNow);
                deviceDataList.get(i).setLastWorkingState(null);
                //如果任务列表中查不到任务 执行清空设备当前任务
                //删除任务列表中的任务 这里请求后会自动清除  也就是说在删除列表之前 要记录设备列表缓存数据
                deviceDataList.get(i).setStartWorkingState(null);
                deviceDataList.get(i).setDuration(0L);
                deviceDataList.get(i).setScreenImgUrl(null);
                log.info("设备清除任务数据，设备：{}",deviceDataList.get(i).toString());
                break;
            }
        }
        if (has == 0){
            //加入设备列表 初始化
            DeviceData deviceData = new DeviceData();
            deviceData.setDeviceId(deviceId);
            deviceData.setDeviceNickName(deviceNickName);
            deviceData.setCardNo(cardNo);
            deviceData.setPersonName(personName);
            deviceData.setState(System.currentTimeMillis());
            deviceData.setTodayTaskNumber(0);
            deviceData.setTodayTaskIntegral(0L);
            deviceData.setDuration(0L);
            deviceData.setIp(httpServletRequest.getRemoteAddr());
            deviceIndex =deviceDataList.size();
            deviceDataList.add(deviceData);
        }

        // 任务RoomId    设备列表RoomId roomid == “0”         脚本发送的RoomId

        //  1脚本初始化无roomId
        //  2脚本执行完任务    带roomid请求 任务列表中无roomid
        //  3脚本执行出错 重新领取任务 带上上一次的roomid  若无任务 设置设备列表roomid 为0
        //  4脚本离线 服务器回收roomId 设备列表roomId为0  //已回收任务
        //  5服务器分配任务 设备列表roomid为执行需要执行roomId

        //遍历任务
        Integer taskIndex = null;
        if (taskDataList !=null && !taskDataList.isEmpty()){
        for (int i = 0; i < taskDataList.size(); i++) {
            if (taskDataList.get(i).getId().equals(id)){
                taskIndex = i;
                break;
            }
        }
        }
        if (StrUtil.isEmptyIfStr(id) || taskIndex == null || deviceDataList.get(deviceIndex).getId() == null){
            if (taskDataList!=null&&!taskDataList.isEmpty()){
                synchronized (taskClaimTimestamps) {
                    for (TaskData data : taskDataList){
                        if (data == null) continue;
                        String tid = data.getId();
                        Deque<Long> dq = taskClaimTimestamps.computeIfAbsent(tid, k -> new ArrayDeque<>());
                        // 清理过期时间戳（10 秒以前的）
                        while (!dq.isEmpty() && timeNow - dq.peekFirst() > 10_000L) {
                            dq.pollFirst();
                        }
                        // 如果最近 10s 已达到 10 次领取，跳过该任务
                        if (dq.size() >= 10) {
                            log.info("任务 {} 在 10s 内已达到领取上限，跳过", tid);
                            continue;
                        }
                        log.info("遍历任务列表分配任务");
                        if (data.number>0 ){
                            data.number = data.number - 1;
                            taskData = data;
                            dq.addLast(timeNow);
                            break;
                        }
                    }
                }
            }
        }
        else{
            if (!StrUtil.isEmptyIfStr(deviceDataList.get(deviceIndex).getId()) &&
                    "0".equals(deviceDataList.get(deviceIndex).getId())){ //当roomId 为0的时候  1 设备离线 2 一直未进任务
                    synchronized (taskClaimTimestamps) {
                        for (int i = 0; i < taskDataList.size(); i++) {
                            if (!taskDataList.get(i).getId().equals(id) && taskDataList.get(i).getNumber() > 0) {
                                taskDataList.get(i).setNumber(taskDataList.get(i).getNumber() - 1);
                                taskData = taskDataList.get(i);
                                break;
                            }
                        }
                    }
                }
            else if(taskDataList.get(taskIndex).getId().equals(deviceDataList.get(deviceIndex).getId()) &&
                    !StrUtil.isEmptyIfStr(deviceDataList.get(deviceIndex).getId()) &&
                    !deviceDataList.get(deviceIndex).getId().equals("0") &&
                    !deviceDataList.get(deviceIndex).getId().equals(id) ){//脚本被分配任务
                    taskData = taskDataList.get(taskIndex);
                }
            //上一次的id还在任务列表 说明
            //脚本执行出错 重新领取任务 带上上一次的roomId
            //回收脚本之前接的任务
            else if (!StrUtil.isEmptyIfStr(deviceDataList.get(deviceIndex).getId()) &&
                    !deviceDataList.get(deviceIndex).getId().equals("0") && deviceDataList.get(deviceIndex).getId()!= null  &&
                    deviceDataList.get(deviceIndex).getId().equals(id) ){
                if (taskDataList.get(taskIndex).getNumber()+1<=taskDataList.get(taskIndex).getNumberStatic()){
                            taskDataList.get(taskIndex).setNumber(taskDataList.get(taskIndex).getNumber()+1);
                }

                deviceDataList.get(deviceIndex).setRoomId("0");
                deviceDataList.get(deviceIndex).setId("0");
                //取其他任务
                synchronized (taskClaimTimestamps) {
                    for (int i = 0; i < taskDataList.size(); i++) {
                        if (!taskDataList.get(i).getId().equals(id) && taskDataList.get(i).getNumber() > 0) {
                            taskData = taskDataList.get(i);
                            taskDataList.get(i).setNumber(taskDataList.get(i).getNumber() - 1);
                            break;
                        }
                    }
                }
            }
        }
        //根据taskData 是否为null 来确认分配到任务
        if (taskData!=null){

            log.info("成功分配到任务");

            deviceDataList.get(deviceIndex).setRoomId(taskData.roomId);

            deviceDataList.get(deviceIndex).setTodayTaskNumber(deviceDataList.get(deviceIndex).getTodayTaskNumber()+1);

            deviceDataList.get(deviceIndex).setHaveWorkTime(timeNow);

            deviceDataList.get(deviceIndex).setId(taskData.getId());

            taskData.setSid(String.valueOf(timeNow));

            String sid = SecureUtil.md5(taskData.getVideoName()+taskData.getRoomId()+timeNow+"sb1314520sbNB$");

            return AjaxResult.success(sid,taskData);
        }

       return AjaxResult.fail("暂无任务","");

    }

    @Auth(user = "1000")
    @PostMapping("/setTask")
    public AjaxResult setTask(@RequestBody TaskData taskData ) throws InterruptedException {
        //参数校验
        if ( taskData.number ==null || taskData.number.equals(0)||taskData.number<0){
            return AjaxResult.fail(404,"设备数出错");
        }
        log.info(taskData.toString());
        taskData.setCreatIntegral(0L);

        taskData.setNumberStatic(taskData.getNumber());

       Long duration= DateUtil.between( DateUtil.parse(taskData.beginTimeFrom),DateUtil.parse(taskData.beginTimeTo), DateUnit.MINUTE);
        if (duration<10){
            return AjaxResult.fail(404,"任务小于十分钟");
        }

        if (DateUtil.compare( DateUtil.parse(taskData.beginTimeTo),DateUtil.date())<1){

            return AjaxResult.fail(404,"任务最终时间必须大于当前时间");

        }
        if ( DateUtil.between( DateUtil.date(),DateUtil.parse(taskData.beginTimeTo), DateUnit.MINUTE)<10){
            return AjaxResult.fail(404,"任务时间小于十分钟");
        }

        taskData.setDuration(duration.toString());


        //设置截止时间戳
        taskData.setTime(String.valueOf(DateUtil.parse(taskData.beginTimeTo).getTime()));


        if (taskData.integral ==null || taskData.integral <= 0){
            return AjaxResult.fail(404,"请输入任务每分钟积分");
        }
        if (DateUtil.compare( DateUtil.parse(taskData.beginTimeFrom),DateUtil.date())>0){
            // 加入临时任务表
            log.info("addTempTask {}",taskData);
            if ( taskMapper.addTempTask(taskData)){
                return  AjaxResult.success();
            }
            else {
                return AjaxResult.fail(-1,"加入任务出错");
            }
        }

        if (taskData.getRoomAddress() !=null && !taskData.getRoomAddress().isEmpty()){

            String pageSource = xiguaAddress.getPageSource(taskData.getRoomAddress());
            if (pageSource == null || !pageSource.contains("fromshareroomid")){
                return AjaxResult.fail(404,"直播结束或出错");
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

        AtomicReference<String> videoName = new AtomicReference<>();
        AtomicReference<String> xiguaName = new AtomicReference<>();

            if (taskData.getPersonAddress() !=null && !taskData.getPersonAddress().isEmpty()){
                String sec_uid =xiguaAddress.getsecuidBypersonAddress(taskData.getPersonAddress());
                String roomId = xiguaAddress.getRoomIdByPersonAddress(sec_uid);
                if (roomId == null || roomId.isEmpty() || roomId.isBlank()){
                    return AjaxResult.fail(404,"地址解析错误");
                }

                String yellowish = xiguaAddress.getYellowish(roomId);
                if (yellowish == null || yellowish.isEmpty() || yellowish.isBlank() || "true".equals(yellowish)){
                    return AjaxResult.fail(404,"禁止小黄车");
                }

//                int taskCount = 2;
//                CountDownLatch latch = new CountDownLatch(taskCount);

//                Thread task1 = new Thread(() -> {
//                    System.out.println("任务 1 开始");
//                    //获取直播人名
//                    videoName.set(xiguaAddress.getNickNameByPersonAddress(sec_uid));
//                    System.out.println("任务 1 完成");
//                    latch.countDown();
//                });

//                Thread task2 = new Thread(() -> {
//                    System.out.println("任务 2 开始");
//                    xiguaName.set(xiguaAddress.getXiGuaName(roomId));
//                    System.out.println("任务 2 完成");
//                    latch.countDown();
//                });
//                task1.start();
//                task2.start();
                // 等待所有任务完成
//                latch.await(20, TimeUnit.SECONDS);

//                if (videoName.get() == null){
//                    return AjaxResult.fail(404,"直播人地址解析错误");
//                }
                taskData.setRoomId(roomId);
//                taskData.setVideoName(videoName.get());
                xiguaName.set(xiguaAddress.getXiGuaName(roomId));

                if (xiguaName.get() != null){
                    taskData.setVideoName(xiguaName.get());
                    taskData.setVideoNameXiGua(xiguaName.get());

                }
                if (xiguaName.get() == null){
                    return AjaxResult.fail(404,"名字解析错误");
                }
            }



        if (StrUtil.isEmptyIfStr(taskData.getRoomId()) || StrUtil.isEmptyIfStr(taskData.getVideoName()) || !NumberUtil.isNumber(taskData.getRoomId())){
            return AjaxResult.fail(404,"RoomId 出错");
        }

        taskData.setBeginTimeFrom(DateUtil.date(Calendar.getInstance()).toString());
        taskData.setId(IdUtil.randomUUID());
        taskModel.setTask(taskData);

         return  AjaxResult.success();
    }



    @Auth(user = "1000")
    @GetMapping("/getTaskList")
    public AjaxResult getTaskList(){

        Long time = System.currentTimeMillis();

        for (int i = 0; i < taskDataList.size(); i++) {
            taskDataList.get(i).setNumberWorking(0);
            for (int j = 0; j < deviceDataList.size(); j++) {
               if (taskDataList.get(i).getId().equals(deviceDataList.get(j).getId()) && deviceDataList.get(j).getState() != null
                       && deviceDataList.get(j).getLastWorkingState() != null
                       && time- deviceDataList.get(j).getState()<1000*30
                       &&  deviceDataList.get(j).getState().equals(deviceDataList.get(j).getLastWorkingState())){
                   taskDataList.get(i).setNumberWorking(taskDataList.get(i).getNumberWorking()+1);
               }
            }
        }

        //缓存表中任务
        List<TaskData> tempTaskDataList =  taskMapper.selectAllTempTask();

//        log.info("tempTaskDataList：{}",tempTaskDataList);

         tempTaskDataList.addAll(taskDataList);

        return  AjaxResult.success(tempTaskDataList);

    }

    @Auth(user = "1000")
    @GetMapping("/getTempTaskList")
    public AjaxResult getTempTaskList(@PathParam("page")Integer page ,@PathParam("size") Integer size ){

        if (page>0&&size>0){
            return  AjaxResult.success(taskMapper.getTempTaskListCount().toString(),taskMapper.getTempTaskList((page-1)*size,size));
        }

        return AjaxResult.fail(-1,"");

    }

    @Auth(user = "1000")
    @GetMapping("/getHistoryTaskList")
    public AjaxResult getHistoryTaskList(@PathParam("page")Integer page ,@PathParam("size") Integer size ){

        if (page>0&&size>0){

            return  AjaxResult.success(taskMapper.getTaskListCount().toString(),taskMapper.getTaskList((page-1)*size,size));

        }

       return AjaxResult.fail(-1,"");
    }

    @Auth(user = "1000")
    @PostMapping("/deleteHistoryTask")
    public AjaxResult deleteHistoryTask(@RequestBody List<TaskData> taskDataList ){

        return AjaxResult.success(taskMapper.deleteHistoryTasks(taskDataList));

    }


    /* 1校验
     * 2删除任务 清空用户缓存积分
     *  清除设备列表数据  列表数据在删除了任务之后脚本请求接收任务 发现当前执行任务被删除 自动清除数据，
     * 或在执行的任务心跳找不到对应任务，脚本结束任务 重新请求接收任务也会自动清除数据
     * */
    //删除任务

    @Auth(user = "1000")
    @PostMapping("/deleteTask")
    public AjaxResult deleteTask(@RequestBody TaskData taskData){
        //1校验
        if (taskData.getId()==null || taskData.getId().isEmpty()){
           return AjaxResult.fail(-1,"请输入正确roomId");
        }
        if (isNumeric0(taskData.getId())){
            if (taskMapper.selectCountByIdTempTask(Integer.valueOf(taskData.getId())) == 1){
                taskMapper.deleteTempTask(taskData);
                return AjaxResult.success();
            }
        }
        else {
            for (int i=0; i<taskDataList.size();i++){
                if (taskData.getId().equals(taskDataList.get(i).getId())){
                    //删除任务总方法
                    Boolean  delete = taskModel.deleteTaskById(taskData.getId());
                    if (delete){
                        return AjaxResult.success();
                    }
                }
            }
        }
        return  AjaxResult.fail(404,"没有找到任务");
    }


    //容错表
    Map<String, Integer> checkMap = new HashMap<>();
    Map<String, Integer> checkMapYellow = new HashMap<>();
    //脚本状态链接
    @PostMapping("/checkState")
    public AjaxResult checkState(@RequestBody CheckInfo checkInfo,@PathParam("mid") String mid){
        //只有在脚本接收任务后才会请求该接口 每十秒记录一次任务状态
        //一般 参数 账号、设备唯一标识、设备昵称、直播间id、任务状态（在任务直播间或不在）、当前时间、md5校验
        String md5 = SecureUtil.md5(checkInfo.getCardNo()+checkInfo.getDeviceId()+checkInfo.getRoomId()+checkInfo.getTime()+checkInfo.getVideoDieOut()+checkInfo.getTaskState()+checkInfo.getId()+"sb1314520sbNB$$$$");
        if (!md5.equals(mid)){return  AjaxResult.fail(-1,"?????你在做什么,唱歌");}
        Long systemTime = System.currentTimeMillis();
        if ("true".equals(checkInfo.getVideoYellow()) &&
                SecureUtil.md5(checkInfo.getCardNo()+checkInfo.getDeviceId()+checkInfo.getRoomId()+checkInfo.getTime()
                                +checkInfo.getVideoDieOut()+checkInfo.getTaskState()+checkInfo.getId()+checkInfo.getVideoYellow()
                                +"sb1314520sbNB$$$$").equals(checkInfo.getMid2())){
            checkMapYellow.merge(checkInfo.getId(), 1, Integer::sum);
            for (int i = 0; i < taskDataList.size(); i++) {
                if (taskDataList.get(i).getId().equals(checkInfo.getId())){//找到任务直播间 删除他
                    Integer num = (int) (taskDataList.get(i).getNumberStatic()* 0.2);
                    if (checkMapYellow.get(checkInfo.getId())>num){
                        log.info("find the end room yellow deleteTaskById:{}", checkInfo);
                        taskModel.deleteTaskById(checkInfo.getId());
                        checkMapYellow.remove(checkInfo.getId());
                        break;
                    }
                }
            }
            return AjaxResult.fail(400,"任务失效");
        }
        else if (!StrUtil.isEmptyIfStr(checkInfo.getVideoDieOut())&&checkInfo.getVideoDieOut().equals("true")){//脚本发现直播间任务结束
            checkMap.merge(checkInfo.getId(), 1, Integer::sum);
            if (checkMap.get(checkInfo.getId())>10){
                for (int i = 0; i < taskDataList.size(); i++) {
                    if (taskDataList.get(i).getId().equals(checkInfo.getId())){//找到任务直播间 删除他
                        Integer num = (int) (taskDataList.get(i).getNumberStatic()* 0.4);
                        if (checkMap.get(checkInfo.getId())>num){
                            log.info("find the end room checkState deleteTaskById:{}", checkInfo);
                            //直播结束剩余时间 大于20分钟
                            if ( (Long.parseLong(taskDataList.get(i).getTime()) - systemTime)/60000 > 30){
                                //加入 预定列表
                                TaskData taskData = new TaskData();
                                taskData.setBeginTimeFrom(DateUtil.format(DateUtil.offset(DateUtil.date(), DateField.MINUTE, 4),"yyyy-MM-dd HH:mm:ss"));
                                taskData.setBeginTimeTo(taskDataList.get(i).getBeginTimeTo());
                                long duration = DateUtil.between( DateUtil.parse(taskData.getBeginTimeFrom()),DateUtil.parse(taskData.beginTimeTo), DateUnit.MINUTE);
                                taskData.setDuration(Long.toString(duration));
                                taskData.setNumber(taskDataList.get(i).getNumberStatic());
                                taskData.setNumberStatic(taskDataList.get(i).getNumberStatic());
                                taskData.setTime(taskDataList.get(i).getTime());
                                taskData.setPersonAddress(taskDataList.get(i).getPersonAddress());
                                taskData.setRoomAddress(taskDataList.get(i).getRoomAddress());
                                taskData.setIntegral(taskDataList.get(i).getIntegral());
                                taskMapper.addTempTask(taskData);
                            }
                            taskModel.deleteTaskById(checkInfo.getId());
                            checkMap.remove(checkInfo.getId());
                            break;
                        }
                    }
                }
                return AjaxResult.fail(400,"任务失效");
            }
        }

        //当脚本领取任务后还未进入直播间
        if (!checkInfo.getTaskState().equals("true")){
            // 任务有效 在任务中 但是还没进入直播间 //刷新设备在线状态
            for (int i = 0; i < deviceDataList.size(); i++) {
                if (deviceDataList.get(i).getDeviceId().equals(checkInfo.deviceId)) {
                    deviceDataList.get(i).setState(systemTime);
                    log.info("刷新设备在线状态{}", deviceDataList.get(i));
                    break;
                }
            }
            return AjaxResult.success();
        }
        //顺带查找当前任务积分/每分钟
        Long integral = 0L;
        //查询当前直播间列表 判断脚本发送的直播间任务是否有效
        for(int i=0;i<taskDataList.size();i++){
            if (taskDataList.get(i).getId().equals(checkInfo.getId())&&taskDataList.get(i).getIntegral()!=null&&taskDataList.get(i).getIntegral()>0 ){
                log.info("find the task taskDataList length:{}",taskDataList.size());
                integral= (long)(taskDataList.get(i).getIntegral()/6);
                break;
            }
        }
        if (integral.equals(0L)){return AjaxResult.fail(400,"任务失效");}
        //积分统计
        // 任务有效 且脚本发送 在任务直播间中
        boolean isWork = false;
        for (int i = 0; i < deviceDataList.size(); i++) {
            // 第一次发送请求接收到 workingTime
            if (deviceDataList.get(i).getDeviceId().equals(checkInfo.deviceId) && deviceDataList.get(i).getStartWorkingState() == null) {
                log.info("第一次找到 device");
                deviceDataList.get(i).setStartWorkingState(systemTime);
                deviceDataList.get(i).setLastWorkingState(systemTime);
                deviceDataList.get(i).setState(systemTime);
                deviceDataList.get(i).setDuration(0L);
                break;
            }
            else if (deviceDataList.get(i).getDeviceId().equals(checkInfo.deviceId)&& deviceDataList.get(i).getStartWorkingState() != null) {
                // 有效时间段请求
                long l = systemTime - deviceDataList.get(i).getLastWorkingState();
                if (l > 1000 * 20 || l < 1000 * 10) {
                    log.info("脚本发送时间无效,记录下当前时间从下次开始判断有效时间 {}", deviceDataList.get(i));
                }
                else {
                    deviceDataList.get(i).setDuration(l + deviceDataList.get(i).getDuration());
                    deviceDataList.get(i).setTodayTaskIntegral(integral+ deviceDataList.get(i).getTodayTaskIntegral()); // 今日积分
                    isWork=true;
                }
                deviceDataList.get(i).setLastWorkingState(systemTime);
                deviceDataList.get(i).setState(systemTime);
                break;
            }
        }
        //增加用户积分
        if (!integral.equals(0L)&&isWork){
            for (int i = 0; i <userListGlobal.size(); i++) {
                if (userListGlobal.get(i).getCardNo().equals(checkInfo.getCardNo())){
                    log.info("增加用户积分 cardNo:{},integral:{}",userListGlobal.get(i).getCardNo(),userListGlobal.get(i).getTempIntegral());
                    if (userListGlobal.get(i).getTempIntegral()==null ){ userListGlobal.get(i).setTempIntegral(0L); }
                    userListGlobal.get(i).setTempIntegral(integral + userListGlobal.get(i).getTempIntegral());
                    break;
                }
            }
            for (int i = 0; i <taskDataList.size(); i++) {
                if (taskDataList.get(i).getId().equals(checkInfo.getId())){
                    taskDataList.get(i).setCreatIntegral(integral+ taskDataList.get(i).getCreatIntegral());
                    break;
                }
            }
        }
        return AjaxResult.success();
    }



    /*
     *
     * jiao ben jie tu 上传接口
     *
     * */
    @Value("${server.port}")
    String  port;
    @PostMapping("/screenUpload")
    public AjaxResult qrUpload(@RequestParam(value = "file") MultipartFile multipartFile, @PathParam("roomId") String roomId,
                               @PathParam("videoName") String videoName , @PathParam("deviceId") String deviceId,
                               @PathParam("time") String time , @PathParam("mid1") String mid1 , @PathParam("mid2") String mid2,
                               HttpServletRequest request) throws IOException {

//        上传文件 校验  ？？
        // 1. 检查请求是否超时
        if (request.isAsyncStarted()) {
            return AjaxResult.fail(404,"请求超时");
        }
        String md5 = SecureUtil.md5(roomId+videoName+deviceId+mid1+time+"sb1314520sbNB$");

        log.info("md5:{},mid1:{},mid2:{},deviceId:{}",md5,mid1,mid2,deviceId);

        if (!md5.equals(mid2)){
            return  AjaxResult.fail(-1,"?????你在做什么,唱歌");
        }
        if (!DigestUtils.md5Hex(multipartFile.getBytes()).equals(mid1)){
            return  AjaxResult.fail(-1,"?????文件改了");
        }

        int state = 0;
        for (int i = 0; i < taskDataList.size(); i++) {
            if (taskDataList.get(i).getRoomId().equals(roomId) && taskDataList.get(i).getVideoName().equals(videoName)){
                state=1;
            }
        }
        if (state==0){return  AjaxResult.fail(-1,"1");}
           state = 0;

        for (int i = 0; i < deviceDataList.size(); i++) {
            if (deviceDataList.get(i).getDeviceId().equals(deviceId)){
                log.info("screenUpload find deviceId:{}",deviceDataList.get(i).getDeviceId());
                state = 1;
                break;
            }
        }

        if (state==0){return  AjaxResult.fail(-1,"2");}

        try {
            String name = file.adddeviceScreenImg(multipartFile,roomId,videoName,deviceId);
            if (name == null || name.length() == 0) {
                return AjaxResult.fail(404, "上传文件错误");
            }
            for (int i = 0; i < deviceDataList.size() ; i++) {
                if (deviceDataList.get(i).getDeviceId().equals(deviceId)){
                    deviceDataList.get(i).setScreenImgUrl(name);
                    break;
                }
            }
            log.info(name);
            return AjaxResult.success();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return AjaxResult.fail(-1, "出错");

    }

    public static boolean isNumeric0(String str) {

        for(int i=str.length();--i>=0;)
        {
        int chr=str.charAt(i);
        if(chr<48 || chr>57)
            return false;
        }
        return true;
    }

}




