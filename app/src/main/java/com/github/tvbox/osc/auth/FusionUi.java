package com.github.tvbox.osc.auth;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

/** 首页的跑马灯和公告弹窗（显示方式由后台控制：一直 / 每次打开一次 / 每天几次）。 */
public final class FusionUi {

    private final Activity activity;
    private final FrameLayout box;
    private final TextView text;

    public FusionUi(Activity activity, FrameLayout box, TextView text) {
        this.activity = activity;
        this.box = box;
        this.text = text;
        // 跑马灯区域宽度取屏幕宽度的一半
        android.view.ViewGroup.LayoutParams lp = box.getLayoutParams();
        lp.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.46f);
        box.setLayoutParams(lp);
    }

    /** 弹窗尺寸基准：屏幕短边的 1/480 为一个单位。不论 720p、1080p、4K 或手机，弹窗占屏幕的比例都一致。 */
    public static float unit(Activity a) {
        android.util.DisplayMetrics dm = a.getResources().getDisplayMetrics();
        return Math.min(dm.widthPixels, dm.heightPixels) / 480f;
    }

    public static int px(Activity a, float ref) {
        return (int) (ref * unit(a) + 0.5f);
    }

    public static void size(Activity a, android.widget.TextView t, float ref) {
        t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, ref * unit(a));
    }

    /** 所有弹窗统一按屏幕比例定宽：手机竖屏占 92%，横屏/电视占 62%。 */
    public static int dialogWidth(Activity activity) {
        android.util.DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        boolean phone = activity.getResources().getConfiguration().smallestScreenWidthDp < 600;
        float ratio = (phone && dm.widthPixels < dm.heightPixels) ? 0.92f : 0.62f;
        return (int) (dm.widthPixels * ratio);
    }

    public static void fit(Activity activity, android.app.Dialog dialog) {
        if (dialog == null || dialog.getWindow() == null) return;
        dialog.getWindow().setLayout(dialogWidth(activity), android.view.WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private float dp(float v) {
        return v * activity.getResources().getDisplayMetrics().density;
    }

    private final java.util.Set<String> mShownKeys = new java.util.HashSet<>();
    private android.animation.ValueAnimator mMarqueeAnim;
    private String mMarqueeKey = "";

    /** 本次打开应用内每条内容只处理一次；"每天几次"再按当天已显示次数限制。 */
    private boolean shouldShow(String key, String mode, int daily, String prefKey) {
        if (!mShownKeys.add(key)) return false;
        if (!"daily".equals(mode)) return true;
        String today = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(new java.util.Date());
        String day = Prefers.getString(prefKey + "_day");
        int count = today.equals(day) ? Prefers.getInt(prefKey + "_cnt", 0) : 0;
        if (count >= daily) return false;
        Prefers.put(prefKey + "_day", today);
        Prefers.put(prefKey + "_cnt", count + 1);
        return true;
    }

    public void showPopup(FusionApi.Popup popup) {
        if (popup == null) return;
        if ("once".equals(popup.mode)) {
            NoticeDialog.maybeShow(activity, popup);
        } else if (shouldShow("pp|" + popup.version + "|" + popup.mode + "|" + popup.daily, popup.mode, popup.daily, "fusiontv_pp")) {
            NoticeDialog.showNow(activity, popup);
        }
    }

    public void stop() {
        if (mMarqueeAnim != null) {
            android.animation.ValueAnimator anim = mMarqueeAnim;
            mMarqueeAnim = null;
            anim.cancel();
        }
        mMarqueeKey = "";
        box.setVisibility(View.GONE);
    }

    public void showMarquee(FusionApi.Info info) {
        String msg = info.marquee;
        if (TextUtils.isEmpty(msg)) {
            stop();
            return;
        }
        String key = msg + "|" + info.marqueeMode + "|" + info.marqueeDaily + "|" + info.marqueeVersion;
        if (key.equals(mMarqueeKey)) return;
        stop();
        mMarqueeKey = key;
        boolean always = "always".equals(info.marqueeMode);
        if (!always && !shouldShow("mq|" + key, info.marqueeMode, info.marqueeDaily, "fusiontv_mq")) return;
        text.setEllipsize(null);
        text.setText(msg);
        box.setVisibility(View.VISIBLE);
        box.post(() -> startMarquee(always, 0));
    }

    private void startMarquee(boolean always, int retry) {
        if (activity.isFinishing() || box.getVisibility() != View.VISIBLE) return;
        int boxWidth = box.getWidth();
        if (boxWidth <= 0) {
            if (retry < 20) box.postDelayed(() -> startMarquee(always, retry + 1), 100);
            return;
        }
        text.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int textWidth = text.getMeasuredWidth();
        android.widget.FrameLayout.LayoutParams lp = (android.widget.FrameLayout.LayoutParams) text.getLayoutParams();
        lp.width = textWidth;
        text.setLayoutParams(lp);
        float from = boxWidth;
        float to = -textWidth;
        long duration = (long) ((from - to) / (dp(90) / 1000f));
        android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofFloat(from, to);
        anim.setDuration(Math.max(duration, 3000));
        anim.setInterpolator(new android.view.animation.LinearInterpolator());
        if (always) anim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        anim.addUpdateListener(a -> text.setTranslationX((float) a.getAnimatedValue()));
        if (!always) {
            anim.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    // 滚完一遍就收起（被取消时由 stopMarquee 处理）
                    if (mMarqueeAnim == animation) box.setVisibility(View.GONE);
                }
            });
        }
        mMarqueeAnim = anim;
        anim.start();
    }
}
