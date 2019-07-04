package cn.demo.overlay;

import android.app.Activity;

import cn.demo.overlaylibrary.LauncherClient;
import cn.demo.overlaylibrary.LauncherClientCallbacks;

class Util {
    static LauncherClient getClient(Activity a, LauncherClientCallbacks cb) {
        return new LauncherClient(a, cb, "cn.demo.server", true);
    }
}
