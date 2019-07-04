package cn.demo.server.ui;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Point;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;

import cn.demo.overlaylibrary.ILauncherOverlayCallback;
import cn.demo.server.LogTool;
import cn.demo.server.widget.DragListener;
import cn.demo.server.widget.OverlayControllerSlidingPanelLayout;
import cn.demo.server.widget.OverlayControllerStateChanger;
import cn.demo.server.widget.PanelState;
import cn.demo.server.widget.SlidingPanelLayout;
import cn.demo.server.widget.TransparentOverlayController;

public class OverlayUI extends DialogOverlayController {

    public boolean mIsRtl;
    public long downTime = 0;
    public int mWindowShift;
    public String mPackageName;
    public SlidingPanelLayout slidingPanelLayout;
    public DragListener overlayControllerStateChanger = new OverlayControllerStateChanger(this);
    //    public FrameLayout container;
    public int eventX = 0;
    public boolean mAcceptExternalMove = false;
    public boolean unZ = true;
    public ILauncherOverlayCallback callback;
    public PanelState panelState = PanelState.CLOSED;
    public int mActivityStateFlags = 0;

    public OverlayUI(Context context, int theme, int dialogTheme) {
        super(context, theme, dialogTheme);
    }

    public void beforeCreate(String clientPkg, WindowManager.LayoutParams layoutParams) {
        mIsRtl = SlidingPanelLayout.isRtl(getResources());
        mPackageName = clientPkg;
        window.setWindowManager(null, layoutParams.token,
                new ComponentName(this, getBaseContext().getClass()).flattenToShortString(),
                true);// 这个看着有些 少见!//:注释掉也没发现有什么影响
        windowManager = window.getWindowManager();
        Point point = new Point();
        windowManager.getDefaultDisplay().getRealSize(point);
        mWindowShift = -Math.max(point.x, point.y);
        slidingPanelLayout = new OverlayControllerSlidingPanelLayout(this);//:TODO 待细看
        slidingPanelLayout.drag = overlayControllerStateChanger;
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.flags |= WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH;//8650752
        layoutParams.dimAmount = 0.0f;
        layoutParams.gravity = Gravity.LEFT; //3
        layoutParams.type = //WindowManager.LayoutParams.FIRST_SUB_WINDOW + 1;
                Build.VERSION.SDK_INT >= 25 ? WindowManager.LayoutParams.TYPE_DRAWN_APPLICATION
                        : WindowManager.LayoutParams.TYPE_APPLICATION;
        layoutParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;//16
        window.setTitle("Over");
        window.setAttributes(layoutParams);
        window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);//1048576);
    }

    @CallSuper
    public void onCreate(Bundle bundle) {
        LogTool.log("OverlayController. onCreate. register bro");
    }

    protected View setContentView(@LayoutRes int layoutId) {
        slidingPanelLayout.setFitsSystemWindows(true);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        View v = LayoutInflater.from(slidingPanelLayout.getContext()).inflate(layoutId, slidingPanelLayout, false);
        slidingPanelLayout.addContentView(v);
        window.setContentView(slidingPanelLayout);
        decorView = window.getDecorView();
        windowManager.addView(decorView, window.getAttributes());//layoutParams);//ui.window.getAttributes());
        slidingPanelLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);//1792
        setTouchable(false);
        return v;
    }

    @CallSuper
    public void onPause() {
        LogTool.log("OverlayController. onPause");
    }

    @CallSuper
    public void onStop() {
        LogTool.log("OverlayController. onStop");
    }

    @CallSuper
    public void onStart() {
        LogTool.log("OverlayController. onStart");
    }

    @CallSuper
    public void onResume() {
        LogTool.log("OverlayController. onResume");
    }

    @CallSuper
    public void onDestroy() {
        LogTool.log("OverlayController. onDestroy");
    }

    //反编译pixel-launcher,
    // onPause -> state &= -3; 即 state &= ~10
    // onResume-> state |= 2; 即  state |= 10
    // onStart -> state |= 1; 即  state |= 01
    // onStop ->  state &= ~1; 即 state &= ~01
    public void onLifeState(int state) { //:TODO call life.

    }

    @Override
    public void onBackPressed() {
        closeOverlay(1);
    }

    public final void dispatchTouchEvent(int action, int x, long eventTime) {
        MotionEvent obtain = MotionEvent.obtain(downTime, eventTime, action,
                mIsRtl ? (float) (-x) : (float) x, 10.0f, 0);
        obtain.setSource(InputDevice.SOURCE_TOUCHSCREEN);//4098
        slidingPanelLayout.dispatchTouchEvent(obtain);
        LogTool.log("dispatch . action. " + action + ", x=" + x);
        obtain.recycle();
    }

    public final ILauncherOverlayCallback destroy() {
        onLifeState(0);
        try {
            windowManager.removeView(decorView);
        } catch (Throwable t) {
            LogTool.w("OverlayUI removeView ." + t);
        }
        decorView = null;
        onDestroy();
        return callback;
    }

    public void setState(PanelState state) {
        if (state != panelState) {
            panelState = state;
        }
    }

    public boolean isPanelOpen() {
        return panelState == PanelState.OPEN_AS_LAYER || panelState == PanelState.OPEN_AS_DRAWER;
    }

    public void closeOverlay(int value) {
        LogTool.log("OverlayController.closeOverlay " + value);
        int i2 = 1;
        int duration = 0;
        if (isPanelOpen()) {
            int i4 = (value & 1) != 0 ? 1 : 0;
            if (panelState == PanelState.OPEN_AS_LAYER) {
                i2 = 0;
            }
            i4 &= i2;
            if (i4 != 0) {
                duration = 750;
            }
            slidingPanelLayout.closePanel(duration);
        }
    }

    // param is 1 or 0
    public final void openOverlay(int value) {
        LogTool.log("OverlayController. openOverlay " + value);
        int i2 = 0;
        if (this.panelState == PanelState.CLOSED) {
            int i3 = (value & 1) != 0 ? 1 : 0;
            if ((value & 2) != 0) {
                slidingPanelLayout.drag = new TransparentOverlayController(this);
                i3 = 0;
            }
            if (i3 != 0) {
                i2 = 750;
            }
            slidingPanelLayout.openPanel(i2);
        }
    }

    public final void setTouchable(boolean touchable) {
        LogTool.log("OverlayController.setTouchable. " + touchable);
        final int flag = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        if (touchable) {
            window.clearFlags(flag);
        } else {
            window.addFlags(flag);
        }
    }
}
