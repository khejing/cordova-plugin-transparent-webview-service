package com.yang.eto1.CordovaPlugin.TransparentWebViewServicePlugin;

import java.lang.reflect.Field;

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
import android.util.Log;

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
        //SystemExposedJsApi exposedJsApi = new SystemExposedJsApi();
        //wv.addJavascriptInterface(exposedJsApi, "simpleCordova");

        wv.loadUrl("file:///android_asset/www/background.html");
        windowManager.addView(wv, params);
	}

    private void showNotification(){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Intent notifyIntent = new Intent();
        ComponentName mainActivity = findMainActivityComponentName(this);
        notifyIntent.setComponent(mainActivity);
        // Sets the Activity to start in a new task
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent notifyPendingIntent =
                PendingIntent.getActivity(
                this,
                0,
                notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        builder.setContentIntent(notifyPendingIntent);
        try{
            Class rClass = Class.forName(mainActivity.getPackageName()+".R.drawable");
            Field field = rClass.getField("icon");
            int property = field.getInt(rClass);
            builder.setSmallIcon(property);
            builder.setContentTitle("TEST");
            builder.setContentText("test");            
        }catch(ClassNotFoundException e){
            Log.e(TAG, "drawable class in R.java not found");
            return;
        }catch(NoSuchFieldException e){
            Log.e(TAG, "icon field not found");
            return;
        }catch(IllegalAccessException e){
            Log.e(TAG, "get icon field value error");
            return;            
        }
        NotificationManager mNotificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, builder.build());
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
                return new ComponentName(packageInfo.packageName, activityInfo.name);
            }
        }
        throw new RuntimeException("Could not find main activity");
    }

    class SystemExposedJsApi {
        @JavascriptInterface
        public void onMessage(String topic, String message){
            if(TransparentWebViewService.this.hasListenerAdded()){
                //send message to CordovaActivity
            }else{
                TransparentWebViewService.this.showNotification();
            }
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