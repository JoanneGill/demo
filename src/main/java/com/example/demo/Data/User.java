package com.example.demo.Data;

import cn.hutool.core.date.DateTime;
import lombok.Data;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.List;
import java.util.Map;

@Data
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private String cardNo;
    private String password;
    private String realName;
    private String phone;
    private Integer permissions;
    private Integer state;
    private String email;
    private String qrUrl;
    private String qrUrlZFB;
    private String qrUrlOY;
    private List<DeviceData> devices;
    private Long   totalIntegral;
    private Long   freezeIntegral;
    private  Long   tempIntegral;
    private  Long   availableIntegral;
    private String  createdTime;
    private Integer workingDevices;
    private Integer waitDevices;
    private Integer ppDevices;
    private Integer ppWorkingDevices;
    private Integer ppWaitDevices;


}
