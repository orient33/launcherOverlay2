package cn.demo.overlaylibrary;

public interface LauncherClientCallbacks {
    void onOverlayScrollChanged(float progress);

    void onServiceStateChanged(boolean overlayAttached, boolean hotwordActive);
}
