package com.github.tvbox.osc.ui.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.auth.FusionDialog;

import org.jetbrains.annotations.NotNull;

public class TipDialog extends BaseDialog {
    private final FusionDialog fd;

    public TipDialog(@NonNull @NotNull Context context, String tip, String left, String right, OnListener listener) {
        super(context);
        fd = new FusionDialog((Activity) context, this).title("提示").message(tip);
        setCanceledOnTouchOutside(false);
        // 统一版式：次要操作在左、主要操作在右（原来的 left 是主要操作，如“重试”）
        fd.button(right, false, v -> listener.right());
        fd.button(left, true, v -> listener.left());
        setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                listener.cancel();
            }
        });
    }

    @Override
    public void show() {
        super.show();
        fd.fitWindow();
    }

    public interface OnListener {
        void left();

        void right();

        void cancel();
    }
}
