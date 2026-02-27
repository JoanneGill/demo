package com.example.demo.Config;


import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回对象
 */
@Data
public class AjaxResult implements Serializable {
    private int code;
    private String msg;
    private Object data;

    /**
     * 返回成功
     * @param data
     * @return
     */
    public static AjaxResult success(Object data){
        AjaxResult ajaxResult = new AjaxResult();
        ajaxResult.setCode(200);
        ajaxResult.setMsg("");
        ajaxResult.setData(data);
        return ajaxResult;
    }
    public static AjaxResult success(String msg,Object data){
        AjaxResult ajaxResult = new AjaxResult();
        ajaxResult.setCode(200);
        ajaxResult.setMsg(msg);
        ajaxResult.setData(data);
        return ajaxResult;
    }

    public static AjaxResult success(){
        AjaxResult ajaxResult = new AjaxResult();
        ajaxResult.setCode(200);
        ajaxResult.setMsg("");
        ajaxResult.setData("");
        return ajaxResult;
    }
    public static AjaxResult success(Object data,String msg){
        AjaxResult ajaxResult = new AjaxResult();
        ajaxResult.setCode(200);
        ajaxResult.setMsg(msg);
        ajaxResult.setData(data);
        return ajaxResult;
    }

    /**
     * 返回失败数据
     * @param code
     * @param msg
     * @return
     */
    public static AjaxResult fail(Integer code,String msg){
        AjaxResult ajaxResult = new AjaxResult();
        ajaxResult.setCode(code);
        ajaxResult.setMsg(msg);
        ajaxResult.setData("");
        return ajaxResult;
    }
    public static AjaxResult fail(String message,String data){
        AjaxResult ajaxResult = new AjaxResult();
        ajaxResult.setCode(404);
        ajaxResult.setMsg(message);
        ajaxResult.setData(data);
        return ajaxResult;
    }

    public static AjaxResult fail(Integer code,String msg,Object data){
        AjaxResult ajaxResult = new AjaxResult();
        ajaxResult.setCode(code);
        ajaxResult.setMsg(msg);
        ajaxResult.setData(data);
        return ajaxResult;
    }

    public static AjaxResult error(String msg){
        AjaxResult ajaxResult = new AjaxResult();
        ajaxResult.setCode(500);
        ajaxResult.setMsg(msg);
        ajaxResult.setData(null);
        return ajaxResult;
    }
}
