package com.sh7411usa.jrelay;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class LicenseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);

        WebView webView = findViewById(R.id.webview_license);
        webView.loadUrl("file:///android_asset/license.html");
    }
}
