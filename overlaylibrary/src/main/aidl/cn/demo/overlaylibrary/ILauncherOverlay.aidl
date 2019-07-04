package cn.demo.overlaylibrary;

import android.view.WindowManager.LayoutParams;
import cn.demo.overlaylibrary.ILauncherOverlayCallback;

interface ILauncherOverlay {
    void startScroll();
    void onScroll(float progress);
    void endScroll();
    void windowAttached(in LayoutParams p, in ILauncherOverlayCallback callback, int option);
    void windowDetached(boolean isConfigChange);
    void onPause();
    void onResume();
    void onLifeState(int state);//pause/resume之外的state
    void openOverlay(int v);
    void closeOverlay(int v);
}
