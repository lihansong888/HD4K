package com.github.tvbox.osc.auth;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/** 适配层：认证请求用的 OkHttp 客户端。 */
final class OkHttp {
    private OkHttp() {
    }

    static OkHttpClient client(long timeoutMs) {
        return new OkHttpClient.Builder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();
    }
}
