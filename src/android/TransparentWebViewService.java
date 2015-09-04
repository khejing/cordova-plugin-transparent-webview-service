package com.yang.eto1.CordovaPlugin.TransparentWebViewServicePlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.red_folder.phonegap.plugin.backgroundservice.BackgroundService;

import android.view.WindowManager;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.app.PendingIntent;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class TransparentWebViewService extends BackgroundService {
    private static final String TAG = "TransparentWebViewService";

	@Override
    public void onCreate(){
		super.onCreate();

		WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_PHONE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 0;
        params.width = 0;
        params.height = 0;

        WebView wv = new WebView(this);
        wv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        final WebSettings settings = wv.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)) {
            Log.e(TAG, "Disabled addJavascriptInterface() bridge since Android version is old.");
            return;
        }
        SystemExposedJsApi exposedJsApi = new SystemExposedJsApi();
        wv.addJavascriptInterface(exposedJsApi, "simpleCordova");

        wv.loadUrl("file:///android_asset/www/background.html");
        windowManager.addView(wv, params);
	}

    class SystemExposedJsApi {
        private String packageName;

        @JavascriptInterface
        public void onMessage(String topic, String message){
            if(TransparentWebViewService.this.hasListenerAdded()){
                //send message to CordovaActivity
            }else{
                //show notification
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                Intent notifyIntent = new Intent(findMainActivityComponentName(this));
                // Sets the Activity to start in a new task
                notifyIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                PendingIntent notifyIntent =
                        PendingIntent.getActivity(
                        this,
                        0,
                        notifyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
                builder.setContentIntent(notifyIntent);
                Class rClass = Class.forName(pacakageName+".R.drawable");
                Field field = rClass.getField("icon");
                int property = field.getInt(rClass);
                builder.setSmallIcon(property);
                builder.setContentTitle("TEST");
                builder.setContentText("test");
                NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(id, builder.build());
            }
        }

        private static ComponentName findMainActivityComponentName(Context context) {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = null;
            try {
                packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException("No package info for " + context.getPackageName(), e);
            }

            for (ActivityInfo activityInfo : packageInfo.activities) {
                if ((activityInfo.flags & ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS) == 0) {
                    packageName = packageInfo.packageName;
                    return new ComponentName(packageInfo.packageName, activityInfo.name);
                }
            }
            throw new RuntimeException("Could not find main activity");
        }
    }

    @Override
    protected JSONObject doWork() {
       return null;
    }

    @Override
    protected JSONObject getConfig() {
       return null;
    }

    @Override
    protected void setConfig(JSONObject config) {
       
    }     

    @Override
    protected JSONObject initialiseLatestResult() {
       return null;
    }
}