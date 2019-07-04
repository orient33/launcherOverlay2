package cn.demo.server;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class OverlayService extends Service {
    private OverlaysController controller;

     @Override
    public void onCreate() {
        super.onCreate();
        controller = new OverlaysController(this);
    }

    @Override
    public void onDestroy() {
        controller.onDestroy();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return controller.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        controller.onUnbind(intent);
        return false;
    }
}
