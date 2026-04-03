package com.example.demo.Service;

import com.example.demo.Config.YlGoodsConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class YLmall {

    private static final Logger log = LoggerFactory.getLogger(YLmall.class);
    private static final int HOURS = 24;

    public String address;
    public String appId;
    public String appSecret;

    @Value("${yl.config.path.win}")
    private String windowsPath;

    @Value("${yl.config.path.linux}")
    private String unixPath;

    /**
     * 单个 YL 站点（一个 address/appId/appSecret）及其下的商品与价格配置。
     */
    public static class Target {
        private String key;
        private String address;
        private String appId;
        private String appSecret;
        /** 仅当未使用 {@code goods} 数组时兼容旧字段 */
        private Integer legacyGoodsId;
        private List<YlGoodsConfig> goods = Collections.emptyList();

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }


        public void setLegacyGoodsId(Integer legacyGoodsId) {
            this.legacyGoodsId = legacyGoodsId;
        }

        public List<YlGoodsConfig> getGoods() {
            return goods;
        }

        public void setGoods(List<YlGoodsConfig> goods) {
            this.goods = goods != null ? goods : Collections.emptyList();
        }

        public YlGoodsConfig findGoods(int ylGoodsId) {
            if (goods == null) {
                return null;
            }
            for (YlGoodsConfig g : goods) {
                if (g.getYlGoodsId() == ylGoodsId) {
                    return g;
                }
            }
            return null;
        }
    }

    private volatile List<Target> targets = Collections.emptyList();

    @PostConstruct
    public void init() {
        final String osName = System.getProperty("os.name", "").toLowerCase();
        final String filePath = osName.contains("win") ? windowsPath : unixPath;

        // 每次访问时都重新读取配置：通过自定义 AbstractList 在 size/get/iterator 等调用时重新加载文件
        this.targets = new java.util.AbstractList<Target>() {
            private volatile List<Target> cached = Collections.emptyList();

            private synchronized void reload() {
                try {
                    String jsonString = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
                    JsonObject root = new JsonParser().parse(jsonString).getAsJsonObject();

                    List<Target> parsedTargets = new ArrayList<>();

                    JsonElement asArray = root.get("ylConfig");
                    if (asArray != null && asArray.isJsonArray()) {
                        parseTargetArray(asArray.getAsJsonArray(), parsedTargets);
                    }
                    if (parsedTargets.isEmpty()) {
                        JsonElement ylConfigsEl = root.get("ylConfigs");
                        if (ylConfigsEl != null && ylConfigsEl.isJsonArray()) {
                            parseTargetArray(ylConfigsEl.getAsJsonArray(), parsedTargets);
                        }
                    }
                    if (parsedTargets.isEmpty()) {
                        JsonElement single = root.get("ylConfig");
                        if (single != null && single.isJsonObject()) {
                            parseLegacySingleYlConfig(single.getAsJsonObject(), parsedTargets);
                        }
                    }

                    cached = Collections.unmodifiableList(parsedTargets);
                    // 同步默认凭证（兼容旧逻辑）
                    syncLegacyDefaultCredentials(parsedTargets);
                    log.info("YL config reloaded, {} target(s)", parsedTargets.size());
                } catch (Exception e) {
                    log.error("Failed to load YL config file: {}", filePath, e);
                    cached = Collections.emptyList();
                }
            }

            private void ensureLoaded() {
                // 每次访问都强制重新加载，保证拿到最新配置
                reload();
            }

            @Override
            public Target get(int index) {
                ensureLoaded();
                return cached.get(index);
            }

            @Override
            public int size() {
                ensureLoaded();
                return cached.size();
            }

            @Override
            public java.util.Iterator<Target> iterator() {
                ensureLoaded();
                return cached.iterator();
            }
        };
        log.info("YL config will be read from {} on each access", filePath);
    }

    /**
     * 根据 appId 在给定的 target 列表中查找并返回匹配的 Target。
     * 若未找到或参数无效则返回 null。
     */
    public static Target findByAppId(List<Target> targets, String appId) {
        if (targets == null || appId == null) {
            return null;
        }
        for (Target t : targets) {
            if (appId.equals(t.getAppId())) {
                return t;
            }
        }
        return null;
    }

    private void syncLegacyDefaultCredentials(List<Target> parsedTargets) {
        if (parsedTargets == null || parsedTargets.isEmpty()) {
            return;
        }
        Target first = parsedTargets.get(0);
        this.address = first.getAddress();
        this.appId = first.getAppId();
        this.appSecret = first.getAppSecret();
    }

    private static void parseTargetArray(JsonArray arr, List<Target> out) {
        for (int i = 0; i < arr.size(); i++) {
            JsonElement el = arr.get(i);
            if (!el.isJsonObject()) {
                continue;
            }
            Target t = parseTargetObject(el.getAsJsonObject(), i);
            if (t != null) {
                out.add(t);
            }
        }
    }

    private static Target parseTargetObject(JsonObject o, int index) {
        Target t = new Target();
        if (o.has("key")) {
            t.setKey(o.get("key").getAsString());
        }
        if (o.has("address")) {
            t.setAddress(o.get("address").getAsString());
        }
        if (o.has("appId")) {
            t.setAppId(o.get("appId").getAsString());
        }
        if (o.has("appSecret")) {
            t.setAppSecret(o.get("appSecret").getAsString());
        }
        if (o.has("goodsId") && !o.get("goodsId").isJsonNull()) {
            try {
                t.setLegacyGoodsId(o.get("goodsId").getAsInt());
            } catch (Exception ignore) {
            }
        }
        if (t.getKey() == null || t.getKey().trim().isEmpty()) {
            t.setKey("target-" + (index + 1));
        }
        List<YlGoodsConfig> goodsList = parseGoodsArray(o);
        if (!goodsList.isEmpty()) {
            t.setGoods(Collections.unmodifiableList(goodsList));
        }
        if (t.getAddress() == null || t.getAppId() == null || t.getAppSecret() == null) {
            return null;
        }
        return t;
    }

    private static void parseLegacySingleYlConfig(JsonObject jsonObject, List<Target> out) {
        try {
            Target t = new Target();
            t.setKey("default");
            t.setAddress(jsonObject.get("address").getAsString());
            t.setAppId(jsonObject.get("appId").getAsString());
            t.setAppSecret(jsonObject.get("appSecret").getAsString());
            if (jsonObject.has("goodsId") && !jsonObject.get("goodsId").isJsonNull()) {
                try {
                    t.setLegacyGoodsId(jsonObject.get("goodsId").getAsInt());
                } catch (Exception ignore) {
                }
            }
            List<YlGoodsConfig> goodsList = parseGoodsArray(jsonObject);
            if (!goodsList.isEmpty()) {
                t.setGoods(Collections.unmodifiableList(goodsList));
            }
            out.add(t);
        } catch (Exception e) {
            log.error("Failed parsing legacy ylConfig object", e);
        }
    }

    private static List<YlGoodsConfig> parseGoodsArray(JsonObject parent) {
        List<YlGoodsConfig> list = new ArrayList<>();
        JsonElement goodsEl = parent.get("goods");
        if (goodsEl == null || !goodsEl.isJsonArray()) {
            return list;
        }
        JsonArray arr = goodsEl.getAsJsonArray();
        for (int i = 0; i < arr.size(); i++) {
            JsonElement el = arr.get(i);
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject go = el.getAsJsonObject();
            if (!go.has("id")) {
                log.warn("goods[{}] missing id, skipped", i);
                continue;
            }
            int ylId;
            try {
                ylId = go.get("id").getAsInt();
            } catch (Exception e) {
                log.warn("goods[{}] invalid id, skipped", i);
                continue;
            }
            String dyPool = parseDyPool(go);

            BigDecimal[] prices = null;
            BigDecimal[] integrals = null;
            if (go.has("hourlyPricesIntegrals") && !go.get("hourlyPricesIntegrals").isJsonNull()) {
                HourlyPricesIntegralsParsed combined = parseHourlyPricesIntegrals(go.get("hourlyPricesIntegrals"), ylId);
                if (combined != null && combined.anySlotFilled()) {
                    prices = combined.prices;
                    integrals = combined.integrals;
                }
            }
            if (prices == null) {
                JsonElement priceEl = go.has("hourlyPrices") ? go.get("hourlyPrices") : go.get("price");
                prices = parse24SlotNumbers(priceEl, "hourlyPrices/price");
                if (prices == null) {
                    log.warn("goods id={} missing or invalid hourlyPrices (need 24 numbers or 1 to fill all)", ylId);
                    prices = new BigDecimal[HOURS];
                }
            }
            if (integrals == null) {
                if (go.has("hourlyIntegrals") && !go.get("hourlyIntegrals").isJsonNull()) {
                    integrals = parse24SlotNumbers(go.get("hourlyIntegrals"), "hourlyIntegrals");
                }
                if (integrals == null) {
                    integrals = new BigDecimal[HOURS];
                }
            }
            list.add(new YlGoodsConfig(ylId, dyPool, prices, integrals));
        }
        return list;
    }

    private static final class HourlyPricesIntegralsParsed {
        final BigDecimal[] prices;
        final BigDecimal[] integrals;

        HourlyPricesIntegralsParsed(BigDecimal[] prices, BigDecimal[] integrals) {
            this.prices = prices;
            this.integrals = integrals;
        }

        boolean anySlotFilled() {
            for (int i = 0; i < HOURS; i++) {
                if (prices[i] != null || integrals[i] != null) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * {@code hourlyPricesIntegrals}：对象或数组，按小时写入单价与积分。
     * <ul>
     *   <li>对象：键 {@code "0"}…{@code "23"} 对应小时，值为 {@code {"hourlyPrices":n,"hourlyIntegrals":m}}；键 {@code "*"} 或 {@code "all"} 表示 24 小时同一组值。</li>
     *   <li>数组：每项为上述单键对象，例如 {@code [{"0":{...}},{"1":{...}}]}，合并进同一组槽位。</li>
     * </ul>
     */
    private static HourlyPricesIntegralsParsed parseHourlyPricesIntegrals(JsonElement el, int ylGoodsIdForLog) {
        if (el == null || el.isJsonNull()) {
            return null;
        }
        BigDecimal[] prices = new BigDecimal[HOURS];
        BigDecimal[] integrals = new BigDecimal[HOURS];
        boolean touched = false;
        try {
            if (el.isJsonObject()) {
                touched = mergeHourlyPricesIntegralsObject(el.getAsJsonObject(), prices, integrals);
            } else if (el.isJsonArray()) {
                for (JsonElement item : el.getAsJsonArray()) {
                    if (item != null && item.isJsonObject()
                            && mergeHourlyPricesIntegralsObject(item.getAsJsonObject(), prices, integrals)) {
                        touched = true;
                    }
                }
            } else {
                log.warn("goods id={} hourlyPricesIntegrals must be object or array", ylGoodsIdForLog);
                return null;
            }
        } catch (Exception e) {
            log.warn("goods id={} failed to parse hourlyPricesIntegrals: {}", ylGoodsIdForLog, e.getMessage());
            return null;
        }
        return touched ? new HourlyPricesIntegralsParsed(prices, integrals) : null;
    }

    private static boolean mergeHourlyPricesIntegralsObject(JsonObject obj, BigDecimal[] prices, BigDecimal[] integrals) {
        boolean touched = false;
        for (String key : obj.keySet()) {
            JsonElement valEl = obj.get(key);
            if (valEl == null || !valEl.isJsonObject()) {
                continue;
            }
            JsonObject slot = valEl.getAsJsonObject();
            String k = key.trim();
            if ("*".equals(k) || "all".equalsIgnoreCase(k)) {
                for (int h = 0; h < HOURS; h++) {
                    if (applyOneHourSlot(slot, h, prices, integrals)) {
                        touched = true;
                    }
                }
                continue;
            }
            int hour = parseHourIndex24(k);
            if (hour < 0) {
                log.warn("hourlyPricesIntegrals unknown hour key '{}', skipped", key);
                continue;
            }
            if (applyOneHourSlot(slot, hour, prices, integrals)) {
                touched = true;
            }
        }
        return touched;
    }

    private static int parseHourIndex24(String key) {
        try {
            int h = Integer.parseInt(key.trim());
            if (h >= 0 && h < HOURS) {
                return h;
            }
        } catch (NumberFormatException ignore) {
        }
        return -1;
    }

    private static boolean applyOneHourSlot(JsonObject slotObj, int hour, BigDecimal[] prices, BigDecimal[] integrals) {
        boolean t = false;
        if (slotObj.has("hourlyPrices") && !slotObj.get("hourlyPrices").isJsonNull()) {
            prices[hour] = toBigDecimal(slotObj.get("hourlyPrices"));
            t = true;
        }
        if (slotObj.has("hourlyIntegrals") && !slotObj.get("hourlyIntegrals").isJsonNull()) {
            integrals[hour] = toBigDecimal(slotObj.get("hourlyIntegrals"));
            t = true;
        }
        return t;
    }

    /** 优先 {@code dyPool}，否则兼容旧字段 {@code taskType}。 */
    private static String parseDyPool(JsonObject go) {
        if (go.has("dyPool") && !go.get("dyPool").isJsonNull()) {
            return go.get("dyPool").getAsString();
        }
        if (go.has("taskType") && !go.get("taskType").isJsonNull()) {
            return go.get("taskType").getAsString();
        }
        return null;
    }

    /**
     * 解析 24 小时槽位：支持长度为 24 的 JSON 数组、单元素填充 24 槽、或对象 {@code "0".."23"}。
     */
    private static BigDecimal[] parse24SlotNumbers(JsonElement el, String label) {
        if (el == null || el.isJsonNull()) {
            return null;
        }
        try {
            if (el.isJsonArray()) {
                JsonArray arr = el.getAsJsonArray();
                if (arr.size() == 1) {
                    BigDecimal v = toBigDecimal(arr.get(0));
                    if (v == null) {
                        return null;
                    }
                    BigDecimal[] out = new BigDecimal[HOURS];
                    Arrays.fill(out, v);
                    return out;
                }
                if (arr.size() == HOURS) {
                    BigDecimal[] out = new BigDecimal[HOURS];
                    for (int i = 0; i < HOURS; i++) {
                        out[i] = toBigDecimal(arr.get(i));
                    }
                    return out;
                }
                log.warn("{}: expected 1 or {} elements, got {}", label, HOURS, arr.size());
                return null;
            }
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                BigDecimal[] out = new BigDecimal[HOURS];
                for (int i = 0; i < HOURS; i++) {
                    String k = String.valueOf(i);
                    if (obj.has(k)) {
                        out[i] = toBigDecimal(obj.get(k));
                    }
                }
                return out;
            }
        } catch (Exception e) {
            log.warn("Failed to parse {}: {}", label, e.getMessage());
        }
        return null;
    }

    private static BigDecimal toBigDecimal(JsonElement e) {
        if (e == null || e.isJsonNull()) {
            return null;
        }
        try {
            return new BigDecimal(e.getAsString().trim());
        } catch (Exception ignore) {
        }
        try {
            return BigDecimal.valueOf(e.getAsDouble());
        } catch (Exception ignore) {
        }
        return null;
    }

    public List<Target> getTargets() {
        return targets;
    }

    public static String generateSHA1(String appId, String appSecret, String requestURI, String appTimestamp) {
        String input = appId + appSecret + requestURI + appTimestamp;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating SHA1 hash", e);
        }
    }

    public JsonElement get(String url) {
        return get(null, url);
    }

    public JsonElement get(Target target, String url) {
        String useAddress = target != null ? target.getAddress() : address;
        String useAppId = target != null ? target.getAppId() : appId;
        String useAppSecret = target != null ? target.getAppSecret() : appSecret;
        long now = Instant.now().getEpochSecond();
        String appToken = generateSHA1(useAppId, useAppSecret, url, Long.toString(now));
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(useAddress + url);
            httpGet.addHeader("Appid", useAppId);
            httpGet.addHeader("Apptoken", appToken);
            httpGet.addHeader("AppTimestamp", Long.toString(now));
            httpGet.addHeader("Content-Type", "multipart/form-data");
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                log.debug("YL GET {} -> {}", url, response);
                if (response.getEntity() != null) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    JsonElement json = new JsonParser().parse(responseBody);
                    log.trace("YL GET body: {}", json);
                    return json;
                }
                log.warn("YL GET {} empty body", url);
                return null;
            }
        } catch (Exception e) {
            log.error("YL GET {} failed", url, e);
            return null;
        }
    }

    public JsonElement post(String url, JsonElement json) {
        return post(null, url, json);
    }

    public JsonElement post(Target target, String url, JsonElement json) {
        String useAddress = target != null ? target.getAddress() : address;
        String useAppId = target != null ? target.getAppId() : appId;
        String useAppSecret = target != null ? target.getAppSecret() : appSecret;
        long now = Instant.now().getEpochSecond();
        String appToken = generateSHA1(useAppId, useAppSecret, url, Long.toString(now));
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(useAddress + url);
            httpPost.addHeader("Appid", useAppId);
            httpPost.addHeader("Apptoken", appToken);
            httpPost.addHeader("AppTimestamp", Long.toString(now));
            httpPost.addHeader("Content-Type", "application/json");
            HttpEntity httpEntity = new StringEntity(json.toString(), StandardCharsets.UTF_8);
            httpPost.setEntity(httpEntity);
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                if (response.getEntity() != null) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    log.debug("YL POST {} -> {} body {}", url, response, responseBody);
                    return new JsonParser().parse(responseBody);
                }
                log.warn("YL POST {} empty body", url);
                return null;
            }
        } catch (Exception e) {
            log.error("YL POST {} failed", url, e);
            return null;
        }
    }

    public static JsonElement findByName(JsonArray jsonArray, String key, String value) {
        if (jsonArray == null || key == null || value == null) {
            return null;
        }
        for (JsonElement element : jsonArray) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject o = element.getAsJsonObject();
            if (!o.has(key)) {
                continue;
            }
            try {
                if (value.equals(o.get(key).getAsString())) {
                    return element;
                }
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    /**
     * 从 YL 订单 JSON 解析商品 id（与配置里 {@code goods.id} 对应）。
     */
    public static int resolveOrderYlGoodsId(JsonObject order, Target target) {
        String[] keys = {"goods_id", "goodsId", "good_id", "product_id", "sku_id"};
        for (String k : keys) {
            if (order.has(k) && !order.get(k).isJsonNull()) {
                try {
                    return order.get(k).getAsInt();
                } catch (Exception ignore) {
                }
            }
        }
        if (order.has("goods") && order.get("goods").isJsonObject()) {
            JsonObject g = order.get("goods").getAsJsonObject();
            if (g.has("id") && !g.get("id").isJsonNull()) {
                try {
                    return g.get("id").getAsInt();
                } catch (Exception ignore) {
                }
            }
        }
        if (target.getGoods() != null && target.getGoods().size() == 1) {
            return target.getGoods().get(0).getYlGoodsId();
        }
        return -1;
    }
}
