package com.example.demo.Controller;


import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONObject;
import com.example.demo.Config.AjaxResult;
import com.example.demo.Config.ApplicationVariable;
import com.example.demo.Data.User;
import com.example.demo.Mapper.UserMapper;

import com.example.demo.Model.IntegralModel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@Slf4j
public class LoginController {
 @Autowired
    UserMapper userMapper;
@Autowired
IntegralModel integralModel;

    private LineCaptcha lineCaptcha;

    @PostMapping("/login")
    public AjaxResult login(@RequestBody JSONObject jsonObject, HttpServletRequest request) {

            String cardNo =jsonObject.getStr("cardNo");

            String password =jsonObject.getStr("password");

            String code =jsonObject.getStr("captcha");

            String  localCode = (String) request.getSession().getAttribute(ApplicationVariable.USER_CODE);

        if (!StringUtils.hasLength(cardNo) || !StringUtils.hasLength(password) )
            return AjaxResult.fail(-1, "参数有误");

        if (localCode == null || !localCode.equals(code)){
            request.getSession().setAttribute(ApplicationVariable.USER_CODE,null);
            return AjaxResult.fail(-2,"验证码错误");
        }

          User user = userMapper.selectMyInfo(cardNo,password);

        if (user == null|| user.getState().equals(0)){
            request.getSession().setAttribute(ApplicationVariable.USER_CODE,null);
            return  AjaxResult.fail(-1,"用户名或密码错误");}
        //如果是管理员用户 不能用此接口登录
        if (!user.getPermissions().equals(1)){
            request.getSession().setAttribute(ApplicationVariable.USER_CODE,null);
            return AjaxResult.fail(-1,"用户名或密码错误");
        }


        // 将当前成功登录的用户信息存储到 session
        HttpSession session = request.getSession();

        session.setAttribute(ApplicationVariable.SESSION_KEY_USERINFO, user);

        //返回用户信息
        user.setPassword(null);

        return AjaxResult.success(user);


    }

    @PostMapping("/login2")
    public AjaxResult loginAdmin(@RequestBody JSONObject jsonObject, HttpServletRequest request) {

        if (request.getSession().getAttribute(ApplicationVariable.SESSION_KEY_USERINFO)!=null){
            return AjaxResult.fail(-1,"用户已登录");
        }


        String cardNo =jsonObject.getStr("cardNo");
        String password =jsonObject.getStr("password");
        String code =jsonObject.getStr("captcha");

        String  localCode = (String) request.getSession().getAttribute(ApplicationVariable.USER_CODE);
        if (!StringUtils.hasLength(cardNo) || !StringUtils.hasLength(password) )
            return AjaxResult.fail(-1, "参数有误");

        if (localCode == null || !localCode.equals(code)){

            request.getSession().setAttribute(ApplicationVariable.USER_CODE,null);

            return AjaxResult.fail(-1,"验证码错误");
        }


        User user = userMapper.selectMyInfo(cardNo,password);

        if (user == null|| user.getState().equals(0)){return  AjaxResult.fail(-1,"用户名或密码错误");}

        if (user.getPermissions().equals(1)){
            return AjaxResult.fail(-1,"用户名或密码错误");
        }





        // 将当前成功登录的用户信息存储到 session
        HttpSession session = request.getSession();

        session.setAttribute(ApplicationVariable.SESSION_KEY_USERINFO, user);

        //返回用户信息
        user.setPassword(null);

        return AjaxResult.success(user);


    }

   @GetMapping("/logout")
   public AjaxResult logout(HttpServletRequest request){

        if (request.getSession().getAttribute(ApplicationVariable.SESSION_KEY_USERINFO)!=null){
            request.setAttribute(ApplicationVariable.SESSION_KEY_USERINFO,null);
            return AjaxResult.success();
        }

        return AjaxResult.fail(-1,"");

   }

    @GetMapping("/loginJiaoBen")
    public AjaxResult loginJiaoBen(@Param("cardNo") String cardNo,@Param("password")String password,@Param("time")String time,@Param("deviceId")String deviceId,@Param("mid")String mid ){

        if (StrUtil.isEmptyIfStr(cardNo)||StrUtil.isEmptyIfStr(password)||StrUtil.isEmptyIfStr(time)||StrUtil.isEmptyIfStr(mid)){

            return AjaxResult.fail(-1,"");

        }

        //m校验
        String md5 = SecureUtil.md5(cardNo+password+deviceId+time+"sb1122sbbbb");
        if (!md5.equals(mid)){
            log.info("sssssssssssssss?????");
            return  AjaxResult.fail(-1,"?????你在做什么,唱歌");
        }


         User user = userMapper.selectMyInfo(cardNo,password);

        if ( user == null ||  user.getState().equals(0) ){

            return AjaxResult.fail(-1,"账密出错");

        }

        return AjaxResult.success();

    }






    @RequestMapping("/getCode")
    public void getCode(HttpServletRequest request,HttpServletResponse response) {

        HttpSession session = request.getSession();

        // 随机生成 4 位验证码
        RandomGenerator randomGenerator = new RandomGenerator("0123456789", 4);
        // 定义图片的显示大小
        lineCaptcha = CaptchaUtil.createLineCaptcha(100, 30);
        response.setContentType("image/jpeg");
        response.setHeader("Pragma", "No-cache");
        try {
            // 调用父类的 setGenerator() 方法，设置验证码的类型
            lineCaptcha.setGenerator(randomGenerator);
            // 输出到页面
            try {
                lineCaptcha.write(response.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            session.setAttribute(ApplicationVariable.USER_CODE,lineCaptcha.getCode());
            // 打印日志
            log.info("生成的验证码:{}", lineCaptcha.getCode());
            // 关闭流
            response.getOutputStream().close();
        } catch (IOException e) {
            e.printStackTrace();
        }











}



    @PostMapping("/setMyPassword")
    public AjaxResult setMyPassword(@RequestBody JSONObject jsonObject, HttpServletRequest httpServletRequest){

        String  password = jsonObject.getStr("password");
//        String cardNo = jsonObject.getStr("cardNo");
        if (StrUtil.isEmptyIfStr(password)){
            return AjaxResult.fail(-1,"请输入修改的密码");
        }
        String cardNo = ((User)httpServletRequest.getSession().getAttribute(ApplicationVariable.SESSION_KEY_USERINFO)).getCardNo();

        if ( StrUtil.isEmptyIfStr(cardNo) || !integralModel.changePassword(cardNo,password)){

            return AjaxResult.fail(-1,"操作失败");
        }

        return AjaxResult.success();

    }
}
