package com.example.android.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        Intent currentIntent = getIntent();

        String clickedReportURL = currentIntent.getStringExtra("clickedReportUrl");

        //sometimes
       /* if(clickedReportURL.startsWith("http://")){
            clickedReportURL = clickedReportURL.replace("http:", "https:");
            Log.i("Works!", clickedReportURL);
        }*/



        WebView webView = (WebView) findViewById(R.id.webView);


        webView.getSettings().setJavaScriptEnabled(true);

        //this new web view client make us open links inside our app in the webView,
        //if we don't put it, the link wil be opened by the default browser in the phone
        webView.setWebViewClient(new WebViewClient());


        webView.loadUrl(clickedReportURL);
    }
}
