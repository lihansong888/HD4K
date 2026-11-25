package com.github.tvbox.osc.auth;

import android.content.Context;

import com.github.tvbox.osc.base.App;

/** 适配层：提供全局 Context。 */
final class Init {
    private Init() {
    }

    static Context context() {
        return App.getInstance();
    }
}
