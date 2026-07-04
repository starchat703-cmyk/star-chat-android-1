package com.starchat.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class MainActivity extends Activity {
    WebView webView;
    FrameLayout root;
    AdView bannerAd;
    InterstitialAd interstitialAd;
    RewardedAd rewardedAd;
    ValueCallback<Uri[]> filePathCallback;

    final String BANNER_ID = "ca-app-pub-2795375653009372/9601507474";
    final String INTERSTITIAL_ID = "ca-app-pub-2795375653009372/6629492681";
    final String REWARDED_ID = "ca-app-pub-2795375653009372/9064084337";
    final int FILE_CHOOSER_REQUEST = 2025;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }

        MobileAds.initialize(this, status -> {});
        root = new FrameLayout(this);
        setContentView(root);

        openWebView();
        loadBanner();
        loadInterstitial();
        loadRewarded();
    }

    void openWebView() {
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                MainActivity.this.filePathCallback = filePathCallback;
                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    MainActivity.this.filePathCallback = null;
                    return false;
                }
                return true;
            }
        });
        webView.addJavascriptInterface(new AdsBridge(), "AndroidAds");
        webView.loadUrl("file:///android_asset/index.html");
    }

    void loadBanner() {
        bannerAd = new AdView(this);
        bannerAd.setAdUnitId(BANNER_ID);
        bannerAd.setAdSize(AdSize.BANNER);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;

        root.addView(bannerAd, params);
        bannerAd.loadAd(new AdRequest.Builder().build());
    }

    void loadInterstitial() {
        InterstitialAd.load(this, INTERSTITIAL_ID, new AdRequest.Builder().build(),
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(InterstitialAd ad) { interstitialAd = ad; }
                @Override
                public void onAdFailedToLoad(LoadAdError error) { interstitialAd = null; }
            });
    }

    void showInterstitial() {
        if (interstitialAd != null) {
            interstitialAd.show(this);
            interstitialAd = null;
            loadInterstitial();
        }
    }

    void loadRewarded() {
        RewardedAd.load(this, REWARDED_ID, new AdRequest.Builder().build(),
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(RewardedAd ad) { rewardedAd = ad; }
                @Override
                public void onAdFailedToLoad(LoadAdError error) { rewardedAd = null; }
            });
    }

    void showRewarded() {
        if (rewardedAd != null) {
            rewardedAd.show(this, rewardItem -> {
                webView.post(() -> webView.evaluateJavascript(
                    "window.dispatchEvent(new CustomEvent('adRewardEarned',{detail:{coins:10}}));",
                    null
                ));
            });
            rewardedAd = null;
            loadRewarded();
        } else {
            webView.post(() -> webView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('adNotReady'));",
                null
            ));
            loadRewarded();
        }
    }

    public class AdsBridge {
        @JavascriptInterface
        public void showInterstitialAd() { runOnUiThread(() -> showInterstitial()); }
        @JavascriptInterface
        public void showRewardedAd() { runOnUiThread(() -> showRewarded()); }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
public void onBackPressed() {
    if (webView != null && webView.canGoBack()) {
        webView.goBack();
    } else {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Exit Star Chat?")
            .setMessage("Kya aap app band karna chahte hain?")
            .setPositiveButton("Yes", (dialog, which) -> finish())
            .setNegativeButton("No", null)
            .show();
    }
}
}
