package cn.demo.server;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.IBinder;
import android.util.SparseArray;

import java.util.Arrays;

import cn.demo.server.ui.Overlay;

class OverlaysController {
    private final Context context;
    private final SparseArray<OverlayBinder> clients = new SparseArray<>();

    OverlaysController(Service s) {
        context = s;
    }

    //Intent { act=com.android.launcher3.WINDOW_OVERLAY dat=app://top.mxlwq.launcherclientsdk:10113?v=0 pkg=io.fabianterhorst.server }
    IBinder onBind(Intent intent) {
//        int callerUid = Binder.getCallingUid();//false!
        Uri data; // app://a.b.c:uid?v=0
        int port;
        if (!"com.android.launcher3.WINDOW_OVERLAY".equals(intent.getAction())
                || (data = intent.getData()) == null
                || (port = data.getPort()) == -1) {
//                || port != callerUid) {
            LogTool.w("onBind. invalid intent. " + intent);
            return null;
        }
        String hostPkg = data.getHost();
        String[] packages = context.getPackageManager().getPackagesForUid(port);
        if (packages == null || !Arrays.asList(packages).contains(hostPkg)) {
            LogTool.w("invalid package or Uid " + hostPkg);
            return null;
        }
        int sVer = 0, cVer = 0;
        try {
            String tmp;
            if ((tmp = data.getQueryParameter("v")) != null)
                sVer = Integer.parseInt(tmp);
            if ((tmp = data.getQueryParameter("cv")) != null)
                cVer = Integer.parseInt(tmp);
        } catch (Exception e) {
            LogTool.log("fail parse version. " + data.getQuery());
        }
        OverlayBinder binder = clients.get(port);
        if (binder != null && port == binder.mServerVersion) {  //clean old
            binder.destroy();
            binder = null;
        }
        if (binder == null) {   //create new if need.
            binder = new OverlayBinder(this, port, hostPkg, sVer, cVer);
            clients.put(port, binder);
        }
        return binder;
    }

    synchronized void onUnbind(Intent intent) {
        if (intent.getData() == null) return;
        int port = intent.getData().getPort();
        if (port == -1) return;
        OverlayBinder client = clients.get(port);
        if (client != null) {
            client.destroy();
            clients.remove(port);
        }
    }

    void onDestroy() {
        for (int size = this.clients.size() - 1; size >= 0; size--) {
            OverlayBinder binder = clients.valueAt(size);
            if (binder != null) {
                binder.destroy();
            }
        }
        clients.clear();
    }

    @SuppressWarnings("unused")
    final Overlay createOverlay(Configuration conf, int sVer, int cVer) {
        Context _context = context;
        if (conf != null) {
            _context = context.createConfigurationContext(conf);
        }
        return new Overlay(_context, R.style.AppTheme, R.style.WindowTheme);
    }
}
