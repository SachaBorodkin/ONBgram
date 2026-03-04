package com.onbgram.pro;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.getcapacitor.BridgeActivity;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BridgeActivity {

    private PermissionRequest pendingPermissionRequest;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupWebView();
        requestAllPermissions();
    }

    private void setupWebView() {
        // Get the WebView from Capacitor bridge
        WebView webView = getBridge().getWebView();
        WebSettings settings = webView.getSettings();

        // Enable all features
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Allow camera and microphone
        webView.setWebChromeClient(new WebChromeClient() {

            // Handle camera/microphone/notifications permission requests from JS
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                pendingPermissionRequest = request;
                List<String> androidPerms = new ArrayList<>();

                for (String resource : request.getResources()) {
                    if (resource.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                        androidPerms.add(Manifest.permission.CAMERA);
                    }
                    if (resource.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                        androidPerms.add(Manifest.permission.RECORD_AUDIO);
                    }
                }

                if (!androidPerms.isEmpty()) {
                    // Check which permissions we still need
                    List<String> toRequest = new ArrayList<>();
                    for (String perm : androidPerms) {
                        if (ContextCompat.checkSelfPermission(MainActivity.this, perm)
                                != PackageManager.PERMISSION_GRANTED) {
                            toRequest.add(perm);
                        }
                    }
                    if (toRequest.isEmpty()) {
                        // Already granted — approve WebView request immediately
                        request.grant(request.getResources());
                        pendingPermissionRequest = null;
                    } else {
                        ActivityCompat.requestPermissions(
                            MainActivity.this,
                            toRequest.toArray(new String[0]),
                            PERMISSION_REQUEST_CODE
                        );
                    }
                } else {
                    request.grant(request.getResources());
                    pendingPermissionRequest = null;
                }
            }

            // Handle geolocation (just in case)
            @Override
            public void onGeolocationPermissionsShowPrompt(
                    String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Keep everything inside the WebView
                if (url.startsWith("https://sachaborodkin.github.io")) {
                    return false;
                }
                return false;
            }
        });
    }

    private void requestAllPermissions() {
        List<String> perms = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.RECORD_AUDIO);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!perms.isEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                perms.toArray(new String[0]),
                PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE && pendingPermissionRequest != null) {
            // Grant the WebView permission request after Android grants it
            pendingPermissionRequest.grant(pendingPermissionRequest.getResources());
            pendingPermissionRequest = null;
        }
    }
}