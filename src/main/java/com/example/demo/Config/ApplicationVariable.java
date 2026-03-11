package com.example.demo.Config;

/**
 * 全局变量类
 */
public class ApplicationVariable {

    /**
     * 存放当前登录用户的session key
     */
    public static final String SESSION_KEY_USERINFO = "SESSION_KEY_USERINFO";

    //用户状态  锁
    public static final Integer USER_STATE_LOCK = 2;

    //用户状态  不锁
    public static final Integer USER_STATE_UNLOCK = 1;

    //机会

    public static final Integer USER_STATE_CHANCE = 5;

    public static final String USER_CODE =  "code";

    //排序
    public static final String ARTICLE_ORDER_BY_ID = "id";
    public static final String ARTICLE_ORDER_BY_CREATETIME = "createtime";
    public static final String ARTICLE_ORDER_BY_RCOUNT = "rcount";


    public static final String PP_TASK_CLAIM_STATUS_CLAIMED = "claimed";    // 任务中
    public static final String PP_TASK_CLAIM_STATUS_SUCCESS = "success";   // 成功
    public static final String PP_TASK_CLAIM_STATUS_FAILED = "fail";      // 失败
    public static final String PP_TASK_CLAIM_STATUS_EXPIRED = "expired";  //超时
    public static final String PP_TASK_CLAIM_STATUS_DONE = "done";  //超时





}
