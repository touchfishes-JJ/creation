package com.forcefocus.app;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannel();
        requestNotificationPermissionIfNeeded();

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(255,253,228));
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);

        webView.addJavascriptInterface(new NativeBridge(this), "NativeBridge");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        setContentView(webView);
        try {
            StringBuilder html = new StringBuilder();
            for (int i = 1; i <= 4; i++) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        getAssets().open("ui_part" + i + ".txt"), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) html.append(line).append('\n');
                }
            }
            webView.loadDataWithBaseURL("file:///android_asset/", html.toString(), "text/html", "UTF-8", null);
        } catch (Exception e) {
            webView.loadData("<h3>ForceFocus UI load failed</h3>", "text/html", "UTF-8");
        }

        WeekendScheduler.scheduleAll(this);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            NotificationChannel ch = new NotificationChannel(
                    FocusAlarmReceiver.CHANNEL_ID,
                    "ForceFocus 专注提醒",
                    NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("周末时段提前 5 分钟提醒与专注结束提醒");
            nm.createNotificationChannel(ch);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
