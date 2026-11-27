package com.github.tvbox.osc.auth;

import android.app.Activity;
import android.graphics.Bitmap;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 会员提示、卡密输入、二维码。遥控器和触屏都能操作。
 */
public final class PurchaseDialog {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private PurchaseDialog() {
    }

    private static boolean phone(Activity a) {
        return a.getResources().getConfiguration().smallestScreenWidthDp < 600;
    }

    private static int dp(Activity a, float v) {
        return FusionUi.px(a, v);
    }

    private static TextView text(Activity a, String s) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setGravity(Gravity.CENTER);
        t.setTextColor(0xFF222222);
        t.setLineSpacing(0, 1.25f);
        FusionUi.size(a, t, 16);
        t.setPadding(dp(a, 8), dp(a, 10), dp(a, 8), dp(a, 4));
        return t;
    }

    /** 点到会员专享内容时弹出：说明 + 输入卡密 + 去购买 */
    public static void showLocked(Activity activity, Runnable onSuccess) {
        if (activity == null || activity.isFinishing()) return;
        String tip = FusionApi.info().purchaseTip;
        if (tip == null || tip.isEmpty()) tip = "此内容为会员专享，请购买卡密后输入开通。";
        final FusionDialog d = new FusionDialog(activity).title("会员专享").message(tip);
        d.button("取消", false, v -> d.dismiss());
        if (hasPurchaseUrl()) {
            d.button("去购买", false, v -> {
                d.dismiss();
                showQr(activity, "扫码购买", "用手机扫描二维码购买卡密，付款后回到这里输入卡密。", FusionApi.info().purchaseUrl);
            });
        }
        d.button("输入卡密", true, v -> {
            d.dismiss();
            showRedeem(activity, onSuccess);
        });
        d.show();
    }

    public static boolean hasPurchaseUrl() {
        String u = FusionApi.info().purchaseUrl;
        return u != null && !u.isEmpty();
    }

    /** 卡密输入框 */
    public static void showRedeem(Activity activity, Runnable onSuccess) {
        if (activity == null || activity.isFinishing()) return;
        final EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setHint("XXXX-XXXX-XXXX");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        FusionUi.size(activity, input, 16);
        input.setGravity(Gravity.CENTER);
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(activity, 8), dp(activity, 10), dp(activity, 8), dp(activity, 4));
        box.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final FusionDialog d = new FusionDialog(activity).title("输入卡密").content(box);
        d.button("取消", false, v -> d.dismiss());
        if (hasPurchaseUrl()) {
            d.button("去购买", false, v -> {
                d.dismiss();
                showQr(activity, "扫码购买", "用手机扫描二维码购买卡密，付款后回到这里输入卡密。", FusionApi.info().purchaseUrl);
            });
        }
        final TextView[] ok = new TextView[1];
        ok[0] = d.button("激活", true, v -> {
            String code = input.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(activity, "请输入卡密", Toast.LENGTH_SHORT).show();
                return;
            }
            ok[0].setEnabled(false);
            EXECUTOR.execute(() -> {
                String message;
                boolean success = false;
                try {
                    JSONObject r = FusionApi.redeem(code);
                    success = r.optBoolean("ok", false);
                    message = r.optString("message", success ? "开通成功" : "卡密无效或已被使用。");
                    if (r.optInt("_http_code", 0) == 429) message = "尝试次数过多，请稍后再试。";
                } catch (Exception e) {
                    message = "网络异常，请稍后重试。";
                }
                final String msg = message;
                final boolean good = success;
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing()) return;
                    Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
                    if (good) {
                        d.dismiss();
                        if (onSuccess != null) onSuccess.run();
                    } else {
                        ok[0].setEnabled(true);
                    }
                });
            });
        });
        d.show();
        input.requestFocus();
    }

    /** 显示二维码（购买、升级下载地址） */
    public static void showQr(Activity activity, String title, String hint, String url) {
        if (activity == null || activity.isFinishing()) return;
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(activity, 8), dp(activity, 6), dp(activity, 8), dp(activity, 4));
        Bitmap bitmap = QRCode.getBitmap(url, 400, 1);
        if (bitmap != null) {
            ImageView image = new ImageView(activity);
            image.setImageBitmap(bitmap);
            box.addView(image, new LinearLayout.LayoutParams(dp(activity, 200), dp(activity, 200)));
        }
        box.addView(text(activity, hint));
        final FusionDialog d = new FusionDialog(activity).title(title).content(box);
        d.button("知道了", true, v -> d.dismiss());
        d.show();
    }
}
