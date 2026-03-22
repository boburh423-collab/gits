package uz.sozboyligi.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.*;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public class MainActivity extends Activity {

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout offlineLayout;
    private RelativeLayout loadingLayout;
    private static final String URL = "https://soz-boyligi.zya.me/student.html";
    private ValueCallback<Uri[]> fileUploadCallback;
    private static final int FILE_CHOOSER = 1;
    private boolean isLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#000d07"));
            getWindow().setNavigationBarColor(Color.parseColor("#000d07"));
        }

        setContentView(R.layout.activity_main);

        webView      = findViewById(R.id.webView);
        progressBar  = findViewById(R.id.progressBar);
        offlineLayout = findViewById(R.id.offlineLayout);
        loadingLayout = findViewById(R.id.loadingLayout);

        Button retryBtn = findViewById(R.id.retryBtn);
        retryBtn.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                offlineLayout.setVisibility(View.GONE);
                loadingLayout.setVisibility(View.VISIBLE);
                webView.reload();
            }
        });

        setupWebView();

        if (isNetworkAvailable()) {
            webView.loadUrl(URL);
        } else {
            showOffline();
        }
    }

    private void setupWebView() {
        webView.setBackgroundColor(Color.parseColor("#000d07"));

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(false);
        s.setTextZoom(100);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAppCacheEnabled(true);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                loadingLayout.setVisibility(View.GONE);
                offlineLayout.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                isLoaded = true;

                // Inject native app CSS improvements
                String css = "javascript:(function(){"
                    + "var s=document.createElement('style');"
                    + "s.innerHTML='body{-webkit-tap-highlight-color:transparent;}';"
                    + "s.innerHTML+='input,textarea,select{font-size:16px!important;}';"
                    + "s.innerHTML+='*{-webkit-touch-callout:none;}';"
                    + "document.head.appendChild(s);"
                    + "})()";
                view.loadUrl(css);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String url) {
                if (!isLoaded) {
                    showOffline();
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("mailto:") || url.startsWith("tel:") || url.startsWith("tg:")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                if (!url.contains("soz-boyligi.zya.me") && url.startsWith("http")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progressBar != null) {
                    progressBar.setProgress(progress);
                    if (progress >= 100) {
                        new Handler().postDelayed(() ->
                            progressBar.setVisibility(View.GONE), 300);
                    }
                }
            }

            @Override
            public boolean onShowFileChooser(WebView webView,
                    ValueCallback<Uri[]> callback, FileChooserParams params) {
                fileUploadCallback = callback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(
                    Intent.createChooser(intent, "Fayl tanlang"), FILE_CHOOSER);
                return true;
            }
        });
    }

    private void showOffline() {
        loadingLayout.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        offlineLayout.setVisibility(View.VISIBLE);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
            getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == FILE_CHOOSER && fileUploadCallback != null) {
            Uri[] results = null;
            if (res == RESULT_OK && data != null && data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
            fileUploadCallback.onReceiveValue(results);
            fileUploadCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        if (!isLoaded && isNetworkAvailable()) {
            webView.reload();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
