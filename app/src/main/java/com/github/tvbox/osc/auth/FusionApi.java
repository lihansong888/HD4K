package com.github.tvbox.osc.auth;

import android.os.Build;
import android.provider.Settings;


import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * FusionTV 后台接口：设备编号、设备令牌、请求封装、最近一次的服务端信息。
 * 应用里所有需要访问后台的地方都从这里走。
 */
public final class FusionApi {

    public static final String API = "https://api.lihansong.top/api/v1/";
    public static final String LOCK_URL = "locked://purchase";

    private static final String KEY_DEVICE_ID = "fusiontv_device_id";
    private static final String KEY_DEVICE_TOKEN = "fusiontv_device_token";
    private static final String KEY_DEVICE_HW = "fusiontv_device_hw";

    /** 弹窗内容 */
    public static final class Popup {
        public int version;
        public String title = "";
        public String body = "";
        public String imageUrl = "";
        public String mode = "once";   // once 只弹一次 / open 每次打开 / daily 每天几次
        public int daily = 1;
    }

    /** 服务端最近一次返回的状态 */
    public static final class Info {
        public String status = "";
        public String group = "";
        public String deviceCode = "";
        public boolean trial;
        public long expiresAt;
        public int rev;
        public String purchaseUrl = "";
        public String purchaseTip = "";
        public String marquee = "";
        public String marqueeMode = "always"; // always 一直显示 / open 每次打开滚一遍 / daily 每天几次
        public int marqueeDaily = 3;
        public int marqueeVersion;
        public boolean maintenance;
        public String maintenanceMessage = "";
        public boolean updateRequired;
        public String updateVersion = "";
        public String updateUrl = "";
        public String updateNotes = "";
        public Popup popup;

        public boolean isApproved() {
            return "approved".equals(status);
        }

        static Info parse(JSONObject o) {
            Info i = new Info();
            i.status = o.optString("status", "pending");
            i.group = o.optString("group", "");
            i.deviceCode = o.optString("device_code", "");
            i.trial = o.optBoolean("trial", false);
            i.expiresAt = o.optLong("expires_at", 0);
            i.rev = o.optInt("rev", 0);
            JSONObject purchase = o.optJSONObject("purchase");
            if (purchase != null) {
                i.purchaseUrl = purchase.optString("url", "");
                i.purchaseTip = purchase.optString("tip", "");
            }
            JSONObject marquee = o.optJSONObject("marquee");
            if (marquee != null) {
                i.marquee = marquee.optString("text", "");
                i.marqueeMode = marquee.optString("mode", "always");
                i.marqueeDaily = Math.max(1, marquee.optInt("daily", 3));
                i.marqueeVersion = marquee.optInt("version", 0);
            }
            JSONObject maint = o.optJSONObject("maintenance");
            if (maint != null && maint.optBoolean("on", false)) {
                i.maintenance = true;
                i.maintenanceMessage = maint.optString("message", "");
            }
            JSONObject update = o.optJSONObject("update");
            if (update != null) {
                i.updateRequired = update.optBoolean("required", false);
                i.updateVersion = update.optString("version", "");
                i.updateUrl = update.optString("url", "");
                i.updateNotes = update.optString("notes", "");
            }
            JSONObject popup = o.optJSONObject("popup");
            if (popup != null) {
                Popup p = new Popup();
                p.version = popup.optInt("version", 0);
                p.title = popup.optString("title", "");
                p.body = popup.optString("body", "");
                p.imageUrl = popup.optString("image_url", "");
                p.mode = popup.optString("mode", "once");
                p.daily = Math.max(1, popup.optInt("daily", 1));
                i.popup = p;
            }
            return i;
        }
    }

    private static volatile Info info = new Info();

    public static Info info() {
        return info;
    }

    static void setInfo(Info value) {
        info = value;
    }

    public static String deviceId() {
        String id = Prefers.getString(KEY_DEVICE_ID);
        if (id == null || id.isEmpty()) {
            id = hardwareId();
            boolean hw = id != null;
            if (!hw) id = "FT-" + UUID.randomUUID().toString().replace("-", "");
            Prefers.put(KEY_DEVICE_ID, id);
            Prefers.put(KEY_DEVICE_HW, hw);
        }
        return id;
    }

    /** 设备编号是否由硬件派生（卸载重装后不变）。 */
    public static boolean isHardwareId() {
        deviceId();
        return Prefers.getBoolean(KEY_DEVICE_HW, false);
    }

    /** 用系统的 ANDROID_ID 派生设备编号：同一台设备卸载重装后编号不变，老用户重装自动恢复授权。 */
    private static String hardwareId() {
        try {
            String aid = Settings.Secure.getString(Init.context().getContentResolver(), Settings.Secure.ANDROID_ID);
            if (aid == null || aid.length() < 8 || "9774d56d682e549c".equalsIgnoreCase(aid) || aid.matches("0+")) return null;
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(("fusiontv|" + aid).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder("FT-");
            for (int i = 0; i < 16; i++) sb.append(String.format("%02x", hash[i]));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** 给人看的简短编号，完整编号不显示在屏幕上。 */
    public static String shortId() {
        String id = deviceId();
        return id.length() > 11 ? id.substring(0, 11) : id;
    }

    public static String token() {
        String t = Prefers.getString(KEY_DEVICE_TOKEN);
        return t == null ? "" : t;
    }

    static void saveToken(String token) {
        Prefers.put(KEY_DEVICE_TOKEN, token);
    }

    static void clearToken() {
        Prefers.remove(KEY_DEVICE_TOKEN);
    }

    /** 设备编号和令牌一起清掉，下次启动按新设备重新申请（凭证对不上时自救，避免卡死在授权界面）。 */
    static void resetIdentity() {
        Prefers.remove(KEY_DEVICE_TOKEN);
        // 硬件编号不换（服务器会重新签发令牌）；随机编号的旧安装换成新编号
        if (!isHardwareId()) {
            Prefers.remove(KEY_DEVICE_ID);
            Prefers.remove(KEY_DEVICE_HW);
        }
        info = new Info();
    }

    public static int appVersion() {
        return com.github.tvbox.osc.BuildConfig.VERSION_CODE;
    }

    /** 当前设备专属的配置地址；还没有令牌时返回空字符串。 */
    public static String configUrl() {
        String t = token();
        return t.isEmpty() ? "" : API + "config/" + t;
    }

    public static String deviceName() {
        return Build.MODEL == null ? "TV" : Build.MODEL;
    }

    public static String deviceModel() {
        return (Build.MANUFACTURER == null ? "" : Build.MANUFACTURER + " ") + (Build.MODEL == null ? "Android TV" : Build.MODEL);
    }

    /** 同步 POST，必须在子线程调用。HTTP 失败时结果里带 _http_code。 */
    public static JSONObject post(String endpoint, JSONObject payload) throws Exception {
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), payload.toString());
        Request request = new Request.Builder().url(API + endpoint).post(body).build();
        Response response = OkHttp.client(15000).newCall(request).execute();
        try {
            String text = response.body() == null ? "{}" : response.body().string();
            JSONObject result;
            try {
                result = new JSONObject(text);
            } catch (Exception e) {
                result = new JSONObject();
            }
            if (!response.isSuccessful()) {
                result.put("_http_code", response.code());
                result.put("ok", false);
            }
            return result;
        } finally {
            response.close();
        }
    }

    /** 带设备凭证的请求体 */
    public static JSONObject authPayload() throws Exception {
        JSONObject p = new JSONObject();
        p.put("device_id", deviceId());
        p.put("device_token", token());
        p.put("app_version", appVersion());
        return p;
    }

    /** 用卡密开通，必须在子线程调用。返回服务端 JSON。 */
    public static JSONObject redeem(String code) throws Exception {
        JSONObject p = authPayload();
        p.put("code", code);
        return post("card/redeem", p);
    }

    public static boolean isLocked(String url) {
        return url != null && url.startsWith(LOCK_URL);
    }
}
