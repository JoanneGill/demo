package com.example.demo.Service;

import cn.hutool.json.JSON;
import com.example.demo.Config.YLApi;
import com.example.demo.Config.YlDevicePolicyProperties;
import com.example.demo.Config.YlGoodsConfig;
import com.example.demo.Data.TaskData;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import com.example.demo.common.DyTaskType;
/**
 * YL 与 Dy 之间的订单、商品同步业务。
 */
@Service
public class YlOrderBusinessService {

    private static final DateTimeFormatter ORDER_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger log = LoggerFactory.getLogger(YlOrderBusinessService.class);
    @Autowired
    private  YLmall yLmall;
    @Autowired
    private LitemallService litemallService;
    @Autowired
    private YlDevicePolicyProperties devicePolicy;

    public void processPaidOrdersAndFulfill() {
        List<YLmall.Target> targets = yLmall.getTargets();
        if (targets == null || targets.isEmpty()) {
            return;
        }
        LocalDateTime nowDt = LocalDateTime.now();
        for (YLmall.Target target : targets) {
            JsonArray orderList = fetchPaidOrderInfos(target);
            if (orderList == null) {
                continue;
            }
            for (int i = 0; i < orderList.size(); i++) {
                JsonObject order = orderList.get(i).getAsJsonObject();
                JsonArray params = order.getAsJsonArray("buy_params");
                BuyParams bp = extractBuyParams(params);

                String now = nowDt.format(ORDER_TIME);
                String buyNumber = order.get("buy_number").getAsString();
                log.debug("sssssssss1:{},{}",bp,buyNumber);
                int ylGid = YLmall.resolveOrderYlGoodsId(order, target);
                if (ylGid <= 0) {
                    log.error("无法解析订单 {} 的 YL 商品 ID，跳过", order.get("id").getAsInt());
                    continue;
                }
                YlGoodsConfig gcfg =  target.findGoods(ylGid);
                if (gcfg == null ||  target.getGoods() == null) {
                    log.error("订单 {} 的 YL 商品 ID {} 在配置中未找到，尝试按规则匹配", order.get("id").getAsInt(), ylGid);
                    postOrderState(target, order, YLApi.PAID, YLApi.CANCELLED, "订单商品未找到");
                    continue;
                }
                BigDecimal integral = gcfg.integralAt(nowDt);
                DyTaskType dyTaskType = gcfg.getDyTaskType();
                TaskData taskData = new TaskData();
                taskData.setBeginTimeFrom(now);  // 订单支付时间作为任务开始时间
                taskData.setDuration(bp.duration);
                taskData.setNumber(Integer.valueOf(buyNumber));
                taskData.setDuration(bp.duration);
                taskData.setPersonAddress(bp.personAddress);
                taskData.setRoomAddress(bp.roomAddress);
                taskData.setRoomId(bp.roomId);
                taskData.setYlOrderId(order.get("id").getAsInt()); //订单id作为 ylTaskId，方便后续对账和问题排查
                taskData.setYlAppId(target.getAppId());
                taskData.setYlGoodId(ylGid);
                taskData.setIntegral(Integer.valueOf(integral.toString()));
                if (dyTaskType.equals(DyTaskType.RQ)){
                    //检查
                    try {
//                        if (litemallService.checkTask(taskData)){
                            litemallService.setTask(taskData);
//                        }
                        postOrderState(target, order, YLApi.PAID, YLApi.COMPLETED, "发货成功");
                    }catch (BusinessException e){
                        log.error("设置 RQ 任务失败: {}", e.getMessage());
                        if (e.getCode() == 405) {
                            postOrderState(target, order, YLApi.PAID, YLApi.PROCESSING, "禁止小黄车");
                        } else {
                            postOrderState(target, order, YLApi.PAID, YLApi.CANCELLED, e.getMessage());
                        }
                    }
                }
                else if (dyTaskType.equals(DyTaskType.PP)){
                    //检查
                    try {
//                        if (litemallService.checkPpTask(taskData)){
                            litemallService.setPpTask(taskData);
//                        }
                        postOrderState(target, order, YLApi.PAID, YLApi.PROCESSING, "发货成功");
                    }catch (BusinessException e){
                        log.error("设置 PP 任务失败: {}", e.getMessage());
                        if (e.getCode() == 405) {
                            postOrderState(target, order, YLApi.PAID, YLApi.PROCESSING, "禁止小黄车");
                        } else {
                            postOrderState(target, order, YLApi.PAID, YLApi.CANCELLED, e.getMessage());
                        }
                    }
                }
            }
        }
    }

    public void syncGoodsStockAndHourlyPrice() {
        List<YLmall.Target> targets = yLmall.getTargets();
        if (targets == null || targets.isEmpty()) {
            return;
        }
        LocalDateTime nowDt = LocalDateTime.now();
        for (YLmall.Target target : targets) {
            JsonArray goodList = fetchGoodsInfos(target);
            if (goodList == null) {
                continue;
            }
            applyDevicePolicyPerGood(target, goodList);
            List<YlGoodsConfig> cfgs = target.getGoods();
            if (cfgs == null || cfgs.isEmpty()) {
                continue;
            }
            for (YlGoodsConfig gcfg : cfgs) {
                if (!gcfg.hasAnyPrice()) {
                    continue;
                }
                syncSingleGoodsPriceIfNeeded(target, goodList, gcfg, nowDt);
            }
        }
    }

    private JsonArray fetchPaidOrderInfos(YLmall.Target target) {
        JsonObject body = new JsonObject();
        body.addProperty("page", 1);
        body.addProperty("list_rows", 200);
        body.addProperty("status", YLApi.PAID);
        JsonElement orders = yLmall.post(target, YLApi.OrderList, body);
//        JsonElement orders = JsonParser.parseString(
//        "{\"code\":0,\"message\":\"ok\",\"data\":{\"count\":1,\"infos\":[{\"id\":28536,\"status\":1,\"goods_id\":3,\"selling_price\":10,\"amount\":100,\"customer_id\":12,\"buy_params\":[{\"key\":\"主页链接\",\"name\":\"Σ╕╗Θí╡Θô╛µÄÑ\",\"type\":61,\"value\":\"https://v.douyin.com/9R04SF0T-LU/\",\"verify\":{\"max\":0,\"min\":0},\"is_default\":false,\"description\":\"Σ╕╗Θí╡Θô╛µÄÑ\",\"type_config\":\"\"},{\"key\":\"下单时长\",\"name\":\"Σ╕ïσìòµù╢Θò┐\",\"type\":6,\"value\":\"1\",\"verify\":{\"max\":10,\"min\":0},\"is_default\":false,\"description\":\"Σ╕ïσìòµù╢Θò┐\",\"type_config\":\"\"}],\"parameter\":\"\\\"https://v.douyin.com/xrh5z6-3rOw/\\\",\\\"1\\\"\",\"buy_number\":10,\"refund_amount\":0,\"start_num\":0,\"current_num\":0,\"is_card_code\":2,\"card_code_ids\":[],\"create_time\":1774963146,\"status_changes\":[{\"At\":1774963146,\"Name\":\"σ╖▓Σ╗ÿµ¼╛\"}],\"remark\":\"\",\"ip\":\"43.228.227.216\",\"goods_name\":\"ceshiΦ»╖σï┐Σ╕ïσìò\",\"goods_refund_status\":[],\"customer_order_id\":\"\"}]}}");
//        log.debug("sssssssss2:{}",orders);
        if (orders == null || !orders.isJsonObject()) {
            return null;
        }
        JsonObject root = orders.getAsJsonObject();
        JsonElement dataEl = root.get("data");
        if (dataEl == null || dataEl.isJsonNull() || !dataEl.isJsonObject()) {
            return null;
        }
        JsonObject dataObj = dataEl.getAsJsonObject();
        JsonElement infosEl = dataObj.get("infos");
        if (infosEl == null || infosEl.isJsonNull() || !infosEl.isJsonArray()) {
            return null;
        }
        return infosEl.getAsJsonArray();
    }

    private JsonArray fetchGoodsInfos(YLmall.Target target) {
        JsonObject body = new JsonObject();
        body.addProperty("page", 1);
        body.addProperty("list_rows", 200);
        JsonElement goods = yLmall.post(target, YLApi.goodsList, body);
        if (goods == null || goods.isJsonNull() || !goods.isJsonObject()) {
            return null;
        }
        JsonObject goodsObj = goods.getAsJsonObject();
        JsonElement dataEl = goodsObj.get("data");
        if (dataEl == null || dataEl.isJsonNull() || !dataEl.isJsonObject()) {
            return null;
        }
        JsonObject dataObj = dataEl.getAsJsonObject();
        JsonElement infosEl = dataObj.get("infos");
        if (infosEl == null || infosEl.isJsonNull() || !infosEl.isJsonArray()) {
            return null;
        }
        return infosEl.getAsJsonArray();
    }

    private void postOrderState(YLmall.Target target, JsonObject order, int oldStatus, int newStatus, String remark) {
        JsonObject json = new JsonObject();
        json.addProperty("id", order.get("id").getAsInt());
        json.addProperty("old_status", oldStatus);
        json.addProperty("new_status", newStatus);
        json.addProperty("remark", remark);
        yLmall.post(target, YLApi.OrderEditState, json);
    }

    /**
     * 按 YL 商品 id 匹配配置：有配置则用其 {@code dyPool} 查 Dy 剩余设备；未在配置中的商品按 {@code rq} 池。
     */
    private void applyDevicePolicyPerGood(YLmall.Target target, JsonArray goodList) {
        for (int i = 0; i < goodList.size(); i++) {
            JsonObject g = goodList.get(i).getAsJsonObject();
            int ylId;
            try {
                ylId = g.get("id").getAsInt();
            } catch (Exception e) {
                continue;
            }
            YlGoodsConfig cfg = target.findGoods(ylId);
            DyTaskType pool = cfg != null ? cfg.getDyTaskType() : DyTaskType.RQ;
            Integer devices = 0;
            if (pool == DyTaskType.RQ) {
                devices = litemallService.getDevices();
            }
            else if (pool == DyTaskType.PP) {
                devices = litemallService.getPpDevices();
            }
            if (devices == null) {
                devices = 0;
            }
            applyDevicePolicyToOneGood(target, g, devices, pool);
        }
    }

    private void applyDevicePolicyToOneGood(YLmall.Target target, JsonObject g, int devices, DyTaskType pool) {
        int deviceLow;
        int deviceHigh;
        int stockReserve;
        if (pool == DyTaskType.PP) {
            YlDevicePolicyProperties.Pp p = devicePolicy.getPp();
            deviceLow = p.getDeviceLow();
            deviceHigh = p.getDeviceHigh();
            stockReserve = p.getStockReserve();
        } else {
            YlDevicePolicyProperties.Rq r = devicePolicy.getRq();
            deviceLow = r.getDeviceLow();
            deviceHigh = r.getDeviceHigh();
            stockReserve = r.getStockReserve();
        }
        if (devices < deviceLow) {
            if ("2".equals(g.get("is_close").getAsString())) {
                g.addProperty("is_close", 1);}
        } else if (devices > deviceHigh) {
            g.addProperty("is_close", 2);
            g.addProperty("stock", -1);
        } else {
            g.addProperty("is_close", 2);
            g.addProperty("stock", devices - stockReserve);
        }
        yLmall.post(target, YLApi.goodsEdit, g);
    }

    private void syncSingleGoodsPriceIfNeeded(YLmall.Target target, JsonArray goodList, YlGoodsConfig gcfg, LocalDateTime nowDt) {
        BigDecimal expectedPrice = gcfg.priceAt(nowDt);
        if (expectedPrice == null) {
            return;
        }
        int goodsId = gcfg.getYlGoodsId();
        for (int i = 0; i < goodList.size(); i++) {
            JsonObject good = goodList.get(i).getAsJsonObject();
            int ylGoodId;
            try {
                ylGoodId = good.get("id").getAsInt();
            } catch (Exception e) {
                continue;
            }
            if (goodsId != ylGoodId) {
                continue;
            }
            String priceStr = Objects.toString(good.get("price").getAsString(), null);
            if (priceStr == null) {
                continue;
            }
            BigDecimal currentPrice;
            try {
                currentPrice = new BigDecimal(priceStr);
            } catch (Exception e) {
                continue;
            }
            if (expectedPrice.compareTo(currentPrice) != 0) {
                JsonObject j = new JsonObject();
                j.addProperty("id", ylGoodId);
                j.addProperty("price", expectedPrice);
                yLmall.post(target, YLApi.goodsEditPrice, j);
            }
        }
    }

    private static BuyParams extractBuyParams(JsonArray params) {
        BuyParams bp = new BuyParams();
        JsonElement el = YLmall.findByName(params, "key", "主页链接");
        if (el != null) {
            String v = el.getAsJsonObject().get("value").getAsString();
            if (v.length() < 255) {
                bp.personAddress = v;
            }
        }
        el = YLmall.findByName(params, "key", "直播链接");
        if (el != null) {
            String v = el.getAsJsonObject().get("value").getAsString();
            if (v.length() < 3000) {
                bp.roomAddress = v;
            }
        }
        el = YLmall.findByName(params, "key", "下单时长");
        if (el != null) {
            bp.duration = el.getAsJsonObject().get("value").getAsString();
        }
        bp.roomId = buyParamFirst(params, "房间ID", "roomId", "直播间ID");
        bp.secUid = buyParamFirst(params, "sec_uid", "secUid", "人员sec_uid");
        bp.personName = buyParamFirst(params, "主页链接", "人员姓名", "personName");
        return bp;
    }

    private static String buyParamFirst(JsonArray params, String... keys) {
        if (params == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            JsonElement el = YLmall.findByName(params, "key", key);
            if (el == null || !el.isJsonObject()) {
                continue;
            }
            try {
                String v = el.getAsJsonObject().get("value").getAsString();
                if (v != null && !v.trim().isEmpty()) {
                    return v.trim();
                }
            } catch (Exception ignore) {
            }
        }
        return "";
    }

    private static final class BuyParams {
        String personAddress = "";
        String roomAddress = "";
        String duration = "";
        String roomId = "";
        String secUid = "";
        String personName = "";
        @Override
        public String toString() {
            return "BuyParams{" +
                    "personAddress='" + personAddress + '\'' +
                    ", roomAddress='" + roomAddress + '\'' +
                    ", duration='" + duration + '\'' +
                    ", roomId='" + roomId + '\'' +
                    ", secUid='" + secUid + '\'' +
                    ", personName='" + personName + '\'' +
                    '}';
        }
    }
}
