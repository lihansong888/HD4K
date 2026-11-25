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

import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AlertDialog;

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
        return (int) (v * a.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static TextView text(Activity a, String s) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setGravity(Gravity.CENTER);
        t.setTextSize(phone(a) ? 14 : 16);
        t.setPadding(dp(a, 20), dp(a, 14), dp(a, 20), dp(a, 6));
        return t;
    }

    /** 点到会员专享内容时弹出：说明 + 输入卡密 + 去购买 */
    public static void showLocked(Activity activity, Runnable onSuccess) {
        if (activity == null || activity.isFinishing()) return;
        String tip = FusionApi.info().purchaseTip;
        if (tip == null || tip.isEmpty()) tip = "此内容为会员专享，请购买卡密后输入开通。";
        AlertDialog.Builder b = new AlertDialog.Builder(activity)
                .setTitle("会员专享")
                .setMessage(tip)
                .setPositiveButton("输入卡密", null)
                .setNegativeButton("取消", null);
        if (hasPurchaseUrl()) b.setNeutralButton("去购买", null);
        AlertDialog dialog = b.create();
        dialog.show();
        FusionUi.fit(activity, dialog);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            dialog.dismiss();
            showRedeem(activity, onSuccess);
        });
        if (hasPurchaseUrl()) {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                dialog.dismiss();
                showQr(activity, "扫码购买", "用手机扫描二维码购买卡密，付款后回到这里输入卡密。", FusionApi.info().purchaseUrl);
            });
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();
    }

    public static boolean hasPurchaseUrl() {
        String u = FusionApi.info().purchaseUrl;
        return u != null && !u.isEmpty();
    }

    /** 卡密输入框 */
    public static void showRedeem(Activity activity, Runnable onSuccess) {
        if (activity == null || activity.isFinishing()) return;
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setHint("XXXX-XXXX-XXXX");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setTextSize(phone(activity) ? 16 : 16);
        input.setGravity(Gravity.CENTER);
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(activity, 20), dp(activity, 12), dp(activity, 20), dp(activity, 4));
        box.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog.Builder b = new AlertDialog.Builder(activity)
                .setTitle("输入卡密")
                .setView(box)
                .setPositiveButton("激活", null)
                .setNegativeButton("取消", null);
        if (hasPurchaseUrl()) b.setNeutralButton("去购买", null);
        AlertDialog dialog = b.create();
        dialog.show();
        FusionUi.fit(activity, dialog);
        input.requestFocus();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String code = input.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(activity, "请输入卡密", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            EXECUTOR.execute(() -> {
                String message;
                boolean ok = false;
                try {
                    JSONObject r = FusionApi.redeem(code);
                    ok = r.optBoolean("ok", false);
                    message = r.optString("message", ok ? "开通成功" : "卡密无效或已被使用。");
                    if (r.optInt("_http_code", 0) == 429) message = "尝试次数过多，请稍后再试。";
                } catch (Exception e) {
                    message = "网络异常，请稍后重试。";
                }
                final String msg = message;
                final boolean success = ok;
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing()) return;
                    Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
                    if (success) {
                        dialog.dismiss();
                        if (onSuccess != null) onSuccess.run();
                    } else {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    }
                });
            });
        });
        if (hasPurchaseUrl()) {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                dialog.dismiss();
                showQr(activity, "扫码购买", "用手机扫描二维码购买卡密，付款后回到这里输入卡密。", FusionApi.info().purchaseUrl);
            });
        }
    }

    /** 显示二维码（购买、升级下载地址） */
    public static void showQr(Activity activity, String title, String hint, String url) {
        if (activity == null || activity.isFinishing()) return;
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(activity, 16), dp(activity, 8), dp(activity, 16), dp(activity, 8));
        Bitmap bitmap = QRCode.getBitmap(url, phone(activity) ? 220 : 200, 1);
        if (bitmap != null) {
            ImageView image = new ImageView(activity);
            image.setImageBitmap(bitmap);
            box.addView(image, new LinearLayout.LayoutParams(dp(activity, phone(activity) ? 220 : 200), dp(activity, phone(activity) ? 220 : 200)));
        }
        box.addView(text(activity, hint));
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(box)
                .setPositiveButton("知道了", null)
                .create();
        dialog.show();
        FusionUi.fit(activity, dialog);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();
    }
}
