package com.github.tvbox.osc.ui.dialog;

import android.app.Activity;
import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.auth.FusionDialog;
import com.github.tvbox.osc.auth.FusionUi;

import org.jetbrains.annotations.NotNull;

public class LivePasswordDialog extends BaseDialog {
    private final EditText inputPassword;
    private final FusionDialog fd;

    public LivePasswordDialog(@NonNull @NotNull Context context) {
        super(context);
        Activity activity = (Activity) context;
        setOwnerActivity(activity);
        inputPassword = new EditText(activity);
        inputPassword.setSingleLine(true);
        inputPassword.setHint("请输入密码");
        inputPassword.setGravity(Gravity.CENTER);
        inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        FusionUi.size(activity, inputPassword, 16);
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(FusionUi.px(activity, 8), FusionUi.px(activity, 10), FusionUi.px(activity, 8), FusionUi.px(activity, 4));
        box.addView(inputPassword, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        fd = new FusionDialog(activity, this).title("直播密码").content(box);
        fd.button("取消", false, v -> {
            if (listener != null) listener.onCancel();
            dismiss();
        });
        fd.button("确定", true, v -> {
            String password = inputPassword.getText().toString().trim();
            if (!password.isEmpty()) {
                listener.onChange(password);
                dismiss();
            }
        });
    }

    @Override
    public void show() {
        super.show();
        fd.fitWindow();
        inputPassword.requestFocus();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (listener != null) listener.onCancel();
        dismiss();
    }

    public void setOnListener(OnListener listener) {
        this.listener = listener;
    }

    OnListener listener = null;

    public interface OnListener {
        void onChange(String password);
        void onCancel();
    }
}
