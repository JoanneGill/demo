package com.example.demo.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyConfig implements WebMvcConfigurer {
    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")//拦截所有的url地址
                .excludePathPatterns("/login-1.html")
                .excludePathPatterns("/page/admin/login-1.html")
                .excludePathPatterns("/css/**")
                .excludePathPatterns("/lib/**")
                .excludePathPatterns("/images/**")
                .excludePathPatterns("/js/**")
                .excludePathPatterns("/fonts/**")
                .excludePathPatterns("/login")//不拦截登录接口
                .excludePathPatterns("/login2")//不拦截登录接口
                .excludePathPatterns("/getCode")//不拦截验证码图片接口
                .excludePathPatterns("/Task/getTask**")//
                .excludePathPatterns("/Task/checkState")
                .excludePathPatterns("/EC/**")//版本下载地址
                .excludePathPatterns("/loginJiaoBen")//脚本登录
                .excludePathPatterns("/update/getUpdateECVersion")//更新版本地址
                .excludePathPatterns("/litemall/devices")
                .excludePathPatterns("/litemall/setTask")
                .excludePathPatterns("/Task/screenUpload")
                .excludePathPatterns("/litemall/checkTask")
                .excludePathPatterns("/ppTask/**");

    }
}