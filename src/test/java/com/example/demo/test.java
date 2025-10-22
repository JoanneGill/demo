package com.example.demo;

import org.apache.commons.lang3.StringUtils;
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


    public static void main(String[] args) {
        // 创建 HttpClient
        HttpClient client = HttpClient.newHttpClient();

        // 构建 HttpRequest
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://edith.xiaohongshu.com/api/sns/v3/user/info?user_id=67fdffb9000000000d00a83f&profile_page_head_exp=1&cny_source=&new_page_exp=0"))
                .GET()
                .header("xy-direction", "56")
                .header("x-b3-traceid", "75755bbd5b130e0f")
                .header("x-xray-traceid", "cd02acd92c2c0478827f97cfc0ac6058")
                .header("xy-scene", "fs=1&point=-1")
                .header("x-legacy-did", "3e14db4b-067c-34b9-962b-f702d532d286")
                .header("x-legacy-fid", "17604989311010afa2700df10945d24cba6d72eb92b4")
                .header("x-legacy-sid", "session.1761024474250189083678")
                .header("x-mini-gid", "7ccfb55abe3054c4fa123992458164c6d560b91947359b71772255f9")
                .header("x-mini-s1", "AAcAAAAB+HwLxeU9pXOHSD9ESnhUsL0ZAubRkP/Q0DlusIBalmWsSj8Ki+AbA/z27DyfCilJvAev34HulmI=")
                .header("x-mini-sig", "d1d8590eb869d2460edb424b9a077e43f2156182c5fa426c1ddd87ae554cfdbc")
                .header("x-mini-mua", "eyJhIjoiRUNGQUFGMDEiLCJjIjo4LCJrIjoiMzU2OGJiMWIyNjVlN2U5MDkwYjQ2NzA2MDVhMmQ5ZDZkMWY5OGQ0YjM0MGIyNDdlYWM2OTBhODM0NzBlOWIyNSIsInAiOiJhIiwicyI6IjgzZTUzNjE0YjBkYWQzZTk5MGJiZjUwM2U0N2M1NDBkZjcxOGJmNzUwZmQ1NjFiNjgxMzg4MTEwZTNhOTNiZTkxOWU4ZDhkNGVkMzlmNDM3YjljNzU1OTM5YTM5M2Q4ODU2NjA4M2M1NDczMmE5OTM3NDg4ZGVlODA1ZTdkODUxIiwidCI6eyJjIjowLCJkIjowLCJmIjowLCJzIjo0MDk4LCJ0IjowLCJ0dCI6W119LCJ1IjoiMDAwMDAwMDA1ZDgxNmFiYzM2MjdhYmZjM2Q1ZWYwOTE2N2JjMDdjOSIsInYiOiIyLjkuMzExIn0.YaoO5XrPGlklLWU75VLABLJRgrvN0kXEEazzM71r0Dw2MmaQZFoMJp2QzGn_dxc46hJkVhgUf3W7CoymYG_T2XJaaV6y7ztq0vGTQZmG8dSOvVW3-9ALkvTBE_mIZmp7tvMcXLtDhs0yMBouB9NMIfLFFuJgxrgOOjiD6JDpcHTAh-3X_UGSYWAxOpDdy8ypRUEBhrxspSyAoWW_L17I464O0pXEiC7WvWnBptVX2FUFd8msboDKY9eMRhZ3MQ9xUzFNr3pyAOxlCTFAe4LgdCXQtnxEzmsvQWTYSCLxO9nnnsXcres2wIoAjGJA18UyzHS0n20BrcAOkbWK-BU_HzMb9FL30W7guJIMSujjjZLkpj2zvFYdaxemXEhtEHpbNhYKXM9YJjrfTVAlL2HR77k5q-QUX5KqvAZxi7GjDFetdNAs1f0HEqzkuyi23WneT8L6gnx75IBGzQ48xO34p_fRgtzlVGI3Ob8fSFkAWw_a55_DNMDgpmAV-tfQkgUDQNGj5xGZAQ64_j7d3g2eVVTOepxsSHW3AJVtEneHRNhLF2OuzvusW3GvI3MXyI9zECqwYItBczoZKj43Uvx4lRaJkMc8XUE1FAZmhifuxWkV444S5J_qgYMnpuhycoQgu61l-kTLsQXeVPiBxeCC2uesMFo_JWUs369VFgsUfccjglLwh-f4I0ZWxV2hOsDd_x4TP__Vh6WDiYIwJza-xiHk0NaOarX054wOPIVwNpCCbjdYHAxjAM5ouldEtdLVDBQndNiTo64ihVwcQ_ecJr4AAa_w_DH_3RFRnSEQbNmZ_nBBtUr070RAJ32sU6AMlP9gMkVrhBVFt3IrXY1vOMHJ_n1WrO90gjzu8RHy_D0ww82NBYA3wZDxu137td2GqD_jxWolP5TSIzdElGAnMmmLyfD_ivtXgEJZ8BP9eRdNY1S6S8dCBLXs9N7-GLVQRuQwPSgyf9djKOMljgFKNWiKuEdQDAOf00pYT67erwEihFwRmnkpKmza2wUZS-BSTP7a8qwZWefqjE3W2ZPny8wwmojQZCR-vqo0msWkoqkr8raOIB4n4O6lV3a-NpZxSBlTa1QBjI_VpJmoVG9szsTPWgvqBAzNUrNvJZMZKVuaf8KBLBSF1egkc5G8gicb8PX3jVHOEnLnmhgW4zVVtQ.")
                .header("xy-common-params", "fid=17604989311010afa2700df10945d24cba6d72eb92b4&gid=7ccfb55abe3054c4fa123992458164c6d560b91947359b71772255f9&device_model=phone&tz=Asia%2FShanghai&channel=Xiaomi&versionName=9.4.0&deviceId=3e14db4b-067c-34b9-962b-f702d532d286&platform=android&sid=session.1761024474250189083678&identifier_flag=0&cpu_abi=&nqe_score=&project_id=ECFAAF&x_trace_page_current=&lang=zh-Hans&app_id=ECFAAF01&uis=light&teenager=0&active_ctry=CN&cpu_name=Qualcomm+Technologies%2C+Inc+SM7225&dlang=zh&data_ctry=CN&SUE=1&launch_id=1761026353&device_level=&origin_channel=Xiaomi&overseas_channel=0&mlanguage=zh_cn&folder_type=none&auto_trans=0&t=1761026354&build=9040805&holder_ctry=CN&did=93c9d64da7e45da9b7f07e57d9a8f85c")
                .header("user-agent", "Dalvik/2.1.0 (Linux; U; Android 12; Reno 11 Pro Build/TP1A.251020.001) Resolution/1080*2400 Version/9.4.0 Build/9040805 Device/(OPPO;Reno 11 Pro) discover/9.4.0 NetType/WiFi")
                .header("referer", "https://app.xhs.cn/")
                .header("shield", "XYAAQAAwAAAAEAAABTAAAAUzUWEe0xG1IbD9/c+qCLOlKGmTtFa+lG438OeOFeRKhBkIW0yuRjHp34+ecMz8MujsR+2KQwQgwYFmWKML733n9ijuKzUDjtdPSwpXX6VDH1MjGI")
                .header("xy-platform-info", "platform=android&build=9040805&deviceId=3e14db4b-067c-34b9-962b-f702d532d286")
                .header("accept-encoding", "gzip")
                .build();

        try {
            // 发送请求并接收响应
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response Body: " + response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
