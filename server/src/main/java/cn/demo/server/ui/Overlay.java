package cn.demo.server.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import cn.demo.server.R;

//模拟activity
public class Overlay extends OverlayUI {

    public Overlay(Context ctx, int theme, int dialogTheme) {
        super(ctx, theme, dialogTheme);
    }

    @Override
    public final void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        View v = setContentView(R.layout.server);
        v.findViewById(android.R.id.text1).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                PackageManager pm = getPackageManager();
                startActivity(pm.getLaunchIntentForPackage("com.android.settings"));
            }
        });
    }
}
