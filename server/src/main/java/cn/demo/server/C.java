package cn.demo.server;

import android.os.Message;

//一些常量: 分析时将它重命名为有意义的值.
public class C {
    public static final int MSG_WINDOW = 0; // window attach 或 detach ,视msg.arg1定
    public static final int MSG_LIFE_STATE = 1; // 0-pause, 3-resume.//偶数时会delay 100ms
    public static final int MSG_CREATE_DESTROY = 2;// arg1=1 create.. 0 destroy
    public static final int MSG_START_SCROLL = 3;
    public static final int MSG_ON_SCROLL = 4;
    public static final int MSG_END_SCROLL = 5;
    public static final int MSG_TOGGLE_OVERLAY = 6;// arg1=0 open, arg1
    public static final int MSG_REQUEST_VOICE = 7;

    public static final int LIFE_PAUSE = 0;
    public static final int LIFE_RESUME = 3;

    public static final int ARG1_OVERLAY_OPEN = 1;
    public static final int ARG1_OVERLAY_CLOSE = 0;

    public static final int ARG1_WINDOW_ATTACH = 1;
    public static final int ARG1_WINDOW_DETACH = 0;

    public static final int Callback_Minus = 3;

    public static final String KEY_LP = "layout_params";
    public static final String KEY_OPT = "client_options";

    public static String msg2String(Message msg) {
        switch (msg.what) {
            case MSG_CREATE_DESTROY:
                return ARG1_WINDOW_ATTACH == msg.arg1 ? "create" : "destroy";
            case MSG_WINDOW:
                return ARG1_WINDOW_ATTACH == msg.arg1 ? "windowAttach" : "windowDetach";
            case MSG_TOGGLE_OVERLAY:
                return ARG1_OVERLAY_OPEN == msg.arg1 ? "overlayOpen" : "overlayClose";
            case MSG_LIFE_STATE:
                return "life.state " + msg.arg1;
            case MSG_START_SCROLL:
                return "startScroll.";
            case MSG_END_SCROLL:
                return "endScroll";
            default:
                return "unknown." + msg.what;
        }
    }

}
