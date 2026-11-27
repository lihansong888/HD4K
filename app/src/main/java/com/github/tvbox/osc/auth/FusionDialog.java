package com.github.tvbox.osc.auth;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 统一弹窗：公告、设备授权、升级、卡密、会员专享、扫码都用它，外观完全一致。
 * 白色圆角底，标题居中加粗，正文居中，底部一排文字按钮（左边次要、右边主要），遥控器聚焦时显示下划线和浅灰底。
 * 所有尺寸按屏幕短边等比换算（见 FusionUi.unit），任何分辨率下占屏幕的比例都一样。
 */
public final class FusionDialog {
    private static final int INK = 0xFF222222;
    private static final int PRIMARY = 0xFF1976D2;
    private static final int SECONDARY = 0xFF666666;

    private final Activity activity;
    private final Dialog dialog;
    private final TextView titleView;
    private final FrameLayout body;
    private final LinearLayout buttons;
    private TextView messageView;
    private TextView lastPrimary;

    public FusionDialog(Activity activity) {
        this.activity = activity;
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(px(10));
        root.setBackground(bg);
        root.setPadding(px(24), px(16), px(24), px(8));

        titleView = new TextView(activity);
        titleView.setTextColor(INK);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        FusionUi.size(activity, titleView, 24);
        titleView.setPadding(0, 0, 0, px(8));
        titleView.setVisibility(View.GONE);
        root.addView(titleView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        body = new FrameLayout(activity);
        root.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        buttons = new LinearLayout(activity);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.topMargin = px(8);
        root.addView(buttons, bp);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private int px(float ref) {
        return FusionUi.px(activity, ref);
    }

    public FusionDialog title(String title) {
        titleView.setText(title);
        titleView.setVisibility(title == null || title.isEmpty() ? View.GONE : View.VISIBLE);
        return this;
    }

    /** 正文（居中）。之后可用 messageView() 改文字。 */
    public FusionDialog message(CharSequence text) {
        TextView t = new TextView(activity);
        t.setText(text);
        t.setTextColor(INK);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        t.setLineSpacing(0, 1.25f);
        FusionUi.size(activity, t, 16);
        t.setPadding(px(8), px(8), px(8), px(8));
        body.removeAllViews();
        body.addView(t, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        messageView = t;
        return this;
    }

    /** 自定义内容（图文、输入框、二维码等）。 */
    public FusionDialog content(View view) {
        body.removeAllViews();
        body.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return this;
    }

    public TextView messageView() {
        return messageView;
    }

    /** 添加按钮：先加的在左。primary 为主要操作（蓝色加粗）。点击后不会自动关闭，由调用方决定。 */
    public TextView button(String text, boolean primary, View.OnClickListener listener) {
        final TextView b = new TextView(activity);
        b.setText(text);
        b.setGravity(Gravity.CENTER);
        b.setTextColor(primary ? PRIMARY : SECONDARY);
        if (primary) b.setTypeface(null, android.graphics.Typeface.BOLD);
        FusionUi.size(activity, b, 16);
        b.setPadding(px(14), px(8), px(14), px(8));
        b.setFocusable(true);
        b.setFocusableInTouchMode(true);
        b.setClickable(true);
        b.setOnFocusChangeListener((v, focus) -> {
            b.getPaint().setUnderlineText(focus);
            b.setBackgroundColor(focus ? 0x14000000 : 0x00000000);
            b.invalidate();
        });
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = px(6);
        buttons.addView(b, lp);
        if (primary) lastPrimary = b;
        return b;
    }

    public FusionDialog setCancelable(boolean cancelable) {
        dialog.setCancelable(cancelable);
        return this;
    }

    public FusionDialog setOnDismissListener(android.content.DialogInterface.OnDismissListener l) {
        dialog.setOnDismissListener(l);
        return this;
    }

    public Dialog getDialog() {
        return dialog;
    }

    public boolean isShowing() {
        return dialog.isShowing();
    }

    public void dismiss() {
        if (dialog.isShowing()) dialog.dismiss();
    }

    public void show() {
        dialog.show();
        FusionUi.fit(activity, dialog);
        if (lastPrimary != null && lastPrimary.getVisibility() == View.VISIBLE) lastPrimary.requestFocus();
    }
}
