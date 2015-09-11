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
import android.content.pm.PackageManager;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.media.RingtoneManager;
import android.util.Log;

public class TransparentWebViewService extends BackgroundService {
    private static final String TAG = "TransparentWebViewService";
    private WebView wv;
    private boolean isActivityBound = false;
    private JSONObject currentMsg;
    private static int messageId = 0;

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

        wv = new WebView(this);
        wv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        final WebSettings settings = wv.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        ApplicationInfo appInfo = this.getApplicationContext().getApplicationInfo();
        if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0 &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            enableRemoteDebugging();
        }
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)) {
            Log.e(TAG, "Disabled addJavascriptInterface() bridge since Android version is old.");
            return;
        }
        SystemExposedJsApi exposedJsApi = new SystemExposedJsApi();
        wv.addJavascriptInterface(exposedJsApi, "simpleCordova");

        wv.loadUrl("file:///android_asset/www/background.html");
        windowManager.addView(wv, params);
	}

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void enableRemoteDebugging() {
        try {
            wv.setWebContentsDebuggingEnabled(true);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "You have one job! To turn on Remote Web Debugging! YOU HAVE FAILED! ");
            e.printStackTrace();
        }
    }

    private void showNotification(String title, String text){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Intent notifyIntent = new Intent();
        ComponentName mainActivityComponent = findMainActivityComponentName(this);
        notifyIntent.setComponent(mainActivityComponent);
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
        builder.setAutoCancel(true);
        builder.setSmallIcon(this.getResources().getIdentifier("icon", "drawable", mainActivityComponent.getPackageName()));
        builder.setContentTitle(title);
        builder.setContentText(text);
        builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        builder.setVibrate(new long[]{0, 3000});
        NotificationManager mNotificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(messageId, builder.build());
        messageId++;
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

    @Override
    public IBinder onBind(Intent intent) {
        isActivityBound = true;
        return super.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent){
        isActivityBound = false;
        return super.onUnbind(intent);
    }

    class SystemExposedJsApi {
        @JavascriptInterface
        public void showNotification(String title, String text){
            Log.i(TAG, "no activity, just service, so show notification, sender is "+title+", text is "+text);
            TransparentWebViewService.this.showNotification(title, text);
        }

        @JavascriptInterface
        public boolean isActivityBound(){
            return TransparentWebViewService.this.isActivityBound;
        }

        @JavascriptInterface
        public void onMessage(JSONObject msg){
            TransparentWebViewService.this.currentMsg = msg;
            TransparentWebViewService.this.runOnce();
        }
    }

    @Override
    protected JSONObject doWork() {
       return currentMsg;
    }

    @Override
    protected JSONObject getConfig() {
       return null;
    }

    @Override
    protected void setConfig(JSONObject config) {
        String type;
        try{
            type = config.getString("type");
        }catch(JSONObject e){
            Log.e(TAG, "msg from main activity error, no type field");
            return;
        }
        if(type.equals("LoginInfo")){
            String username;
            String password;
            String role;
            try{
                username = config.getString("username");
                password = config.getString("password");
                role = config.getString("role");
            }catch(JSONObject e){
                Log.e(TAG, "LoginInfo msg from main activity error");
                return;
            }
            wv.loadUrl("javascript:Auth.login("+username+", "+password+", "+role+", loginCallback);localStorage.loginInfo = JSON.stringify({username: "+username+", password: "+password+", role: "+role+"});");
        }else if(type.equals("Subscribe")){
            String topic;
            try{
                topic = config.getString("topic");
            }catch(JSONObject e){
                Log.e(TAG, "Subscribe msg from main activity error");
                return;
            }
            wv.loadUrl("javascript:MqttClient.subscribe("+topic+");");
        }else if(type.equals("Publish")){
            String topic, msg;
            try{
                topic = config.getString("topic");
                msg = config.getString("message");
            }catch(JSONObject e){
                Log.e(TAG, "publish msg from main activity error");
                return;
            }
            wv.loadUrl("javascript:MqttClient.publish("+topic+", "+msg+");");
        }
    }

    @Override
    protected JSONObject initialiseLatestResult() {
       return null;
    }
}