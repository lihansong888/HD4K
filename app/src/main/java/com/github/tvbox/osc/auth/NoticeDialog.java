package com.github.tvbox.osc.auth;

import android.app.Activity;
import android.view.Gravity;
import android.view.ViewGroup;
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
        return FusionUi.px(a, v);
    }

    private static void show(Activity activity, FusionApi.Popup popup) {
        int dialogWidth = FusionUi.dialogWidth(activity);
        int ink = 0xFF222222;

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 2));
        if (!popup.imageUrl.isEmpty()) {
            ImageView image = new ImageView(activity);
            image.setAdjustViewBounds(true);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int size = (int) (dialogWidth * 0.27f);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(size, size);
            ip.rightMargin = dp(activity, 24);
            content.addView(image, ip);
            Glide.with(activity.getApplicationContext()).load(popup.imageUrl).into(image);
        }
        if (!popup.body.isEmpty()) {
            TextView body = new TextView(activity);
            body.setText(popup.body);
            body.setTextColor(ink);
            FusionUi.size(activity, body, 16);
            body.setLineSpacing(0, 1.25f);
            body.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            content.addView(body, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(content);

        final FusionDialog d = new FusionDialog(activity).title(popup.title).content(scroll);
        d.button("确定", true, v -> d.dismiss());
        showing = d.getDialog();
        d.setOnDismissListener(x -> showing = null);
        d.show();
    }
}
