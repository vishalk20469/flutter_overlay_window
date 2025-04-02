package flutter.overlay.window.flutter_overlay_window;

import android.content.Context;
import android.util.Log;

public class NativeContext {
    private static Context appContext;

    public static void setApplicationContext(Context context) {
        Log.d("NativeContext","Set");
        appContext = context;
    }

    public static Context getApplicationContext() {
        return appContext;
    }
}