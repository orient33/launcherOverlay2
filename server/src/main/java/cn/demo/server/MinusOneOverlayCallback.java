package cn.demo.server;

import android.content.res.Configuration;
import android.os.Message;

import cn.demo.server.ui.Overlay;
import cn.demo.server.ui.OverlayUI;
import cn.demo.server.widget.SlidingPanelLayout;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;
import static cn.demo.server.C.MSG_END_SCROLL;
import static cn.demo.server.C.MSG_ON_SCROLL;
import static cn.demo.server.C.MSG_START_SCROLL;

public final class MinusOneOverlayCallback extends OverlayControllerCallback {

    private final OverlaysController overlaysController;

    MinusOneOverlayCallback(OverlayBinder binder, OverlaysController oc) {
        super(binder, C.Callback_Minus);
        overlaysController = oc;
    }

    @Override
    final Overlay createController(Configuration configuration) {
        return overlaysController.createOverlay(configuration, mBinder.mServerVersion, mBinder.mClientVersion);
    }

    public final boolean handleMessage(Message message) {
        if (super.handleMessage(message)) {
            return true;
        }
        if (ui == null) return false;
        final OverlayUI localUI = ui;
        long when;
        switch (message.what) {
            case MSG_START_SCROLL:
                when = message.getWhen();
                if (!localUI.isPanelOpen()) {
                    SlidingPanelLayout slidePanel = localUI.slidingPanelLayout;
                    if (slidePanel.panelX < slidePanel.mTouchSlop) {
                        localUI.slidingPanelLayout.setPanelX(0);
                        localUI.mAcceptExternalMove = true;
                        localUI.eventX = 0;
                        localUI.slidingPanelLayout.mForceDrag = true;
                        localUI.downTime = when - 30;
                        localUI.dispatchTouchEvent(ACTION_DOWN, localUI.eventX, localUI.downTime);
                        localUI.dispatchTouchEvent(ACTION_MOVE, localUI.eventX, when);
                    }
                }
                return true;
            case MSG_ON_SCROLL /*4*/:
                float floatValue = (float) message.obj;
                when = message.getWhen();
                if (localUI.mAcceptExternalMove) {
                    localUI.eventX = (int) (floatValue * ((float) localUI.slidingPanelLayout.getMeasuredWidth()));
                    localUI.dispatchTouchEvent(ACTION_MOVE, localUI.eventX, when);
                }
                return true;
            case MSG_END_SCROLL:
                when = message.getWhen();
                if (localUI.mAcceptExternalMove) {
                    localUI.dispatchTouchEvent(ACTION_UP, localUI.eventX, when);
                }
                localUI.mAcceptExternalMove = false;
                return true;
            default:
                return false;
        }
    }
}
