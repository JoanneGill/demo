package com.example.demo;

import org.json.JSONException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiDeviceClaimFinishJsoupTest {
    static final String CLAIM_URL = "http://localhost:8999/ppTask/claim";
    static final String FINISH_URL = "http://localhost:8999/ppTask/finish";
    static final int THREAD_COUNT = 110;

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int idx = i;
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();

                    String deviceId = "d" + (1000 + idx);
                    String deviceNickName = "n" + idx;
                    String cardNo = "c" + idx;

                    String claimUrl = CLAIM_URL +
                            "?deviceId=" + URLEncoder.encode(deviceId, StandardCharsets.UTF_8) +
                            "&deviceNickName=" + URLEncoder.encode(deviceNickName, StandardCharsets.UTF_8) +
                            "&cardNo=" + URLEncoder.encode(cardNo, StandardCharsets.UTF_8);

                    // 1. 发起CLAIM请求
                    try {
                        // Jsoup 用作 http 请求工具
                        Connection.Response claimResp = Jsoup.connect(claimUrl)
                                .method(Connection.Method.GET)
                                .ignoreContentType(true)
                                .timeout(10000)
                                .execute();

                        String claimJsonStr = claimResp.body();
                        System.out.println("Thread#" + idx + " claim | " + claimJsonStr);

                        JSONObject claimJson = new JSONObject(claimJsonStr);
                        if (claimJson.optInt("code") == 200 && claimJson.optJSONObject("data") != null) {
                            JSONObject data = claimJson.getJSONObject("data");
                            int claimId = data.optInt("id");
                            String claimDeviceId = data.optString("deviceId", deviceId);
//                            boolean isSuccess = idx % 2 == 0;
                            boolean isSuccess = true;
                            String msg = "device " + deviceId + ", finish: " + isSuccess;

                            String finishUrl = FINISH_URL +
                                    "?claimId=" + claimId +
                                    "&deviceId=" + URLEncoder.encode(claimDeviceId, StandardCharsets.UTF_8) +
                                    "&success=" + isSuccess +
                                    "&msg=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);

                            Connection.Response finishResp = Jsoup.connect(finishUrl)
                                    .method(Connection.Method.GET)
                                    .ignoreContentType(true)
                                    .timeout(10000)
                                    .execute();

                            String finishRespMsg = finishResp.body();
                            System.out.println("Thread#" + idx + " finish | " + finishRespMsg);
                        }
                    } catch (IOException e) {
                        System.out.println("Thread#" + idx + " error: " + e.getMessage());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } catch (InterruptedException ignore) {
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        long t0 = System.currentTimeMillis();
        start.countDown();
        done.await();
        System.out.println("全部测试完毕, 用时: " + (System.currentTimeMillis() - t0) + " ms");
        executor.shutdown();
    }
}