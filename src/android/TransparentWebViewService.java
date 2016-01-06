package com.yang.eto1.CordovaPlugin.TransparentWebViewServicePlugin;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Random;

import com.red_folder.phonegap.plugin.backgroundservice.BackgroundService;

import de.appplant.cordova.plugin.localnotification.ClickActivity;

import android.annotation.TargetApi;
import android.view.WindowManager;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.os.Build;
import android.os.IBinder;
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
import android.net.Uri;
import android.util.Log;

public class TransparentWebViewService extends BackgroundService {
    private static final String TAG = "TransparentWebViewService";
    private static WindowManager windowManager;
    private static WebView wv;
    private boolean isActivityBound = false;
    private JSONObject currentMsg;

	@Override
    public void onCreate(){
		super.onCreate();

		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

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
        //ApplicationInfo appInfo = this.getApplicationContext().getApplicationInfo();
        if (/*(appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0 &&*/
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
        Log.i(TAG, "service onCreate() finished, webview is started");
	}

    @Override
	public void onDestroy() {
        windowManager.removeView(wv);
        wv.destroy();
        super.onDestroy();
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

    private void showNotification(String title, String text, int contactId){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Intent notifyIntent = new Intent(this, ClickActivity.class);
        Uri notificationRing = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        //must contain soundUri, or will print stack
        notifyIntent.putExtra("NOTIFICATION_OPTIONS", "{\"soundUri\":\""+notificationRing+"\", \"data\":\"{\\\"contactId\\\":"+contactId+"}\"}");
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        int requestCode = new Random().nextInt();
        PendingIntent notifyPendingIntent =
                PendingIntent.getActivity(
                this,
                requestCode,
                notifyIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
        );
        builder.setContentIntent(notifyPendingIntent);
        builder.setAutoCancel(true);
        builder.setSmallIcon(this.getResources().getIdentifier("icon", "drawable", getPackageName()));
        builder.setContentTitle(title);
        builder.setContentText(text);
        builder.setSound(notificationRing);
        NotificationManager mNotificationManager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify((int)System.currentTimeMillis(), builder.build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        isActivityBound = true;
        Log.i(TAG, "main activity bound to service");
        return super.onBind(intent);
    }

    @Override
    public void onRebind (Intent intent){
        isActivityBound = true;
        Log.i(TAG, "main activity rebound to service");
    }

    @Override
    public boolean onUnbind(Intent intent){
        isActivityBound = false;
        Log.i(TAG, "main activity unbound to service");
        return true;
    }

    class SystemExposedJsApi {
        @JavascriptInterface
        public void showNotification(String title, String text, int contactId){
            Log.i(TAG, "no activity, just service, so show notification, sender is "+title+", text is "+text+", contactId is "+contactId);
            TransparentWebViewService.this.showNotification(title, text, contactId);
        }

        @JavascriptInterface
        public boolean isActivityBound(){
            return TransparentWebViewService.this.isActivityBound;
        }

        @JavascriptInterface
        public void onMessage(String json){
            try{
                TransparentWebViewService.this.currentMsg = new JSONObject(json);
            }catch(JSONException e){
                Log.e(TAG, "construct JSONObject from javascript msg error");
                return;
            }            
            TransparentWebViewService.this.runOnce();
            setLatestResult(null);
        }
    }

    private void webviewExecuteJSInMainThread(final String js){
        wv.post(new Runnable(){
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    wv.evaluateJavascript(js, null);
                } else {
                    wv.loadUrl("javascript:" + js);
                }
            }
        });
    }

    @Override
    protected JSONObject doWork() {
        Log.i(TAG, "doWork return current msg");
        return currentMsg;
    }

    @Override
    protected JSONObject getConfig() {
       return null;
    }

    @Override
    protected void setConfig(JSONObject config) {
        //TODO: rework below to a general version
        String type;
        try{
            type = config.getString("type");
            Log.i(TAG, "got msg from main activity, type is "+type);
        }catch(JSONException e){
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
                Log.i(TAG, "got login msg from main activity, username is "+username+", password is "+password+", role is "+role);
            }catch(JSONException e){
                Log.e(TAG, "LoginInfo msg from main activity error");
                return;
            }
            webviewExecuteJSInMainThread("myEvents.emit(\"LoginInfo\", \""+username+"\", \""+password+"\", \""+role+"\");");
        }else if(type.equals("Logout")){
            webviewExecuteJSInMainThread("myEvents.emit(\"Logout\");");
        }else if(type.equals("Subscribe")){
            String topic;
            try{
                topic = config.getString("topic");
                Log.i(TAG, "got subscribe msg from main activity, topic is "+topic);
            }catch(JSONException e){
                Log.e(TAG, "Subscribe msg from main activity error");
                return;
            }
            webviewExecuteJSInMainThread("myEvents.emit(\"Subscribe\", \""+topic+"\");");
        }else if(type.equals("Publish")){
            String topic;
            JSONObject msg;
            try{
                topic = config.getString("topic");
                msg = config.getJSONObject("message");
                Log.i(TAG, "got publish msg from main activity, topic is "+topic+", msg is "+msg.toString());
            }catch(JSONException e){
                Log.e(TAG, "publish msg from main activity error");
                return;
            }
            webviewExecuteJSInMainThread("myEvents.emit(\"Publish\", \""+topic+"\", "+msg.toString()+");");
        }else{
            Log.w(TAG, "got msg from main activity, but type is unknown");
        }
    }

    @Override
    protected JSONObject initialiseLatestResult() {
       return null;
    }
}