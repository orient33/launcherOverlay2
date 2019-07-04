package cn.demo.server;

import android.util.Log;
import android.view.ViewDebug;
import android.view.WindowManager;

public class LogTool {
    private static final String TAG = "drawer.server";

    public static void log(String msg) {
        Log.d(TAG, msg);
    }

    public static void w(String msg) {
        Log.w(TAG, msg);
    }

// 正数x的取反 等于 -(1+x)
//2019-06-28 19:38:15.562 6624-6624/? D/drawer.server: flag 1048576 is SHOW_WALLPAPER
//2019-06-28 19:38:15.566 6624-6624/? D/drawer.server: flag 512 is LAYOUT_NO_LIMITS
//2019-06-28 19:38:15.576 6624-6624/? D/drawer.server: flag 201326592 is TRANSLUCENT_STATUS TRANSLUCENT_NAVIGATION
//2019-06-28 19:38:15.579 6624-6624/? D/drawer.server: flag MIN is DRAWS_SYSTEM_BAR_BACKGROUNDS
    //1048576, 512,  -513,Integer.MIN, 201326592
    private static String flagsToString(Class<?> clazz, String field, int flags) {
        final ViewDebug.FlagToString[] mapping = getFlagMapping(clazz, field);
        if (mapping == null) {
            return Integer.toHexString(flags);
        }
        final StringBuilder result = new StringBuilder();
        final int count = mapping.length;
        for (int j = 0; j < count; j++) {
            final ViewDebug.FlagToString flagMapping = mapping[j];
            final boolean ifTrue = flagMapping.outputIf();
            final int maskResult = flags & flagMapping.mask();
            final boolean test = maskResult == flagMapping.equals();
            if (test && ifTrue) {
                final String name = flagMapping.name();
                result.append(name).append(' ');
            }
        }
        if (result.length() > 0) {
            result.deleteCharAt(result.length() - 1);
        }
        return result.toString();
    }

    private static ViewDebug.FlagToString[] getFlagMapping(Class<?> clazz, String field) {
        try {
            return clazz.getDeclaredField(field).getAnnotation(ViewDebug.ExportedProperty.class)
                    .flagMapping();
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}
