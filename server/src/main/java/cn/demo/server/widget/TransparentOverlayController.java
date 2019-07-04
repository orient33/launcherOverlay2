package cn.demo.server.widget;

import android.os.Build;
import android.util.Log;
import android.view.WindowManager.LayoutParams;

import cn.demo.server.ui.OverlayUI;

public final class TransparentOverlayController implements DragListener {

    private final OverlayUI overlayUI;

    public TransparentOverlayController(OverlayUI ui) {
        overlayUI = ui;
    }

    public final void drag() {
        Log.d("wo.OverlayUI", "Drag event called in transparent mode");
    }

    public final void dragTouchable() {
    }

    public final void close(boolean z) {
    }

    public final void open() {
        overlayUI.setTouchable(true);
        LayoutParams attributes = overlayUI.window.getAttributes();
        if (Build.VERSION.SDK_INT >= 26) {
            float f = attributes.alpha;
            attributes.alpha = 1.0f;
            if (f != attributes.alpha) {
                overlayUI.window.setAttributes(attributes);
            }
        } else {
            attributes.x = 0;
            attributes.flags &= ~LayoutParams.FLAG_LAYOUT_NO_LIMITS;//-513;
            overlayUI.unZ = true;
            overlayUI.window.setAttributes(attributes);
        }
        overlayUI.setState(PanelState.OPEN_AS_LAYER);//Todo: PanelState.uoh was default
    }

    public final void close() {
        LayoutParams attributes = overlayUI.window.getAttributes();
        if (Build.VERSION.SDK_INT >= 26) {
            float f = attributes.alpha;
            attributes.alpha = 0.0f;
            if (f != attributes.alpha) {
                overlayUI.window.setAttributes(attributes);
            }
        } else {
            attributes.x = overlayUI.mWindowShift;
            attributes.flags |= LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            overlayUI.unZ = false;
            overlayUI.window.setAttributes(attributes);
        }
        overlayUI.setTouchable(false);
        if (overlayUI.panelState != PanelState.CLOSED) {
            overlayUI.panelState = PanelState.CLOSED;
            overlayUI.setState(overlayUI.panelState);
        }
        overlayUI.slidingPanelLayout.drag = overlayUI.overlayControllerStateChanger;
    }

    public final void overlayScrollChanged(float f) {
    }

    public final boolean cnI() {
        return true;
    }
}
