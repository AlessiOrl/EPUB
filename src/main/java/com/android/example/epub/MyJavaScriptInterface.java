package com.android.example.epub;

import android.webkit.JavascriptInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class MyJavaScriptInterface {
    private String contentView;
    private ArrayList<String> sentences;

    public MyJavaScriptInterface() {
        contentView = "";
        sentences = new ArrayList<>();
    }

    @JavascriptInterface
    public void setContent(String content) {
        this.contentView = content;
        setContentSentences();
    }


    public String getContent() {
        return this.contentView;
    }

    private void setContentSentences() {
        this.sentences = new ArrayList<>(Arrays.asList(this.contentView.split("\n")));
    }

    public ArrayList<String> getContentSentences(){
        return this.sentences;
    }

}
