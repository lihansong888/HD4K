package com.github.tvbox.osc.auth;

import android.app.Activity;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;


import com.bumptech.glide.Glide;

/**
 * 公告弹窗（图文）。同一个版本号的公告每台设备只弹一次，后台重新保存公告后版本号递增，会再弹一次。
 */
public final class NoticeDialog {

    private static final String KEY_SEEN = "fusiontv_popup_seen";

    private static android.app.Dialog showing;

    private NoticeDialog() {
    }

    /** 不检查"是否看过"，直接弹（由调用方按显示方式决定）。 */
    public static void showNow(Activity activity, FusionApi.Popup popup) {
        if (popup == null || activity == null || activity.isFinishing()) return;
        if (showing != null && showing.isShowing()) return;
        if (popup.title.isEmpty() && popup.body.isEmpty() && popup.imageUrl.isEmpty()) return;
        show(activity, popup);
    }

    public static void maybeShow(Activity activity, FusionApi.Popup popup) {
        if (popup == null || activity == null || activity.isFinishing()) return;
        if (showing != null && showing.isShowing()) return;
        if (popup.title.isEmpty() && popup.body.isEmpty() && popup.imageUrl.isEmpty()) return;
        if (popup.version == Prefers.getInt(KEY_SEEN, 0)) return;
        Prefers.put(KEY_SEEN, popup.version);
        show(activity, popup);
    }

    private static int dp(Activity a, float v) {
        return (int) (v * a.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static void show(Activity activity, FusionApi.Popup popup) {
        boolean phone = activity.getResources().getConfiguration().smallestScreenWidthDp < 600;
        android.util.DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        // 统一用「图在左、文字在右」的横向版式，所有尺寸都按弹窗宽度的比例推算，手机竖屏和电视/模拟器看起来一致
        boolean land = true;
        boolean small = phone && dm.widthPixels < dm.heightPixels; // 手机竖屏：整体按比例缩小
        float widthRatio = (phone && dm.widthPixels < dm.heightPixels) ? 0.92f : 0.62f;
        int dialogWidth = (int) (dm.widthPixels * widthRatio);
        int ink = 0xFF222222;

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFFFF);
        root.setPadding(dp(activity, small ? 16 : 24), dp(activity, small ? 10 : 12), dp(activity, small ? 16 : 24), dp(activity, small ? 4 : 6));

        if (!popup.title.isEmpty()) {
            TextView title = new TextView(activity);
            title.setText(popup.title);
            title.setTextColor(ink);
            title.setTextSize(small ? 17 : (phone ? 20 : 24));
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            title.setGravity(Gravity.CENTER);
            root.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        // 图在左、文字在右（竖屏手机则上下排）
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(land ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        content.setGravity(land ? Gravity.CENTER_VERTICAL : Gravity.CENTER_HORIZONTAL);
        content.setPadding(0, dp(activity, small ? 6 : 8), 0, dp(activity, 2));

        if (!popup.imageUrl.isEmpty()) {
            ImageView image = new ImageView(activity);
            image.setAdjustViewBounds(true);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int size = (int) (dialogWidth * (small ? 0.22f : 0.27f));
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(size, size);
            if (land) ip.rightMargin = dp(activity, small ? 14 : 24); else ip.bottomMargin = dp(activity, 12);
            content.addView(image, ip);
            Glide.with(activity.getApplicationContext()).load(popup.imageUrl).into(image);
        }
        if (!popup.body.isEmpty()) {
            TextView body = new TextView(activity);
            body.setText(popup.body);
            body.setTextColor(ink);
            body.setTextSize(small ? 12 : (phone ? 14 : 16));
            body.setLineSpacing(0, small ? 1.2f : 1.25f);
            body.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams bp = land ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            content.addView(body, bp);
        }
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView ok = new TextView(activity);
        ok.setText("确定");
        ok.setTextColor(ink);
        ok.setTextSize(small ? 13 : (phone ? 14 : 16));
        ok.setGravity(Gravity.CENTER);
        ok.setPadding(dp(activity, 20), dp(activity, 4), dp(activity, 20), dp(activity, 4));
        ok.setFocusable(true);
        ok.setFocusableInTouchMode(true);
        // 不要底色：获得焦点时只用下划线提示
        ok.setOnFocusChangeListener((v, focus) -> { ok.getPaint().setUnderlineText(focus); ok.invalidate(); });
        ok.setBackgroundColor(0x00000000);
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        op.gravity = Gravity.END;
        root.addView(ok, op);

        android.app.Dialog dialog = new android.app.Dialog(activity);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(root);
        showing = dialog;
        ok.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(d -> showing = null);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xFFFFFFFF));
            dialog.getWindow().setLayout(dialogWidth, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        ok.requestFocus();
    }
}
