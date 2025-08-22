package com.example.demo.Config;

import com.example.demo.Data.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    //系统开放的端口只有  脚本接任务和心跳，上传 验证码，用户端登录和管理端登录 六个端口  ,其他端口均需要登录

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断用户登录
        HttpSession session = request.getSession(false);

//        log.info("LoginInterceptor start");

        //如果没有拦截 放行

        if(session == null || session.getAttribute(ApplicationVariable.SESSION_KEY_USERINFO) == null){
            //用户未登陆
//            response.sendRedirect("/login-1.html");
            return false;
        }

        if (!handler.getClass().isAssignableFrom(HandlerMethod.class)) {
            return true;
        }

        // 获取注解   如果不需要权限
        Auth auth = ((HandlerMethod) handler).getMethod().getAnnotation(Auth.class);
        if (auth == null) {
            return true;
        }

        // 从参数中取出用户身份并验证
        String admin = auth.user();
        //取出session中的信息
        User  user = (User)request.getSession().getAttribute(ApplicationVariable.SESSION_KEY_USERINFO);
        log.info("admin:{},User:{}",admin,user);
        if (admin.equals(user.getPermissions().toString())){
            return true;
        }

        //当代码执行到此处，说明用户访问了一个管理员接口
        // 可以做相应的操作

        return false;
    }

//    @Override
//    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
//        HttpSession session = request.getSession(false);
//        response.getWriter().print(session.getAttribute(ApplicationVariable.SESSION_KEY_USERINFO));
//
//    }



}
