package com.example.demo.Data;


import lombok.Data;

@Data
public class EC {

    public String download_url;//: "http://baidu.com/aaa.iec",
    public String version;//: "1.1.0",
    public Boolean dialog;//: true,
    public String msg;//: "优化部分问题",
    public Boolean force;//: false
    public String ec_true_version;

}
