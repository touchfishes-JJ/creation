package com.forcefocus.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;

/**
 * This Activity is intentionally only a WebView host. All visible UI comes
 * from the byte-identical ForceFocus_v16.html asset.
 */
public final class MainActivity extends Activity {
    private static final String UI_URL = "file:///android_asset/ForceFocus_v16.html";
    private static WeakReference<MainActivity> currentActivity = new WeakReference<>(null);

    private WebView webView;
    private String adapterScript;
    private boolean pageLoaded;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentActivity = new WeakReference<>(this);
        configureSystemBars();
        NotificationHelper.ensureChannel(this);
        NotificationHelper.requestPermissionIfNeeded(this);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(255, 253, 228));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }

        adapterScript = readUtf8Asset("native-bridge.js");
        webView.addJavascriptInterface(new NativeBridge(this), "NativeBridge");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !isTrustedAsset(request.getUrl());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !UI_URL.equals(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (UI_URL.equals(url)) {
                    pageLoaded = true;
                    injectAdapter();
                }
            }
        });

        setContentView(webView);
        webView.loadUrl(UI_URL);
        WeekendScheduler.scheduleAll(this);
        WeekendScheduler.restoreCurrentWeekendSlot(this);
    }

    private void configureSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
        }
        getWindow().setStatusBarColor(Color.rgb(255, 253, 228));
        getWindow().setNavigationBarColor(Color.rgb(255, 253, 228));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private boolean isTrustedAsset(Uri uri) {
        return uri != null && "file".equals(uri.getScheme())
                && "/android_asset/ForceFocus_v16.html".equals(uri.getPath());
    }

    private String readUtf8Asset(String name) {
        try (InputStream input = getAssets().open(name);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException("Missing required asset: " + name, error);
        }
    }

    private void injectAdapter() {
        if (webView != null && pageLoaded) {
            webView.evaluateJavascript(adapterScript, null);
        }
    }

    public void refreshWebState() {
        if (webView != null && pageLoaded) {
            webView.post(() -> webView.evaluateJavascript(
                    "window.ForceFocusNative&&window.ForceFocusNative.refresh()", null));
        }
    }

    public static void requestUiRefresh() {
        MainActivity activity = currentActivity.get();
        if (activity != null) {
            activity.runOnUiThread(activity::refreshWebState);
        }
    }

    public static void bringToFront(Context context) {
        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            context.startActivity(intent);
        } catch (RuntimeException ignored) {
            // The high-priority notification remains available when Android
            // blocks a background activity launch.
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentActivity = new WeakReference<>(this);
        WeekendScheduler.scheduleAll(this);
        WeekendScheduler.restoreCurrentWeekendSlot(this);
        refreshWebState();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        refreshWebState();
    }

    @Override
    public void onBackPressed() {
        if (webView == null || !pageLoaded) {
            super.onBackPressed();
            return;
        }
        webView.evaluateJavascript(
                "Boolean(window.ForceFocusNative&&window.ForceFocusNative.handleBack())",
                handled -> {
                    if (!"true".equals(handled) && !FocusState.isActive(this)) {
                        finishAfterWebBack();
                    }
                });
    }

    private void finishAfterWebBack() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (currentActivity.get() == this) {
            currentActivity.clear();
        }
        if (webView != null) {
            webView.removeJavascriptInterface("NativeBridge");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
