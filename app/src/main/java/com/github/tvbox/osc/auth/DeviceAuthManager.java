package com.github.tvbox.osc.auth;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;


import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 设备授权门禁：向 FusionTV 后台注册设备、轮询状态。
 * 授权通过才放行；被锁定、拒绝、到期、维护、强制升级时回到授权界面。
 */
public final class DeviceAuthManager {

    private static final long POLL_GATE = 4000L;   // 授权界面显示时，查询要快
    private static final long POLL_OK = 10000L;    // 已授权后的状态查询间隔
    private static final long POLL_ERROR = 15000L; // 出错后的重试间隔

    private static final int MODE_NONE = 0;
    private static final int MODE_CARD = 1;   // 待审批 / 已到期：可以输入卡密
    private static final int MODE_UPDATE = 2; // 强制升级：可以显示下载二维码

    public interface Callback {
        void onApproved();

        default void onRevoked() {
        }

        /** 每次拿到最新状态时回调（主线程）。configChanged 为 true 表示配置或分组变了，应重新加载配置。 */
        default void onChanged(FusionApi.Info info, boolean configChanged) {
        }
    }

    private final Activity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final TextView messageView;
    private final AlertDialog dialog;

    private Callback callback;
    private int gateMode = MODE_NONE;
    private int lastRev = -1;
    private String lastGroup = null;
    private volatile boolean finished;
    private volatile boolean authorized;
    private volatile boolean requestInFlight;

    private static final int MAX_RESETS = 3;
    private int resets;

    private final Runnable pollRunnable = this::checkStatus;

    public static DeviceAuthManager create(Activity activity) {
        return new DeviceAuthManager(activity);
    }

    private DeviceAuthManager(Activity activity) {
        this.activity = activity;
        boolean phone = isPhoneLayout();
        messageView = new TextView(activity);
        messageView.setGravity(Gravity.CENTER);
        messageView.setIncludeFontPadding(false);
        messageView.setTextSize(phone ? 22 : 17);
        messageView.setLineSpacing(0, phone ? 1.12f : 1.0f);
        messageView.setPadding(dp(phone ? 24 : 18), dp(phone ? 20 : 7), dp(phone ? 24 : 18), dp(phone ? 20 : 7));

        dialog = new AlertDialog.Builder(activity)
                .setTitle("设备授权")
                .setView(messageView)
                .setCancelable(false)
                .setPositiveButton("重新检查", null)
                .setNegativeButton("退出应用", (d, which) -> activity.finish())
                .setNeutralButton("输入卡密", null)
                .create();
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void start(Callback callback) {
        this.callback = callback;
        showMessage("正在连接授权服务器，请稍候……");
        showDialogIfNeeded();
        registerDevice();
    }

    /** 立即再查一次状态（比如卡密激活成功后） */
    public void checkNow() {
        handler.removeCallbacks(pollRunnable);
        handler.post(pollRunnable);
    }

    // ---------------- 界面 ----------------

    private int dp(float value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
    }

    private boolean isPhoneLayout() {
        return activity.getResources().getConfiguration().smallestScreenWidthDp < 600;
    }

    private void showDialogIfNeeded() {
        if (dialog.isShowing() || finished || activity.isFinishing()) return;
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> checkNow());
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> onNeutral());
        configureDialogWindow();
        applyNeutral();
    }

    private void configureDialogWindow() {
        if (dialog.getWindow() == null) return;
        float widthRatio = isPhoneLayout() ? 0.92f : 0.45f;
        dialog.getWindow().setLayout((int) (activity.getResources().getDisplayMetrics().widthPixels * widthRatio), WindowManager.LayoutParams.WRAP_CONTENT);
        TextView titleView = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if (titleView != null) titleView.setTextSize(isPhoneLayout() ? 22 : 18);
        float size = isPhoneLayout() ? 17 : 14;
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(size);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(size);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextSize(size);
    }

    private void applyNeutral() {
        if (!dialog.isShowing()) return;
        android.widget.Button b = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (gateMode == MODE_CARD) {
            b.setText("输入卡密");
            b.setVisibility(View.VISIBLE);
        } else if (gateMode == MODE_UPDATE && !FusionApi.info().updateUrl.isEmpty()) {
            b.setText("下载二维码");
            b.setVisibility(View.VISIBLE);
        } else {
            b.setVisibility(View.GONE);
        }
    }

    private void onNeutral() {
        if (gateMode == MODE_CARD) {
            PurchaseDialog.showRedeem(activity, this::checkNow);
        } else if (gateMode == MODE_UPDATE) {
            PurchaseDialog.showQr(activity, "下载新版本", "用手机扫描二维码下载新版本，安装后重新打开。", FusionApi.info().updateUrl);
        }
    }

    private void showMessage(String message) {
        handler.post(() -> {
            if (!finished && !activity.isFinishing()) messageView.setText(message);
        });
    }

    private void showGate(String message, int mode) {
        if (finished || activity.isFinishing()) return;
        authorized = false;
        gateMode = mode;
        messageView.setText(message);
        showDialogIfNeeded();
        applyNeutral();
    }

    private void revoke(String message, int mode) {
        if (finished || activity.isFinishing()) return;
        boolean wasAuthorized = authorized;
        authorized = false;
        if (wasAuthorized && callback != null) callback.onRevoked();
        // 正在别的页面（比如播放中）被撤销：直接退出应用，下次打开会看到授权界面
        if (wasAuthorized && !activity.hasWindowFocus()) {
            activity.finishAffinity();
            return;
        }
        showGate(message, mode);
    }

    // ---------------- 网络 ----------------

    private void schedulePoll(long delay) {
        handler.removeCallbacks(pollRunnable);
        if (!finished) handler.postDelayed(pollRunnable, delay);
    }

    private void registerDevice() {
        if (finished || requestInFlight) return;
        requestInFlight = true;
        executor.execute(() -> {
            long next = POLL_GATE;
            try {
                JSONObject payload = new JSONObject();
                payload.put("device_id", FusionApi.deviceId());
                payload.put("device_name", FusionApi.deviceName());
                payload.put("device_model", FusionApi.deviceModel());
                payload.put("app_version", FusionApi.appVersion());
                payload.put("hw", FusionApi.isHardwareId());
                String token = FusionApi.token();
                if (!token.isEmpty()) payload.put("device_token", token);

                JSONObject result = FusionApi.post("device/register", payload);
                if (!result.optBoolean("ok")) {
                    int code = result.optInt("_http_code", 0);
                    if (code == 409 && "token_conflict".equals(result.optString("error")) && resets < MAX_RESETS) {
                        // 本机丢了令牌但服务器还有记录：换一个新的设备编号重新申请，不再卡死
                        resets++;
                        FusionApi.resetIdentity();
                        showMessage("正在重新申请授权……");
                        next = 300L;
                    } else if (code == 409) {
                        handler.post(() -> revoke("设备凭证无效或设备记录已变更。\n\n请联系管理员在后台删除这台设备的记录后重试。\n\n设备编号：" + FusionApi.shortId(), MODE_NONE));
                        next = POLL_ERROR;
                    } else if (code == 429) {
                        showMessage("请求过于频繁，请稍后再试。");
                        next = POLL_ERROR;
                    } else {
                        showMessage("暂时无法连接授权服务器，正在自动重试。\n\n请检查网络连接。\n\n(注册失败，状态码 " + code + ")");
                        next = POLL_ERROR;
                    }
                    return;
                }
                String returned = result.optString("device_token", "");
                if (!returned.isEmpty()) {
                    FusionApi.saveToken(returned);
                } else if (FusionApi.token().isEmpty()) {
                    showMessage("服务器未返回设备凭证，请稍后重试。");
                    next = POLL_ERROR;
                    return;
                }
                handleInfo(FusionApi.Info.parse(result), result.optString("message", ""));
            } catch (Exception e) {
                showMessage("暂时无法连接授权服务器，正在自动重试。\n\n请检查网络连接。\n\n(" + errText(e) + ")");
                next = POLL_ERROR;
            } finally {
                requestInFlight = false;
                schedulePollOnMain(next);
            }
        });
    }

    private void schedulePollOnMain(long delay) {
        handler.post(() -> schedulePoll(delay));
    }

    private void checkStatus() {
        if (finished) return;
        if (requestInFlight) {
            schedulePoll(POLL_GATE);
            return;
        }
        if (FusionApi.token().isEmpty()) {
            registerDevice();
            return;
        }
        requestInFlight = true;
        executor.execute(() -> {
            long next = authorized ? POLL_OK : POLL_GATE;
            try {
                JSONObject result = FusionApi.post("device/status", FusionApi.authPayload());
                if (result.optBoolean("ok")) {
                    handleInfo(FusionApi.Info.parse(result), "");
                } else {
                    int code = result.optInt("_http_code", 0);
                    String err = result.optString("error");
                    if ((code == 401 && "bad_token".equals(err)) || (code == 404 && "not_found".equals(err))) {
                        // 服务器明确说设备记录被删除或凭证对不上：按新设备重新申请
                        if (resets < MAX_RESETS) {
                            resets++;
                            FusionApi.resetIdentity();
                            next = 300L;
                        } else {
                            if (!authorized) showMessage("设备凭证无效，请联系管理员。\n\n(状态码 " + code + ")");
                            next = POLL_ERROR;
                        }
                    } else {
                        if (!authorized) showMessage("授权状态暂时无法查询，正在自动重试。\n\n" + codeText() + "\n\n(状态码 " + code + ")");
                        next = POLL_ERROR;
                    }
                }
            } catch (Exception e) {
                if (!authorized) showMessage("网络暂时不可用，正在自动重试。\n\n" + codeText() + "\n\n(" + errText(e) + ")");
                next = POLL_ERROR;
            } finally {
                requestInFlight = false;
                schedulePollOnMain(next);
            }
        });
    }

    private static String errText(Exception e) {
        String m = e.getMessage();
        if (m != null && m.length() > 80) m = m.substring(0, 80);
        return e.getClass().getSimpleName() + (m == null ? "" : ": " + m);
    }

    private String codeText() {
        String code = FusionApi.info().deviceCode;
        return "设备申请码：" + (code.isEmpty() ? "获取中" : code);
    }

    // ---------------- 状态处理 ----------------

    private void handleInfo(FusionApi.Info info, String serverMessage) {
        FusionApi.setInfo(info);
        handler.post(() -> apply(info));
    }

    private void apply(FusionApi.Info info) {
        if (finished || activity.isFinishing()) return;
        String id = FusionApi.shortId();

        if (info.updateRequired) {
            String notes = info.updateNotes.isEmpty() ? "" : "\n\n" + info.updateNotes;
            String url = info.updateUrl.isEmpty() ? "" : "\n\n下载地址：\n" + info.updateUrl;
            revoke("发现新版本" + (info.updateVersion.isEmpty() ? "" : " " + info.updateVersion) + "，需要升级后才能继续使用。" + notes + url, MODE_UPDATE);
            return;
        }
        if (info.maintenance) {
            revoke(info.maintenanceMessage.isEmpty() ? "系统维护中，请稍后再试。" : info.maintenanceMessage, MODE_NONE);
            return;
        }

        switch (info.status) {
            case "approved":
                approved(info);
                break;
            case "blocked":
                revoke("此设备已被管理员远程锁定。\n\n请联系管理员恢复设备使用。\n\n设备编号：" + id, MODE_NONE);
                break;
            case "rejected":
                revoke("此设备申请未获批准。\n\n请联系管理员处理后重新检查。\n\n设备编号：" + id, MODE_NONE);
                break;
            case "expired":
                revoke("授权已到期。\n\n可以点「输入卡密」续期，或把下面的申请码告知管理员：\n\n" + codeLine(info), MODE_CARD);
                break;
            default:
                revoke("请将下面的申请码告知管理员，等待审核通过。\n\n" + codeLine(info) + "\n\n有卡密的话，点「输入卡密」可立即开通。授权通过后会自动进入首页。", MODE_CARD);
        }
    }

    private String codeLine(FusionApi.Info info) {
        return "设备申请码：\n" + (info.deviceCode.isEmpty() ? "正在获取……" : info.deviceCode);
    }

    private void approved(FusionApi.Info info) {
        boolean first = !authorized;
        authorized = true;
        gateMode = MODE_NONE;
        if (dialog.isShowing()) dialog.dismiss();
        boolean changed = !first && (info.rev != lastRev || !info.group.equals(lastGroup));
        lastRev = info.rev;
        lastGroup = info.group;
        if (first && callback != null) callback.onApproved();
        if (callback != null) callback.onChanged(info, changed);
    }

    public void cancel() {
        finished = true;
        handler.removeCallbacksAndMessages(null);
        if (dialog.isShowing()) dialog.dismiss();
        executor.shutdownNow();
    }
}
