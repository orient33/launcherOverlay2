package cn.demo.server;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Pair;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import cn.demo.overlaylibrary.ILauncherOverlayCallback;
import cn.demo.server.ui.Overlay;
import cn.demo.server.ui.OverlayUI;
import cn.demo.server.widget.SlidingPanelLayout;

abstract class OverlayControllerCallback implements Handler.Callback {
    final OverlayBinder mBinder;
    private final int callbackFlag;
    @Nullable
    OverlayUI ui;

    OverlayControllerCallback(OverlayBinder b, int flag) {
        mBinder = b;
        callbackFlag = flag;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what != C.MSG_ON_SCROLL)
            LogTool.log("OverlayControllerCallback. " + C.msg2String(msg));
        switch (msg.what) {
            case C.MSG_WINDOW:
                if (msg.arg1 == C.ARG1_WINDOW_DETACH)
                    return true;
                Bundle bundle = null;
                if (ui != null) { //保存之前的.并销魂之前的overlay
                    bundle = new Bundle();
                    if (ui.isPanelOpen()) {
                        bundle.putBoolean("open", true);
                    }
                    if (ui.window != null) {
                        bundle.putParcelable("view_state", ui.window.saveHierarchyState());
                    }
                    ui.destroy();
                    ui = null;
                }

                Pair pair = (Pair) msg.obj;
                WindowManager.LayoutParams layoutParams = ((Bundle) pair.first).getParcelable(C.KEY_LP);//"layout_params");
                ui = createController((Configuration) ((Bundle) pair.first).getParcelable("configuration"));
                try {
                    ui.beforeCreate(mBinder.mClientPkg, layoutParams);
                    ui.onCreate((Bundle) pair.first);
                    if (bundle != null) {
                        if (ui.window != null)
                            ui.window.restoreHierarchyState(bundle.getBundle("view_state"));
                        if (bundle.getBoolean("open")) {
                            SlidingPanelLayout panelLayout = ui.slidingPanelLayout;
                            panelLayout.mPanelPositionRatio = 1.0f;
                            panelLayout.panelX = panelLayout.getMeasuredWidth();
                            panelLayout.contentView.setTranslationX(panelLayout.mIsRtl ?
                                    (float) (-panelLayout.panelX) : (float) panelLayout.panelX);
                            panelLayout.onPanelOpening();
                            panelLayout.onPanelOpened();
                        }
                    }
                    ui.callback = (ILauncherOverlayCallback) pair.second;
                    mBinder.replay((ILauncherOverlayCallback) pair.second, callbackFlag);
                } catch (Exception e) {
                    LogTool.w("" + e);
                    android.util.Log.e("df", "create/attach window fail.", e);
                    Message obtain = Message.obtain();
                    obtain.what = C.MSG_CREATE_DESTROY;
                    handleMessage(obtain);
                    obtain.recycle();
                }
                return true;
            case C.MSG_LIFE_STATE:
                if (ui != null)
                    ui.onLifeState(msg.arg1);
                return true;
            case C.MSG_CREATE_DESTROY:
                if (ui != null) {
                    ILauncherOverlayCallback cb = ui.destroy();
                    ui = null;
                    if (msg.arg1 != C.ARG1_WINDOW_DETACH) return true;
                    mBinder.replay(cb, 0);// create!
                }
                return true;
            case C.MSG_TOGGLE_OVERLAY:
                if (ui != null) {
                    int value = msg.arg2 & 1;
                    if (msg.arg1 == C.ARG1_OVERLAY_OPEN) {
                        ui.openOverlay(value);
                    } else {
                        ui.closeOverlay(value);
                    }
                }
        }

        return false;
    }

    abstract Overlay createController(Configuration conf);
}
