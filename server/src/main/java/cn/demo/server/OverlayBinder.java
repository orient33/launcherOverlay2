package cn.demo.server;

import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManager;

import cn.demo.overlaylibrary.ILauncherOverlay;
import cn.demo.overlaylibrary.ILauncherOverlayCallback;

import static cn.demo.server.C.ARG1_OVERLAY_CLOSE;
import static cn.demo.server.C.ARG1_OVERLAY_OPEN;
import static cn.demo.server.C.ARG1_WINDOW_ATTACH;
import static cn.demo.server.C.ARG1_WINDOW_DETACH;
import static cn.demo.server.C.MSG_CREATE_DESTROY;
import static cn.demo.server.C.MSG_END_SCROLL;
import static cn.demo.server.C.MSG_LIFE_STATE;
import static cn.demo.server.C.MSG_ON_SCROLL;
import static cn.demo.server.C.MSG_START_SCROLL;
import static cn.demo.server.C.MSG_TOGGLE_OVERLAY;
import static cn.demo.server.C.MSG_WINDOW;

public class OverlayBinder extends ILauncherOverlay.Stub implements Runnable {
    //    private final OverlaysController controller;
    final String mClientPkg;
    private final int mCallerUid;
    final int mServerVersion;
    final int mClientVersion;
    private final Handler mMainHandler;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    OverlayBinder(OverlaysController oc, int callerUid, String pkg,
                  int serverVersion, int clientVersion) {
//        controller = close;
        mCallerUid = callerUid;
        mClientPkg = pkg;
        mServerVersion = serverVersion;
        mClientVersion = clientVersion;
        mMainHandler = new Handler(Looper.getMainLooper(), new MinusOneOverlayCallback(this, oc));
    }

    private void checkCallerId() {
        if (Binder.getCallingUid() != mCallerUid) {
            throw new RuntimeException("Invalid client. because of UID.");
        }
    }

    @Override
    public final synchronized void startScroll() {
        checkCallerId();
        Message.obtain(mMainHandler, MSG_START_SCROLL).sendToTarget();
    }

    @Override
    public final synchronized void onScroll(float progress) {
//        checkCallerId();
        Message.obtain(mMainHandler, MSG_ON_SCROLL, progress).sendToTarget();
    }

    @Override
    public final synchronized void endScroll() {
        checkCallerId();
        Message.obtain(mMainHandler, MSG_END_SCROLL).sendToTarget();
    }

    @Override
    public final synchronized void windowAttached(WindowManager.LayoutParams p, ILauncherOverlayCallback callback, int option) {
        checkCallerId();
        Bundle bundle = new Bundle();
        bundle.putParcelable(C.KEY_LP, p);
        bundle.putInt(C.KEY_OPT, option);
//        overlaysController.handler.removeCallbacks(this);

//        Configuration configuration = bundle.getParcelable("configuration");//暂未解析
//        mLastAttachWasLandscape = configuration != null && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE;
//        create(bundle.getInt("client_options", 7));// 重新设置callback, 暂不需要
        createOrDestroy(true);
        Message.obtain(mMainHandler, MSG_WINDOW, ARG1_WINDOW_ATTACH, 0
                , Pair.create(bundle, callback)).sendToTarget();
    }

    @Override
    public final synchronized void windowDetached(boolean isConfigChange) {
        Message.obtain(mMainHandler, MSG_WINDOW, ARG1_WINDOW_DETACH, 0).sendToTarget();
        mHandler.postDelayed(this, isConfigChange ? 5000 : 0);
    }

    @Override
    public final synchronized void onPause() {
        onLifeState(C.LIFE_PAUSE);
    }

    @Override
    public final synchronized void onResume() {
        onLifeState(C.LIFE_RESUME);
    }

    @Override
    public void onLifeState(int state) {
        checkCallerId();
        mMainHandler.removeMessages(MSG_LIFE_STATE);
        if ((state & 2) == 0) {
            mMainHandler.sendMessageDelayed(
                    Message.obtain(mMainHandler, MSG_LIFE_STATE, state, 0), 200);
        } else {
            Message.obtain(mMainHandler, MSG_LIFE_STATE, state, 0).sendToTarget();
        }
    }

    @Override
    public final synchronized void openOverlay(int v) {
        checkCallerId();
        mMainHandler.removeMessages(MSG_TOGGLE_OVERLAY);
        Message.obtain(mMainHandler, MSG_TOGGLE_OVERLAY, ARG1_OVERLAY_OPEN, v).sendToTarget();
    }

    @Override
    public final synchronized void closeOverlay(int v) {
        checkCallerId();
        mMainHandler.removeMessages(MSG_TOGGLE_OVERLAY);
        Message.obtain(mMainHandler, MSG_TOGGLE_OVERLAY, ARG1_OVERLAY_CLOSE, v).sendToTarget();
    }

    @Override
    public void run() {
        createOrDestroy(false);
    }

    final void destroy() {
        synchronized (OverlayBinder.class) {
            mHandler.removeCallbacks(this);
            createOrDestroy(false);
        }
    }

    private synchronized void createOrDestroy(boolean create) {
//        synchronized (this) {
        Message.obtain(mMainHandler, MSG_CREATE_DESTROY, create ? C.ARG1_WINDOW_ATTACH
                : ARG1_WINDOW_DETACH, 0).sendToTarget();
    }

    final void replay(ILauncherOverlayCallback cb, int flag) {
        if (cb != null) {
            try {
                cb.overlayStatusChanged(flag | 24);
            } catch (RemoteException e) {
                Log.e("df", "OverlayBinder.reply. fail. " + e);
            }
        }

    }
}
