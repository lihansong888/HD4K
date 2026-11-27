package com.github.tvbox.osc.ui.dialog;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.auth.FusionDialog;

import org.jetbrains.annotations.NotNull;

public class AboutDialog extends BaseDialog {
    private final FusionDialog fd;

    public AboutDialog(@NonNull @NotNull Context context) {
        super(context);
        fd = new FusionDialog((Activity) context, this)
                .title("关于")
                .message("高清4K影视所有资源均采集于互联网络公开渠道，仅供个人学习爬虫、网络技术研究使用，禁止私自转载、售卖、商用牟利。所有内容版权归属原站点及版权方，软件仅做内容检索整理，不存储违规资源，不对第三方内容承担任何法律与连带责任。");
        fd.messageView().setGravity(Gravity.START);
        setCanceledOnTouchOutside(true);
        fd.button("确定", true, v -> dismiss());
    }

    @Override
    public void show() {
        super.show();
        fd.fitWindow();
    }
}
