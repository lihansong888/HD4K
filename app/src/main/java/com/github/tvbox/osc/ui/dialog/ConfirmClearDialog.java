package com.github.tvbox.osc.ui.dialog;

import android.app.Activity;
import android.content.Context;
import com.github.tvbox.osc.auth.FusionDialog;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.cache.VodCollect;
import com.github.tvbox.osc.ui.activity.CollectActivity;
import com.github.tvbox.osc.ui.activity.HistoryActivity;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfirmClearDialog extends BaseDialog {
    private final FusionDialog fd;

    public ConfirmClearDialog(@NonNull @NotNull Context context, String type) {
        super(context);
        fd = new FusionDialog((Activity) context, this).title("确认清空").message("确定全部删除 ?");
        setCanceledOnTouchOutside(true);
        fd.button("取消", false, v -> ConfirmClearDialog.this.dismiss());
        fd.button("确定", true, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // if removing all Favorites
                if (type == "Collect") {
                    List<VodCollect> vodInfoList = new ArrayList<>();
                    CollectActivity.collectAdapter.setNewData(vodInfoList);
                    CollectActivity.collectAdapter.notifyDataSetChanged();
                    RoomDataManger.deleteVodCollectAll();
                    // if removing all History
                } else if (type == "History") {
                    List<VodInfo> vodInfoList = new ArrayList<>();
                    HistoryActivity.historyAdapter.setNewData(vodInfoList);
                    HistoryActivity.historyAdapter.notifyDataSetChanged();
                    RoomDataManger.deleteVodRecordAll();
                }

                ConfirmClearDialog.this.dismiss();
            }
        });
    }

    @Override
    public void show() {
        super.show();
        fd.fitWindow();
    }

}
