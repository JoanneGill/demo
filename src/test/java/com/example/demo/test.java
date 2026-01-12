package com.example.demo;

import com.example.demo.Address.XiguaAddress;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

//@Slf4j
@SpringBootTest
public class test {


    @Autowired
    XiguaAddress xiguaAddress;

    @Test
    public void m(){
    xiguaAddress.getXiGuaName("7573290810708462351");
    }

}
