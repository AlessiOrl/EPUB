package com.android.example.epub;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.webkit.JavascriptInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            List<String> list = new ArrayList<>();
            for (String s : this.contentView.split("[\\n.]")) {
                String replace = s.replace("\"", "\\\"");
                list.add(replace);
            }
            this.sentences = new ArrayList<>(list);
        }

    }

    public ArrayList<String> getContentSentences(){
        return this.sentences;
    }

}
