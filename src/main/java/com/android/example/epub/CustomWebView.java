package com.android.example.epub;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomWebView extends WebView {

    private GestureDetector gestureDetector;
    private MyJavaScriptInterface myInterface;

    public CustomWebView(Context context) {
        super(context);
        myInterface = new MyJavaScriptInterface();
        this.addJavascriptInterface(this.myInterface, "INTERFACE");

    }
    public CustomWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        myInterface = new MyJavaScriptInterface();
        this.addJavascriptInterface(this.myInterface, "INTERFACE");

    }
    public CustomWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        myInterface = new MyJavaScriptInterface();
        this.addJavascriptInterface(this.myInterface, "INTERFACE");
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return gestureDetector.onTouchEvent(ev) || super.onTouchEvent(ev);
    }

    public void setGestureDetector(GestureDetector gestureDetector) {
        this.gestureDetector = gestureDetector;
    }

    @Override
    public void loadUrl(String url) {
        super.loadUrl(url);

    }


    public MyJavaScriptInterface getMyInterface() {
        return myInterface;
    }
}
