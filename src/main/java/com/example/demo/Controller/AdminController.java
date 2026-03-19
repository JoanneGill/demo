package com.example.demo.Controller;


import cn.hutool.Hutool;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.example.demo.Config.AjaxResult;
import com.example.demo.Config.ApplicationVariable;
import com.example.demo.Config.Auth;
import com.example.demo.Data.*;
import com.example.demo.Mapper.IntegralMapper;
import com.example.demo.Mapper.UserMapper;
import com.example.demo.Model.DevicesModel;
import com.example.demo.Model.IntegralModel;

import com.example.demo.Model.UserModel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.demo.Config.ApplicationVariable.*;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    IntegralMapper integralMapper;

    @Autowired
    UserMapper userMapper;
    @Autowired
    IntegralModel integralModel;
    @Autowired
    DevicesModel devicesModel;
    @Autowired
    UserModel userModel;

    //在线任务列表
    List<TaskData> taskDataListGlobe = GlobalVariablesSingleton.getInstance().getTaskDataArrayList();


    //在线设备对象列表
    List<DeviceData> deviceDataListGlobe = GlobalVariablesSingleton.getInstance().getDeviceDataArrayList();

    //用户列表
    List<User> userListGlobe = GlobalVariablesSingleton.getInstance().getUsers();

    @Auth(user = "1000")
    @GetMapping("/taskDeviceList")
    public AjaxResult getTaskDeviceList(@PathParam("id") String id ){

        if (id == null || id.length() == 0) return  AjaxResult.fail(-1,"直播间id为空");

        List<DeviceData> deviceDataList = new CopyOnWriteArrayList<>();

        deviceDataListGlobe.stream().parallel().forEach(deviceData -> {
            if (id.equals(deviceData.getId())){

                deviceDataList.add(deviceData);

            }
        } );

        return AjaxResult.success(deviceDataList);
    }
        /*
        *
        * 获取用户申请兑换积分列表
        *
        * */
    @Auth(user = "1000")
    @PostMapping("/exchangeIntegralList")
    public AjaxResult getExchangeIntegralList(@RequestBody ExchangeIntegral exchangeIntegral){


          List<ExchangeIntegral> integralList = integralMapper.selectExchangeIntegralList(exchangeIntegral);

          Integer total = integralMapper.selectExchangeIntegralListTotal(exchangeIntegral);

          Pager<ExchangeIntegral> pager = new Pager<>();

          pager.setTotal(total);

          pager.setData(integralList);


        return AjaxResult.success(pager );
    }

    /*
    *
    * 同意用户积分申请
    *
    * */
    @Auth(user = "1000")
    @GetMapping("/agreeExchangeIntegral")
    public AjaxResult agreeExchangeIntegral(@PathParam("cardNo")String cardNo,@PathParam("id") BigInteger id ){

        return AjaxResult.success( integralModel.agreeExchangeIntegral(id) );

    }

    @Auth(user = "1000")
    @GetMapping("/disAgreeExchangeIntegral")
    public AjaxResult disAgreeExchangeIntegral(@PathParam("cardNo")String cardNo,@PathParam("id") BigInteger id ){

        return AjaxResult.success( integralModel.disAgreeExchangeIntegral(id));

    }

    /*
     *
     * 开户
     *
     * */
    @Auth(user = "1000")
    @PostMapping("/creatAccount")
    public AjaxResult creatAccount(@RequestBody JSONObject jsonObject){
          String cardNo = jsonObject.getStr("cardNo");
          String password = jsonObject.getStr("password");
          String realName =  jsonObject.getStr("realName");

        if (StrUtil.isEmptyIfStr(cardNo)|| StrUtil.isEmptyIfStr(password) || StrUtil.isEmptyIfStr(realName)  ){ return  AjaxResult.fail(-1,"不能为空");}

        if (userMapper.selectMyInfoByCardNo(cardNo)!=null ){ return  AjaxResult.fail(-1,"账号已存在");}

        User user =new User();
             user.setCardNo(cardNo);
             user.setPassword(password);
             user.setRealName(realName);

        return AjaxResult.success( userMapper.addUser(user) );

    }
    /*
     *
     * 用户信息 列表 在线设备 空闲设备 今日生成积分  从数据库查出所有用户 再从缓存中查出有设备在线的用户 增加今日积分   统计账号在线设备数量和不在线设备数量
     *
     * */
    @Auth(user = "1000")
    @PostMapping("/userList")
    public AjaxResult getUserList(@RequestBody User user){

        if (user.getState() == null){ user.setState(1);}
      List<User> users =  userMapper.selectUserList(user);
      Long currentTime =System.currentTimeMillis();
    //从数据库取出的临时积分 加上缓存中用户的临时积分
        for (int i = 0; i < userListGlobe.size(); i++) {

//            log.info("userList:{},userListSize:{}",userListGlobe.get(i),userListGlobe.size());
            for (int j = 0; j < users.size(); j++) {
                if (userListGlobe.get(i).getCardNo().equals(users.get(j).getCardNo())){
                   users.get(j).setTempIntegral(users.get(j).getTempIntegral()+userListGlobe.get(i).getTempIntegral());
                }
            }
        }

    //统计在线设备数量 和空闲设备数量
        for (int i = 0; i < users.size(); i++) {
            String cardNo = users.get(i).getCardNo();
            Integer workingDevices = 0;
            Integer waitDevices =0;

            for (int j = 0; j < deviceDataListGlobe.size(); j++) {

                if (cardNo.equals(deviceDataListGlobe.get(j).getCardNo())){

                    if (deviceDataListGlobe.get(j).getState()+1000*30 > currentTime && deviceDataListGlobe.get(j).getState().equals(deviceDataListGlobe.get(j).getLastWorkingState() ) ){

                        workingDevices++;

                    }
                    else if (deviceDataListGlobe.get(j).getState()+1000*60 > currentTime){
                        waitDevices++;
                    }
                }
            }

            users.get(i).setWaitDevices(waitDevices);
            users.get(i).setWorkingDevices(workingDevices);
        }

        return AjaxResult.success(users);


    }
    @Auth(user = "1000")
    @GetMapping("/welcomeInfo")
    public AjaxResult getwelcomeInfo(){
        Long currentTime = System.currentTimeMillis();
        //统计全部在线设备数量 和空闲设备数量
        Integer workingDevices = 0;
        Integer waitDevices =0;
        Long todayTotalIntegral = 0L;
        int ppWaitDevices = 0;
        int ppAllDoDevices = 0;
        int ppWaitDoDevices = 0;
        int ppNotDoDevices = 0;
        int ppWorkingDevices = 0;
        Long leastCurrentTime =   currentTime -1000*60;
        for (int i = 0; i < deviceDataListGlobe.size(); i++) {
            if (deviceDataListGlobe.get(i).getId()!=null  && !deviceDataListGlobe.get(i).getId().equals("0") && deviceDataListGlobe.get(i).getState()+1000*30 > currentTime && deviceDataListGlobe.get(i).getState().equals(deviceDataListGlobe.get(i).getLastWorkingState() ) ){
                workingDevices++;
            }
            else if (deviceDataListGlobe.get(i).getState() > leastCurrentTime){
                waitDevices++;
            }
            if (deviceDataListGlobe.get(i).getPpClaimTime() >leastCurrentTime &&(deviceDataListGlobe.get(i).getPpClaimState()==null||deviceDataListGlobe.get(i).getPpClaimState().isEmpty())){
                ppWaitDevices++;
            }
            if (deviceDataListGlobe.get(i).getPpClaimState()!=null&&deviceDataListGlobe.get(i).getPpClaimState().equals(PP_TASK_CLAIM_STATUS_CLAIMED)){
                ppWorkingDevices++;
            }
            if (deviceDataListGlobe.get(i).getPpModel()!=null && deviceDataListGlobe.get(i).getPpModel().equals(PP_TASK_DEVICE_PP_MODEL_ALL_DO)){
                ppAllDoDevices++;
            }
            else if (deviceDataListGlobe.get(i).getPpModel()!=null && deviceDataListGlobe.get(i).getPpModel().equals(PP_TASK_DEVICE_PP_MODEL_WAIT_DO)){
                ppWaitDoDevices++;
            }
            else if (deviceDataListGlobe.get(i).getPpModel()!=null && deviceDataListGlobe.get(i).getPpModel().equals(PP_TASK_DEVICE_PP_MODEL_NOT_DO)){
                ppNotDoDevices++;
            }
        }

        //统计今日生成总积分
        for (int i = 0; i < userListGlobe.size(); i++) {
            todayTotalIntegral = todayTotalIntegral + userListGlobe.get(i).getTempIntegral();
        }
        todayTotalIntegral= todayTotalIntegral+  userMapper.selectTodayAllIntegral();

        //今日待审核和已审核 积分统计
         HashMap<String, BigDecimal> hashMap = integralMapper.selectTodayExchangeAndAlreadExchangeIntegral();
         JSONObject jsonObject =   new JSONObject();

         jsonObject.set("workingDevices",workingDevices);
         jsonObject.set("waitDevices",waitDevices);
         jsonObject.set("todayTotalIntegral",todayTotalIntegral);
        jsonObject.set("ppWaitDevices",ppWaitDevices); //pp在线设备
        jsonObject.set("ppAllDoDevices",ppAllDoDevices); //pp全部做设备
        jsonObject.set("ppWaitDoDevices",ppWaitDoDevices); // pp等待做设备
        jsonObject.set("ppNotDoDevices",ppNotDoDevices); // pp不做设备
        jsonObject.set("ppWorkingDevices",ppWorkingDevices); //pp任务中设备
         if (hashMap != null){
             jsonObject.set("todayExchangeIntegral",hashMap.get("todayExchangeIntegral"));
             jsonObject.set("todayAlreadyExchangeIntegral",hashMap.get("todayAlreadyExchangeIntegral"));
         }

         return AjaxResult.success(jsonObject);
        }

    @Auth(user = "1000")
    @PostMapping("/setUserTempIntegral")
    public AjaxResult setUserTempIntegral(@RequestBody JSONObject jsonObject){
        Long  tempIntegral = jsonObject.getLong("tempIntegral");
        String cardNo = jsonObject.getStr("cardNo");
        if (StrUtil.isEmptyIfStr(tempIntegral)){
            return AjaxResult.fail(-1,"请输入修改的积分");
        }
        if ( !integralModel.changeTempIntegral(cardNo,tempIntegral)){

            return AjaxResult.fail(-1,"操作失败");
        }

        return AjaxResult.success();

        }

    @Auth(user = "1000")
    @PostMapping("/setUserIntegral")
    public AjaxResult setUserIntegral(@RequestBody JSONObject jsonObject){
        Long  tempIntegral = jsonObject.getLong("tempIntegral");
        String cardNo = jsonObject.getStr("cardNo");

        if (StrUtil.isEmptyIfStr(tempIntegral)){
            return AjaxResult.fail(-1,"请输入修改的积分");
        }

        if ( !integralModel.changeIntegral(cardNo,tempIntegral)){
            return AjaxResult.fail(-1,"操作失败");
        }

        return AjaxResult.success();

    }





    @Auth(user = "1000")
    @PostMapping("/setUserPassword")
    public AjaxResult setUserPassword(@RequestBody JSONObject jsonObject){

        String  password = jsonObject.getStr("password");
        String cardNo = jsonObject.getStr("cardNo");
        if (StrUtil.isEmptyIfStr(password)){
            return AjaxResult.fail(-1,"请输入修改的密码");
        }
        if (StrUtil.isEmptyIfStr(cardNo)){
            return AjaxResult.fail(-1,"请输入修改的账号");
        }
        if ( !integralModel.changePassword(cardNo,password)){

            return AjaxResult.fail(-1,"操作失败");
        }

        return AjaxResult.success();

    }

    @Auth(user = "1000")
    @GetMapping("/userDeviceList")
    public AjaxResult getUserDeviceList(@PathParam("cardNo") String cardNo, @PathParam("state") String state ){

      if (StrUtil.isEmptyIfStr(cardNo)||StrUtil.isEmptyIfStr(state)){return  AjaxResult.fail(-1,"");}

      List<DeviceData> deviceDataList = devicesModel.findDevices(cardNo,state);

      return AjaxResult.success(deviceDataList);

    }


    @Auth(user = "1000")
    @GetMapping("/deleteUser")
    public AjaxResult deleteUser(@PathParam("cardNo") String cardNo ){

        if (StrUtil.isEmptyIfStr(cardNo)){return  AjaxResult.fail(-1,"");}

        for (int i = 0; i < userListGlobe.size(); i++) {
            if (cardNo.equals(userListGlobe.get(i).getCardNo())){
                userListGlobe.get(i).setState(0);
            }
        }

        return AjaxResult.success(userModel.setUserState(cardNo,0));

    }

    @Auth(user = "1000")
    @PostMapping("/setTaskNumber")
    public AjaxResult setTaskNumber(@RequestBody TaskData taskData){

        if (StrUtil.isEmptyIfStr(taskData.getId())){
            return AjaxResult.fail(-1,"Id is not null");
        }

        if (!StrUtil.isEmptyIfStr(taskData.getNumber())){
        for (int i = 0; i < taskDataListGlobe.size() ; i++) {
           if (taskData.getId().equals(taskDataListGlobe.get(i).getId())){
               if (taskData.getNumber()+taskDataListGlobe.get(i).getNumber()>0){
                   taskDataListGlobe.get(i).setNumber(taskData.getNumber()+taskDataListGlobe.get(i).getNumber());
                   taskDataListGlobe.get(i).setNumberStatic(taskDataListGlobe.get(i).getNumberStatic()+taskData.getNumber());
               }
           }}
        }
        if (!StrUtil.isEmptyIfStr(taskData.getBeginTimeTo())){
            for (int i = 0; i < taskDataListGlobe.size() ; i++) {
                if (taskData.getId().equals(taskDataListGlobe.get(i).getId()) && DateUtil.compare(DateUtil.parse(taskDataListGlobe.get(i).getBeginTimeTo()),DateUtil.parse(taskData.getBeginTimeTo()))<0){

                  taskDataListGlobe.get(i).setBeginTimeTo(taskData.getBeginTimeTo());

                  taskDataListGlobe.get(i).setTime(String.valueOf(DateUtil.parse(taskData.beginTimeTo).getTime()));
                } }
        }

        return AjaxResult.success();

    }

    @Auth(user = "1000")
    @GetMapping("/getOneDayTotalIntegral")
    public AjaxResult getOneDayTotalIntegral(@PathParam(value = "searchDate") String searchDate){
        return AjaxResult.success(integralModel.getOneDayTotalIntegral(searchDate));
    }



    @Auth(user = "1000")
    @GetMapping("/getAllDevices")
    public AjaxResult getAllDevices(){
        return AjaxResult.success(deviceDataListGlobe);
    }


}


























