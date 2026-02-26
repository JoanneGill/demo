package com.example.demo.Address;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.DefaultJavaScriptErrorListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * 西瓜(抖音)地址解析服务
 * 重构点：
 * 1. 去掉构造函数里直接启动浏览器，改为懒加载
 * 2. 增加配置开关 xigua.browser.enabled
 * 3. 使用 WebDriverManager 自动匹配浏览器驱动
 * 4. user-data-dir 使用随机临时目录，失败自动重试一次
 * 5. 统一 headless 模式（可配置）
 * 6. 增加 @PreDestroy 做清理
 */
@Slf4j
@Service
@Lazy
public class XiguaAddress {

    @Value("${xigua.browser.enabled:true}")
    private boolean browserEnabled;

    @Value("${xigua.browser.headless:false}")
    private boolean headless;

    @Value("${xigua.jiexi.filepath}")
    private String filePath;
    // 轮询索引
    private static final AtomicInteger currentIndex = new AtomicInteger(0);

    // 单例 ChromeDriver（低并发情况下可用；高并发请改为 ThreadLocal 或 池）
    private volatile ChromeDriver driver;

    private Path profileTempDir;
    private Path cacheTempDir;

    // ============= 懒加载获取 driver =============
    private ChromeDriver getDriver() {
        if (!browserEnabled) {
            throw new IllegalStateException("Browser feature disabled by configuration (xigua.browser.enabled = false).");
        }
        if (driver == null) {
            synchronized (this) {
                if (driver == null) {
                    driver = createChromeDriverWithRetry(2);
                }
            }
        }
        return driver;
    }

    private ChromeDriver createChromeDriverWithRetry(int maxAttempts) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return createChromeDriver();
            } catch (RuntimeException e) {
                last = e;
                log.warn("创建 ChromeDriver 第 {} 次失败: {}", attempt, e.getMessage());
                cleanupTempDirs(); // 清理可能遗留的目录
                sleepSilently(1200);
            }
        }
        throw last != null ? last : new RuntimeException("创建 ChromeDriver 失败(未知原因)");
    }

    private ChromeDriver createChromeDriver() {
        log.info("开始初始化 ChromeDriver (headless={}, enabled={})", headless, browserEnabled);
        Path profileTempDir = null;
        Path cacheTempDir = null;
        System.setProperty("webdriver.chrome.driver", "C:\\Program Files\\Google\\Chrome\\Application\\chromedriver.exe");
        try {
            ChromeOptions options = new ChromeOptions();
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);
            if (headless) {
                options.addArguments("--headless=new");
            }

            // 常用稳定参数（容器/无头友好）
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--remote-allow-origins=*");
//
//            // 移动端模拟
            Map<String, String> mobileEmulation = new HashMap<>();
            mobileEmulation.put("deviceName", "iPhone XR");
            options.setExperimentalOption("mobileEmulation", mobileEmulation);
//
//            // 创建唯一临时目录，确保使用字符串路径
//            profileTempDir = Files.createTempDirectory("xigua-chrome-profile-" + UUID.randomUUID());
//            cacheTempDir = Files.createTempDirectory("xigua-chrome-cache-" + UUID.randomUUID());
//            options.addArguments("--user-data-dir=" + profileTempDir.toAbsolutePath().toString());
//            options.addArguments("--disk-cache-dir=" + cacheTempDir.toAbsolutePath().toString());
//
//            log.info("使用 user-data-dir = {}", profileTempDir);
//            log.info("使用 disk-cache-dir = {}", cacheTempDir);

            ChromeDriver drv = new ChromeDriver(options);
            log.info("ChromeDriver 启动成功 (sessionId={})", drv.getSessionId());
            return drv;
        } catch (Exception e) {
            // 发生异常时清理刚创建的临时目录，避免残留锁或占用
//            try {
//                if (profileTempDir != null && Files.exists(profileTempDir)) {
//                    FileUtils.deleteDirectory(profileTempDir.toFile()); // 使用 commons-io 或自行删除递归
//                }
//                if (cacheTempDir != null && Files.exists(cacheTempDir)) {
//                    FileUtils.deleteDirectory(cacheTempDir.toFile());
//                }
//            } catch (IOException ex) {
//                log.warn("清理临时目录失败: {}", ex.getMessage());
//            }
            throw new RuntimeException("初始化 ChromeDriver 失败: " + e.getMessage(), e);
        }
    }

    private void sleepSilently(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    // ============= 资源清理 =============
    @PreDestroy
    public void destroy() {
        log.info("应用关闭，准备清理 ChromeDriver 与临时目录");
        if (driver != null) {
            try { driver.quit(); } catch (Exception ignored) {}
        }
        cleanupTempDirs();
    }

    private void cleanupTempDirs() {
        safeDeleteDir(profileTempDir);
        safeDeleteDir(cacheTempDir);
        profileTempDir = null;
        cacheTempDir = null;
    }

    private void safeDeleteDir(Path dir) {
        if (dir == null) return;
        try {
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                        });
            }
        } catch (Exception ignored) {}
    }

    // ================== 原有逻辑（小幅调整，仅在需要浏览器时调用 getDriver()） ==================

    public  JsonArray getIpAndPortList() throws IOException {
        String jsonContent = Files.readString(Path.of(filePath));
        return JsonParser.parseString(jsonContent).getAsJsonObject().get("jiexiIp").getAsJsonArray();
    }

    public  JsonArray getIpAndPortListYellowish() throws IOException {
        String jsonContent = Files.readString(Path.of(filePath));
        return JsonParser.parseString(jsonContent).getAsJsonObject().get("yellowish").getAsJsonArray();
    }

    public String getRoomId(String address) {
        try {
            HtmlPage page = getTimeByHtmlUnit(address);
            if (page == null) return "false";
            String reg = "(?<=reflow/).*?(?=\\?u_code)";
            Matcher matcher = Pattern.compile(reg).matcher(page.getBaseURI());
            if (matcher.find()) {
                return matcher.group();
            } else {
                List<HtmlElement> p = page.getBody().getElementsByAttribute("a", "class", "B3AsdZT9");
                // 这里似乎未使用 p，可补逻辑
            }
            return "false";
        } catch (Exception e) {
            log.error("getRoomId Exception:{}", e.toString());
            return "false";
        }
    }

    public String getPageSource(String address) {
        try {
            ChromeDriver d = getDriver();
            synchronized (d) { // 简单串行化（避免并发问题）
                d.get(address);
                return d.getPageSource();
            }
        } catch (Exception e) {
            log.error("getPageSource Exception: {}", e.getMessage());
            return null;
        }
    }

    public String getRoomIdByBrowser(String pageSource) {
        try {
            String[] arr = pageSource.split("roomIdStr\\\\\":\\\\\"");
            if (arr.length > 1) {
                String candidate = arr[1].split("\\\\")[0];
                if (candidate != null && !"0".equals(candidate) && NumberUtil.isNumber(candidate)) {
                    return candidate;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("getRoomIdByBrowser Exception: {}", e.getMessage());
            return null;
        }
    }

    public String getVideoNameByBrowser(String pageSource) {
        String[] arr = pageSource.split("\"nickname\\\\\":\\\\\"");
        if (arr.length > 1) {
            String videoName = arr[1].split("\\\\")[0];
            if (StrUtil.isNotBlank(videoName)) {
                return videoName;
            }
        }
        return null;
    }

    public String getVideoName(String address) {
        try {
            HtmlPage page = getTimeByHtmlUnit(address);
            if (page == null) return "false";
            UrlBuilder builder = UrlBuilder.ofHttp(page.getBaseURI(), CharsetUtil.CHARSET_UTF_8);
            String sec_uid;
            if (builder.getQuery() == null || StrUtil.isEmptyIfStr(builder.getQuery().get("sec_user_id"))) {
                String[] parts = builder.getPathStr().split("/");
                sec_uid = parts[parts.length - 1];
            } else {
                sec_uid = builder.getQuery().get("sec_user_id").toString();
            }
            String url = "https://www.iesdouyin.com/web/api/v2/user/info/?sec_uid=" + sec_uid + "&from_ssr=1";
            try (WebClient webClient = new WebClient(BrowserVersion.getDefault())) {
                String json = webClient.getPage(url).getWebResponse().getContentAsString(StandardCharsets.UTF_8);
                JSONObject obj = JSONUtil.parseObj(json);
                return JSONUtil.parseObj(obj.get("user_info")).getStr("nickname", "false");
            }
        } catch (Exception e) {
            log.error("getVideoName Exception: {}", e.getMessage());
            return "false";
        }
    }

    public HtmlPage getTimeByHtmlUnit(String url) throws IOException {
        if (StringUtils.isBlank(url)) return null;
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.addRequestHeader("User-Agent",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1");
        webClient.addRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        webClient.addRequestHeader("Accept-Language", "zh-CN,zh;q=0.9");
        webClient.addRequestHeader("X-Requested-With", "XMLHttpRequest");

        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setRedirectEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setTimeout(6000);
        webClient.setJavaScriptErrorListener(new MyJSErrorListener());
        webClient.setJavaScriptTimeout(5000);

        HtmlPage page = webClient.getPage(url);
        webClient.waitForBackgroundJavaScript(6000);
        return page;
    }

    public class MyJSErrorListener extends DefaultJavaScriptErrorListener {
        @Override public void scriptException(HtmlPage page, ScriptException scriptException) {}
        @Override public void timeoutError(HtmlPage page, long allowedTime, long executionTime) {}
        @Override public void malformedScriptURL(HtmlPage page, String url, MalformedURLException malformedURLException) {}
        @Override public void loadScriptError(HtmlPage page, URL scriptUrl, Exception exception) {}
        @Override public void warn(String message, String sourceName, int line, String lineSource, int lineOffset) {}
    }

    public String getRoomIdByPersonAddress(String sec_uid) {
        try {
            JsonArray jsonArray = getIpAndPortList();
            int index = currentIndex.updateAndGet(i -> i % jsonArray.size());
            for (int i = 0; i < jsonArray.size(); i++) {
                String ip = jsonArray.get(index).getAsString();
                Document doc = Jsoup.connect(ip + "/dy/getRoomIdByPersonAddress?sec_uid=" + sec_uid).get();
                String body = doc.body().html();
                if (StrUtil.isNotBlank(body)) return body;
                index = (index + 1) % jsonArray.size();
            }
            return null;
        } catch (IOException e) {
            log.error("getRoomIdByPersonAddress error {}", e.toString());
            return null;
        }
    }
    public String getTaskInfoBySecUid(String sec_uid) {
        try {
            JsonArray jsonArray = getIpAndPortList();
            System.out.println("sssssssssss"+ jsonArray.toString() +jsonArray.get(0).getAsString());
            int index = currentIndex.updateAndGet(i -> i % jsonArray.size());
            RestTemplate restTemplate = new RestTemplate();
            for (int i = 0; i < jsonArray.size(); i++) {
                String ip = jsonArray.get(index).getAsString();
                String response = restTemplate.getForObject(ip + "/dy/getTaskInfoBySecUid?sec_uid=" + sec_uid, String.class);
                if (StrUtil.isNotBlank(response)) return response;
                index = (index + 1) % jsonArray.size();
            }
            return null;
        } catch (IOException e) {
            log.error("getRoomIdByPersonAddress error {}", e.toString());
            return null;
        }
    }

    public String getYellowish(String roomId) {
        try {
            JsonArray jsonArray = getIpAndPortListYellowish();
            int index = currentIndex.get() % jsonArray.size();
            for (int i = 0; i < jsonArray.size(); i++) {
                String ip = jsonArray.get(index).getAsString();
                Document doc = Jsoup.connect(ip + "/dy/yellowish?roomId=" + roomId).get();
                String body = doc.body().html();
                if (StrUtil.isNotBlank(body)) return body;
                index = (index + 1) % jsonArray.size();
            }
            return null;
        } catch (IOException e) {
            log.error("getYellowish error {}", e.toString());
            return null;
        }
    }

    public String getsecuidBypersonAddress(String personAddress) {
        try {
            JsonArray jsonArray = getIpAndPortList();
            int index = currentIndex.updateAndGet(i -> (i + 1) % jsonArray.size());
            for (int i = 0; i < jsonArray.size(); i++) {
                String ip = jsonArray.get(index).getAsString();
                Document doc = Jsoup.connect(ip + "/dy/getsecuid?personAddress=" + personAddress).get();
                String body = doc.body().html();
                if (StrUtil.isNotBlank(body)) return body;
                index = (index + 1) % jsonArray.size();
            }
            return null;
        } catch (IOException e) {
            log.error("getsecuidBypersonAddress IOException:{}", e.toString());
            return null;
        }
    }

    public String getNickNameByPersonAddress(String sec_uid){
        try {
            JsonArray jsonArray = getIpAndPortList();
            int index = currentIndex.getAndUpdate(i -> (i + 1) % jsonArray.size());
            for (int i = 0; i < jsonArray.size(); i++) {
                String ip = jsonArray.get(index).getAsString();
                Document doc = Jsoup.connect(ip + "/dy/getnickNameBySec_uid?sec_uid=" + sec_uid).get();
                String body = doc.body().html();
                if (StrUtil.isNotBlank(body)) return body;
                index = (index + 1) % jsonArray.size();
            }
            return null;
        } catch (IOException e) {
            log.error("getNickNameByPersonAddress IOException:{}", e.toString());
            return null;
        }
    }

    public String getXiGuaName(String roomId) {
        try {
            ChromeDriver d = getDriver();
            synchronized (d) {
                d.get("https://webcast-open.douyin.com/open/webcast/reflow/?webcast_app_id=247160&room_id=" + roomId);
                new WebDriverWait(d, Duration.ofSeconds(20))
                        .until(ExpectedConditions.presenceOfElementLocated(By.className("saas-reflow-room-anchor-name")));
                return d.findElement(By.className("saas-reflow-room-anchor-name")).getAttribute("textContent");
            }
        } catch (Exception e) {
            log.error("getXiGuaName Exception: {}", e.getMessage());
            return null;
        }
    }
}