package cn.demo.overlaylibrary;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PatternMatcher;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LauncherClient {
    private static final String TAG = "LauncherClient";
    private static AppServiceConnection sAppConnection;

    private final Activity mActivity;
    private OverlayCallbacks mCurrentCallbacks;
    private boolean mDestroyed;
    private boolean mIsResumed;
    private LauncherClientCallbacks mLauncherClientCallbacks;
    @Nullable
    private ILauncherOverlay mOverlay;
    private OverlayServiceConnection mServiceConnection;
    private int mServiceConnectionOptions;
    @NonNull
    private final Intent mServiceIntent;
    private int mServiceStatus;
    private int mState;
    private final BroadcastReceiver mUpdateReceiver;
    private WindowManager.LayoutParams mWindowAttrs;

    public LauncherClient(Activity activity, LauncherClientCallbacks callbacks, String targetPackage, boolean overlayEnabled) {
        mUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                reconnectIfNeed();
            }
        };
        mIsResumed = false;
        mDestroyed = false;
        mServiceStatus = -1;
        mActivity = activity;
        mServiceIntent = LauncherClient.getServiceIntent(activity, targetPackage);
        mLauncherClientCallbacks = callbacks;
        mState = 0;
        mServiceConnection = new OverlayServiceConnection();
        mServiceConnectionOptions = overlayEnabled ? 3 : 2;

        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addDataScheme("package");
        filter.addDataSchemeSpecificPart(targetPackage, PatternMatcher.PATTERN_LITERAL);
        mActivity.registerReceiver(mUpdateReceiver, filter);
        reconnectIfNeed();
    }

//    private static void loadApiVersion(Context context, Intent intent) {
//        ResolveInfo resolveService = context.getPackageManager().resolveService(intent, PackageManager.GET_META_DATA);
//        apiVersion = resolveService == null || resolveService.serviceInfo.metaData == null ? 1 :
//                resolveService.serviceInfo.metaData.getInt("service.api.version", 1);
//    }

    //使用这个可以 平滑显示 Google Now, (需要在Pixel手机, or PixelExperience 之类的ROM)
    @SuppressWarnings("unused")
    public LauncherClient(Activity activity, LauncherClientCallbacks callbacks, boolean overlayEnabled) {
        this(activity, callbacks, "com.google.android.googlequicksearchbox", overlayEnabled);
    }

    public void startMove() {
        if (mOverlay == null) return;

        try {
            mOverlay.startScroll();
        } catch (RemoteException ignored) {
        }
    }

    public void onScrolled(float progressX) {
        if (mOverlay == null) return;

        try {
            mOverlay.onScroll(progressX);
        } catch (RemoteException ignored) {
        }
    }

    public void endMove() {
        if (mOverlay == null) return;

        try {
            mOverlay.endScroll();
        } catch (RemoteException ignored) {
        }
    }

    public void openOverlay(boolean anim) {
        if (mOverlay == null) return;

        try {
            mOverlay.openOverlay(anim ? 1 : 0);
        } catch (RemoteException ignored) {
        }
    }

    public void hideOverlay(boolean animate) {
        if (mOverlay == null) return;

        try {
            mOverlay.closeOverlay(animate ? 1 : 0);
        } catch (RemoteException ignored) {
        }
    }

    public final void onAttachedToWindow() {
        if (mDestroyed) return;
        setWindowAttrs(mActivity.getWindow().getAttributes());
    }

    public final void onDetachedFromWindow() {
        if (mDestroyed) return;
        setWindowAttrs(null);
    }

    public void onPause() {
        if (mDestroyed) return;
        mIsResumed = false;
        if (mOverlay != null && mWindowAttrs != null) {
            try {
                mOverlay.onPause();
            } catch (RemoteException ignored) {
            }
        }
    }

    public void onResume() {
        if (mDestroyed) return;
        reconnectIfNeed();
        mIsResumed = true;
        if (mOverlay != null && mWindowAttrs != null) {
            try {
                mOverlay.onResume();
            } catch (RemoteException ignored) {
            }
        }
    }

    public void onDestroy() {
        mDestroyed = true;
        try {
            mActivity.unbindService(mServiceConnection);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "unbind fail. " + e);
        }
        mActivity.unregisterReceiver(mUpdateReceiver);
        if (mCurrentCallbacks != null) {
            mCurrentCallbacks.clear();
            mCurrentCallbacks = null;
        }
        if (!mActivity.isChangingConfigurations() && sAppConnection != null) {
            mActivity.getApplicationContext().unbindService(sAppConnection);
            sAppConnection = null;
        }
    }

    private void setWindowAttrs(@Nullable WindowManager.LayoutParams windowAttrs) { //detach 时 为null
        mWindowAttrs = windowAttrs;
        if (mWindowAttrs != null) {
            applyWindowToken();
        } else if (mOverlay != null) {
            try {
                mOverlay.windowDetached(mActivity.isChangingConfigurations());
            } catch (RemoteException ignored) {
            }
            mOverlay = null;
        }
    }

    private void reconnectIfNeed() {
        if (mDestroyed || mState != C.STATE_IDLE) {
            return;
        }

        if (sAppConnection != null && !sAppConnection.serverPkgName.equals(mServiceIntent.getPackage())) {
            mActivity.getApplicationContext().unbindService(sAppConnection);
        }

        if (sAppConnection == null) {
            sAppConnection = new AppServiceConnection(mServiceIntent.getPackage());

            if (!connectSafely(mActivity.getApplicationContext(), sAppConnection, Context.BIND_WAIVE_PRIORITY)) {
                sAppConnection = null;
            }
        }

        if (sAppConnection != null) {
            mState = C.STATE_CONNECTING;

            if (!connectSafely(mActivity, mServiceConnection, Context.BIND_ADJUST_WITH_ACTIVITY)) {
                mState = C.STATE_IDLE;
            }
        }

        if (mState == C.STATE_IDLE) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyStatusChanged(C.STATE_IDLE);
                }
            });
        }
    }

    private void notifyStatusChanged(int status) {
        if (mServiceStatus != status) {
            mServiceStatus = status;
            mLauncherClientCallbacks.onServiceStateChanged((status & 1) != 0, true);
        }
    }

    private void applyWindowToken() {
        if (mOverlay == null) {
            Log.e(TAG, "applyWindowToken() . mOverlay == null");
            return;
        }
        if (mCurrentCallbacks == null) {
            Log.i(TAG, "new OverlayCallbacks()");
            mCurrentCallbacks = new OverlayCallbacks();
        }
        mCurrentCallbacks.setClient(this);
        try {
            mOverlay.windowAttached(mWindowAttrs, mCurrentCallbacks, mServiceConnectionOptions);
            if (mIsResumed) {
                mOverlay.onResume();
            } else {
                mOverlay.onPause();
            }
        } catch (RemoteException ignored) {
        }
    }

    private boolean connectSafely(Context context, ServiceConnection conn, int flags) {
        try {
            boolean ok = context.bindService(mServiceIntent, conn, flags | Context.BIND_AUTO_CREATE);
            Log.d(TAG, "bind service, success = " + ok + ", : " + mServiceIntent);
            return ok;
        } catch (SecurityException e) {
            Log.e(TAG, "Unable to connect to overlay service");
            return false;
        }
    }

    private static Intent getServiceIntent(Context context, String targetPackage) {
        Uri uri = Uri.parse("app://" + context.getPackageName() + ":" + Process.myUid()).buildUpon()
                .appendQueryParameter("v", Integer.toString(BuildConfig.VERSION_CODE))
                .build();
        return new Intent("com.android.launcher3.WINDOW_OVERLAY")
                .setPackage(targetPackage)
                .setData(uri);
    }


    final class AppServiceConnection implements ServiceConnection {
        final String serverPkgName;

        AppServiceConnection(String pkg) {
            serverPkgName = pkg;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        public void onServiceDisconnected(ComponentName name) {
            if (name.getPackageName().equals(serverPkgName)) {
                sAppConnection = null;
            }
        }
    }

    private static class OverlayCallbacks extends ILauncherOverlayCallback.Stub implements Handler.Callback {
        private LauncherClient mClient;
        private final Handler mUIHandler;
        private Window mWindow;
        private boolean mWindowHidden;
        private WindowManager mWindowManager;
        private int mWindowShift;

        OverlayCallbacks() {
            mWindowHidden = false;
            mUIHandler = new Handler(Looper.getMainLooper(), this);
        }

        void setClient(LauncherClient client) {
            mClient = client;
            mWindowManager = client.mActivity.getWindowManager();

            Point p = new Point();
            mWindowManager.getDefaultDisplay().getRealSize(p);
            mWindowShift = Math.max(p.x, p.y);

            mWindow = client.mActivity.getWindow();
        }

        private void hideActivityNonUI(boolean isHidden) {
            if (mWindowHidden != isHidden) {
                mWindowHidden = isHidden;
            }
        }

        void clear() {
            mClient = null;
            mWindowManager = null;
            mWindow = null;
        }

        @Override
        public boolean handleMessage(Message msg) {
            if (mClient == null) {
                Log.e(TAG, "OverlayCallbacks. handleMessage. mClient == null");
                return true;
            }
            Log.d(TAG, "handleMessage: " + msg.what);
            switch (msg.what) {
                case C.MSG_SCROLL_CHANGED:
                    if ((mClient.mServiceStatus & 1) != 0) {
                        Log.d(TAG, "(float) msg.obj:" + (float) msg.obj);
                        mClient.mLauncherClientCallbacks.onOverlayScrollChanged((float) msg.obj);
                    }
                    return true;
                case 3: //:TODO 未知.
                    WindowManager.LayoutParams attrs = mWindow.getAttributes();
                    if ((boolean) msg.obj) {
                        attrs.x = mWindowShift;
                        attrs.flags = attrs.flags | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
                    }
                    mWindowManager.updateViewLayout(mWindow.getDecorView(), attrs);
                    return true;
                case C.MSG_STATUS_CHANGED:
                    mClient.notifyStatusChanged(msg.arg1);
                    return true;
                default:
                    return false;
            }
        }

        // ---- implement ILauncherOverlayCallback.Stub , server will callback. ---
        @Override
        public void overlayScrollChanged(float progress) {
            mUIHandler.removeMessages(C.MSG_SCROLL_CHANGED);
            Message.obtain(mUIHandler, C.MSG_SCROLL_CHANGED, progress).sendToTarget();

            if (progress > 0) {
                hideActivityNonUI(false);
            }
        }

        @Override
        public void overlayStatusChanged(int status) {
            Message.obtain(mUIHandler, C.MSG_STATUS_CHANGED, status, 0).sendToTarget();
        }
        // ---- implement ILauncherOverlayCallback.. --- end ------
    }

    private class OverlayServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "OverlayServiceConnection.onServiceConnected()");
            mState = C.STATE_CONNECTED;
            mOverlay = ILauncherOverlay.Stub.asInterface(service);
            if (mWindowAttrs != null) {
                applyWindowToken();
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            mState = C.STATE_IDLE;
            mOverlay = null;
            notifyStatusChanged(C.STATE_IDLE);
        }
    }
}
