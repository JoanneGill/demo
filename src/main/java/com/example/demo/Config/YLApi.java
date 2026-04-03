package com.example.demo.Config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

/**
 * 易龙（YL）开放平台接口路径与订单状态常量。
 */
@Component
public final class YLApi {

    private YLApi() {
    }
// 1   已付款
//2	待处理
//3	处理中
//4	补单中
//5	退单中
//6	已完成
//7	已退单
//8	已退款
//9	有异常
    public static final int PAID = 1;
    public static final int PENDING = 2;
    public static final int PROCESSING = 3;
    public static final int SUPPLEMENTING = 4;
    public static final int REFUNDING = 5;
    public static final int COMPLETED = 6;
    public static final int CANCELLED = 7;
    public static final int REFUNDED = 8;
    public static final int EXCEPTION = 9;

    public static final String goodsList = "/openapi/supplier/Goods/Paging";
    public static final String goodDetail = "/openapi/supplier/Goods/Show";
    public static final String goodsEdit = "/openapi/supplier/Goods/Edit";
    public static final String goodsEditPrice = "/openapi/supplier/Goods/EditPrice";
    public static final String OrderList = "/openapi/supplier/Order/Paging";
    public static final String OrderDetail = "/openapi/supplier/Order/Show";
    public static final String OrderEditState = "/openapi/supplier/Order/StatusHandle";
    public static final String OrderScheduleHandle = "/openapi/supplier/Order/ScheduleHandle";


    @Setter
    @Getter
    public static class ScheduleHandle {
        private Integer id;
        private Integer start_num;
        private Integer current_num;
        private String remark;

    }
    @Setter
    @Getter
    public static class StatusHandle {
        private Integer id;//商品ID
        private Integer old_status;//当前的订单状态
        private Integer new_status;//修改订单状态
        private String remark;//订单备注
        private Integer refund_number;//退款数量，退款时数量和金额任选一
        private String refund_amount;//退款金额，退款时数量和金额任选一

    }


}
