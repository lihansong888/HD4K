package com.github.tvbox.osc.auth;

import com.orhanobut.hawk.Hawk;

/** 适配层：用 Hawk 实现认证代码用到的简单键值存储。 */
final class Prefers {
    private Prefers() {
    }

    static String getString(String key) {
        return Hawk.get(key, "");
    }

    static String getString(String key, String def) {
        return Hawk.get(key, def);
    }

    static boolean getBoolean(String key, boolean def) {
        return Hawk.get(key, def);
    }

    static int getInt(String key, int def) {
        return Hawk.get(key, def);
    }

    static void put(String key, Object value) {
        Hawk.put(key, value);
    }

    static void remove(String key) {
        Hawk.delete(key);
    }
}
