package com.example.demo.Controller;

import com.example.demo.Config.AjaxResult;
import com.example.demo.Config.Auth;
import com.example.demo.Config.YlGoodsConfig;
import com.example.demo.Service.LyConfigFileService;
import com.example.demo.Service.YLmall;
import com.example.demo.dto.LyGoodsDto;
import com.example.demo.dto.LyTargetDto;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/admin/lyConfig")
public class LyConfigController {

    private static final Logger log = LoggerFactory.getLogger(LyConfigController.class);
    private static final int HOURS = 24;

    @Autowired
    private YLmall yLmall;

    @Autowired
    private LyConfigFileService lyConfigFileService;

    @Auth(user = "1000")
    @GetMapping("/list")
    public AjaxResult list() {
        List<YLmall.Target> targets = yLmall.getTargets();
        List<LyTargetDto> dtoList = new ArrayList<>();

        for (YLmall.Target t : targets) {
            if (t == null) {
                continue;
            }
            LyTargetDto dto = new LyTargetDto();
            dto.setKey(t.getKey());
            dto.setAddress(t.getAddress());
            dto.setAppId(t.getAppId());
            dto.setAppSecret(t.getAppSecret());

            List<LyGoodsDto> goodsDtos = new ArrayList<>();
            if (t.getGoods() != null) {
                for (YlGoodsConfig g : t.getGoods()) {
                    if (g == null) {
                        continue;
                    }
                    LyGoodsDto gd = new LyGoodsDto();
                    gd.setId(g.getYlGoodsId());
                    gd.setDyPool(g.getDyPool());
                    gd.setHourlyPrices(toList24(g.getHourlyPrices()));
                    gd.setHourlyIntegrals(toList24(g.getHourlyIntegrals()));
                    goodsDtos.add(gd);
                }
            }
            dto.setGoods(goodsDtos);
            dtoList.add(dto);
        }

        return AjaxResult.success(dtoList);
    }

    @Auth(user = "1000")
    @PostMapping("/save")
    public AjaxResult save(@RequestBody List<LyTargetDto> body) {
        try {
            List<LyTargetDto> list = body != null ? body : Collections.emptyList();
            validateTargets(list);

            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            int goodsTotal = 0;

            for (LyTargetDto t : list) {
                JsonObject to = new JsonObject();
                to.addProperty("key", t.getKey().trim());
                to.addProperty("address", t.getAddress().trim());
                to.addProperty("appId", t.getAppId().trim());
                to.addProperty("appSecret", t.getAppSecret().trim());

                JsonArray goodsArr = new JsonArray();
                List<LyGoodsDto> goods = t.getGoods() != null ? t.getGoods() : Collections.emptyList();
                for (LyGoodsDto g : goods) {
                    goodsTotal++;
                    JsonObject go = new JsonObject();
                    go.addProperty("id", g.getId());

                    String dyPool = normalizeDyPool(g.getDyPool());
                    if (StringUtils.hasText(dyPool)) {
                        go.addProperty("dyPool", dyPool);
                    }

                    JsonObject hourlyObj = buildHourlyPricesIntegrals(g.getHourlyPrices(), g.getHourlyIntegrals());
                    if (hourlyObj != null) {
                        go.add("hourlyPricesIntegrals", hourlyObj);
                    }

                    goodsArr.add(go);
                }
                to.add("goods", goodsArr);

                arr.add(to);
            }

            root.add("ylConfig", arr);

            lyConfigFileService.writeConfig(root);
            log.info("保存 lyConfig 成功，商城数: {}, 商品总数: {}", list.size(), goodsTotal);
            return AjaxResult.success();
        } catch (IllegalArgumentException e) {
            log.warn("保存 lyConfig 校验失败: {}", e.getMessage());
            return AjaxResult.fail(-1, e.getMessage());
        } catch (Exception e) {
            log.error("保存 lyConfig 失败", e);
            return AjaxResult.fail(500, e.getMessage() != null ? e.getMessage() : "保存失败");
        }
    }

    private static void validateTargets(List<LyTargetDto> list) {
        Set<String> keys = new HashSet<>();

        for (int i = 0; i < list.size(); i++) {
            LyTargetDto t = list.get(i);
            if (t == null) {
                throw new IllegalArgumentException("第 " + (i + 1) + " 个商城为空");
            }
            String key = trimToNull(t.getKey());
            if (key == null) {
                throw new IllegalArgumentException("商城 key 不能为空");
            }
            if (!keys.add(key)) {
                throw new IllegalArgumentException("商城 key 重复：" + key);
            }
            if (trimToNull(t.getAddress()) == null) {
                throw new IllegalArgumentException("商城 [" + key + "] address 不能为空");
            }
            if (trimToNull(t.getAppId()) == null) {
                throw new IllegalArgumentException("商城 [" + key + "] appId 不能为空");
            }
            if (trimToNull(t.getAppSecret()) == null) {
                throw new IllegalArgumentException("商城 [" + key + "] appSecret 不能为空");
            }

            validateGoods(key, t.getGoods());
        }
    }

    private static void validateGoods(String targetKey, List<LyGoodsDto> goods) {
        if (goods == null) {
            return;
        }
        Set<Integer> ids = new HashSet<>();
        for (int i = 0; i < goods.size(); i++) {
            LyGoodsDto g = goods.get(i);
            if (g == null) {
                throw new IllegalArgumentException("商城 " + targetKey + " 内第 " + (i + 1) + " 个商品为空");
            }
            Integer id = g.getId();
            if (id == null || id <= 0) {
                throw new IllegalArgumentException("商城 " + targetKey + " 内商品 ID 非法：" + id);
            }
            if (!ids.add(id)) {
                throw new IllegalArgumentException("商城 " + targetKey + " 内商品 ID 重复：" + id);
            }

            String dyPool = trimToNull(g.getDyPool());
            if (dyPool != null) {
                String normalized = dyPool.trim().toLowerCase(Locale.ROOT);
                if (!"rq".equals(normalized) && !"pp".equals(normalized)) {
                    log.warn("商城 {} 商品 {} dyPool={} 非 rq/pp，将按原值保存（未来扩展兼容）", targetKey, id, dyPool);
                }
            }

            validateHourlyList(targetKey, id, "hourlyPrices", g.getHourlyPrices());
            validateHourlyList(targetKey, id, "hourlyIntegrals", g.getHourlyIntegrals());
        }
    }

    private static void validateHourlyList(String targetKey, Integer goodsId, String label, List<BigDecimal> list) {
        if (list == null) {
            return;
        }
        if (list.size() != HOURS) {
            throw new IllegalArgumentException("商城 " + targetKey + " 内商品 " + goodsId + " 的 " + label + " 长度必须为 24");
        }
        // 元素允许为 null，BigDecimal 类型由 Jackson 反序列化保证
    }

    private static List<BigDecimal> toList24(BigDecimal[] arr) {
        if (arr == null) {
            return null;
        }
        List<BigDecimal> out = new ArrayList<>(HOURS);
        for (int i = 0; i < HOURS; i++) {
            out.add(i < arr.length ? arr[i] : null);
        }
        return out;
    }

    private static JsonObject buildHourlyPricesIntegrals(List<BigDecimal> hourlyPrices, List<BigDecimal> hourlyIntegrals) {
        if (hourlyPrices == null && hourlyIntegrals == null) {
            return null;
        }
        BigDecimal[] prices = new BigDecimal[HOURS];
        BigDecimal[] integrals = new BigDecimal[HOURS];
        if (hourlyPrices != null) {
            for (int i = 0; i < HOURS; i++) {
                prices[i] = hourlyPrices.get(i);
            }
        }
        if (hourlyIntegrals != null) {
            for (int i = 0; i < HOURS; i++) {
                integrals[i] = hourlyIntegrals.get(i);
            }
        }

        JsonObject hourlyObj = new JsonObject();
        boolean any = false;
        for (int h = 0; h < HOURS; h++) {
            BigDecimal p = prices[h];
            BigDecimal ig = integrals[h];
            if (p == null && ig == null) {
                continue;
            }
            JsonObject slot = new JsonObject();
            if (p != null) {
                slot.add("hourlyPrices", bigDecimalAsJson(p));
            }
            if (ig != null) {
                slot.add("hourlyIntegrals", bigDecimalAsJson(ig));
            }
            hourlyObj.add(String.valueOf(h), slot);
            any = true;
        }
        return any ? hourlyObj : null;
    }

    private static String normalizeDyPool(String raw) {
        String v = trimToNull(raw);
        if (v == null) {
            return null;
        }
        return v.trim();
    }

    private static String trimToNull(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static com.google.gson.JsonElement bigDecimalAsJson(BigDecimal v) {
        if (v == null) {
            return JsonNull.INSTANCE;
        }
        try {
            return new JsonPrimitive(v);
        } catch (Exception ignore) {
            return new JsonPrimitive(v.toPlainString());
        }
    }
}

