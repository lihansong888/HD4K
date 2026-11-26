package com.github.tvbox.osc.auth;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 访问我们自己的域名和常用代码托管站（见 HOSTS）时，改用严格校验证书的客户端。
 * 软件其他网络请求为了兼容各种片源站，沿用了"信任所有证书"的设置，抓包软件可以借此读到内容；
 * 后台的请求走这里以后，证书不是系统信任的就直接连接失败，抓包软件看不到配置和授权内容。
 */
public class StrictHostInterceptor implements Interceptor {
    // 严格校验证书的站点（含子域名）：我们自己的域名，以及数据源常用的代码托管站
    private static final String[] HOSTS = {
            "lihansong.top", "gitee.com", "giteeusercontent.com",
            "github.com", "githubusercontent.com", "gh-proxy.com", "gh-proxy.org"
    };

    private static boolean strictHost(String host) {
        if (host == null) return false;
        host = host.toLowerCase(java.util.Locale.ROOT);
        for (String h : HOSTS) {
            if (host.equals(h) || host.endsWith("." + h)) return true;
        }
        return false;
    }
    private static volatile OkHttpClient strict;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (!strictHost(request.url().host())) return chain.proceed(request);
        OkHttpClient client = strict;
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
            strict = client;
        }
        return client.newCall(request).execute();
    }
}
