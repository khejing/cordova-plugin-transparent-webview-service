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

public class TransparentWebViewService extends BackgroundService {
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
        wv.loadUrl("file:///android_asset/www/background.html");

        windowManager.addView(wv, params);
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