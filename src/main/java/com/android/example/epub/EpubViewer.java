package com.android.example.epub;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.Html;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;


public class EpubViewer extends AppCompatActivity {

    Context context;
    SharedPreferences sharedPreferences;
    CustomWebView webView;
    WebView readingwebView;

    String bookTitle;
    boolean searchViewLongClick = false;

    SeekBar seekBar;
    FloatingActionButton start;
    FloatingActionButton stop;
    FloatingActionButton play;
    FloatingActionButton pause;

    FloatingActionButton ff;
    FloatingActionButton rew;

    boolean seeking = false;

    RefreshEpub refreshEpub;

    String path;
    UnzipEpub unzipEpub;
    List<String> pagesRef = new ArrayList<>();
    List<String> pages = new ArrayList<>();
    int pageNumber = 0;

    DrawerLayout drawer;
    NavigationView navigationViewContent;
    int webViewScrollAmount = 0;
    TextToSpeech tts;
    ArrayList<String> readableStrings;
    int lastSentencereaded;

    @SuppressLint({"ClickableViewAccessibility", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_epub_viewer);
        context = getApplicationContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);


        //Toolbar
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        path = getIntent().getStringExtra("path");

        //TTS
        lastSentencereaded = 0;
        tts = new TextToSpeech(EpubViewer.this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.ENGLISH);

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.i("TextToSpeech", "Language Not Supported");
                }

                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {

                        Log.i("TextToSpeech", "On Start");

                        runOnUiThread(() -> {
                            highlight_sentence(Math.min(lastSentencereaded, readableStrings.size()));
                        });
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Log.i("TextToSpeech", "On Done " + lastSentencereaded);

                        runOnUiThread(() -> {
                            remove_highlight_sentence(lastSentencereaded);
                            lastSentencereaded++;
                            if (lastSentencereaded < readableStrings.size()) {
                                readcurrent();
                            } else {
                                readNextChapter();
                            }
                        });
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.i("TextToSpeech", "On Error");
                    }
                });
            } else {
                Log.i("TextToSpeech", "Initialization Failed");
            }
        });

        tts.speak("THIS IS A TEST", TextToSpeech.QUEUE_FLUSH, null, null);

        /* An instance of this class will be registered as a JavaScript interface */
        //WebView

        webView = findViewById(R.id.custom_WebView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setDefaultTextEncodingName("utf-8");
        webView.setGestureDetector(new GestureDetector(new CustomeGestureDetector()));
        webView.setOnTouchListener(new View.OnTouchListener() {
            private static final int MAX_CLICK_DURATION = 60;
            private long startClickTime;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                SyncWebViewScrollSeekBar();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        startClickTime = Calendar.getInstance().getTimeInMillis();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                        if (clickDuration < MAX_CLICK_DURATION) {
                            RelativeLayout relativeLayout = findViewById(R.id.seekLayout);
                            if (relativeLayout.getVisibility() == View.VISIBLE) {
                                relativeLayout.setVisibility(View.GONE);
                            } else if (relativeLayout.getVisibility() == View.GONE) {
                                relativeLayout.setVisibility(View.VISIBLE);
                            }

                            if (toolbar.getVisibility() == View.VISIBLE) {
                                toolbar.setVisibility(View.GONE);
                                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                            } else if (toolbar.getVisibility() == View.GONE) {
                                toolbar.setVisibility(View.VISIBLE);
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                            }
                        }
                    }
                }
                return false;
            }
        });
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!sharedPreferences.getBoolean("built_in_web_browser", false) == true) {
                    if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                        view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    return;
                }
                webView.loadUrl("javascript:(function() { " + "var text=''; setInterval(function(){ if (window.getSelection().toString() && text!==window.getSelection().toString()){ text=window.getSelection().toString(); console.log(text); }}, 20);" + "})()");

                webView.loadUrl("javascript:window.INTERFACE.setContent(document.getElementsByTagName('body')[0].innerText);");

                InjectCss(view, "::selection { background: #ffb7b7; }");
                InjectCss(view, "* { padding: 0px !important; letter-spacing: normal !important; max-width: none !important; }");
                InjectCss(view, "* { font-family: " + getFromPreferences("font-family") + " !important; }");
                InjectCss(view, "* { font-size: " + getFromPreferences("font-size") + " !important; }");
                InjectCss(view, "* { font-style: " + getFromPreferences("font-style") + " !important; }");
                InjectCss(view, "* { font-weight: " + getFromPreferences("font-weight") + " !important; }");
                InjectCss(view, "* { text-align: " + getFromPreferences("text-align") + " !important; }");
                InjectCss(view, "body { background: " + getFromPreferences("themeback") + " !important; }");
                InjectCss(view, "* { color: " + getFromPreferences("themefront") + " !important; }");
                InjectCss(view, "* { line-height: " + getFromPreferences("line-height") + " !important; }");
                InjectCss(view, "body { margin: " + getFromPreferences("margin") + " !important; }");
                InjectCss(view, ".highlight { background-color: rgb(255, 165, 0, " + (Objects.equals(getFromPreferences("highlight"), "on") ? "0.7" : "0") + ");  }");
                InjectCss(view, "img { display: block !important; width: 100% !important; height: auto !important; }");

                try {
                    url = URLDecoder.decode(url, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                for (int i = 0; i < pages.size(); i++) {
                    if (url.contains(pages.get(i))) {
                        pageNumber = pages.indexOf(pages.get(i));
                        if (pageNumber > -1) {
                            navigationViewContent.getMenu().getItem(pageNumber).setChecked(true);
                            TextView textViewPage = findViewById(R.id.textViewPage);

                            textViewPage.setText("Page: " + navigationViewContent.getCheckedItem().toString());
                            if (!seeking) {
                                if (url.contains("#")) {
                                    final String finalUrl = url;
                                    webView.postDelayed(() -> {
                                        String[] anchor = finalUrl.split("#");
                                        webView.loadUrl("javascript:document.getElementById(\"" + anchor[anchor.length - 1] + "\").scrollIntoView()");
                                        SyncWebViewScrollSeekBar();
                                    }, 500);
                                } else {
                                    webView.postDelayed(() -> {
                                        webView.scrollTo(0, webViewScrollAmount);
                                        SyncWebViewScrollSeekBar();
                                    }, 500);
                                }
                            }
                        }
                        break;
                    }
                }


            }
        });

        //Seekbar
        final TextView textViewPercent = findViewById(R.id.textViewPercent);
        seekBar = findViewById(R.id.seekBar);
        seekBar.setMax(100);
        seekBar.setPadding(100, 0, 100, 0);

        start = findViewById(R.id.FAB_start);
        stop = findViewById(R.id.FAB_stop);
        play = findViewById(R.id.FAB_play);
        pause = findViewById(R.id.FAB_pause);
        ff = findViewById(R.id.FAB_forward);
        rew = findViewById(R.id.FAB_rew);
        final FloatingActionButton[] mediabuttons = {ff, play, pause, stop, rew};


        start.setOnClickListener(view -> {

            start.hide();
            for (FloatingActionButton mediabutton : mediabuttons) {
                mediabutton.show();
            }

            startReading();
        });
        stop.setOnClickListener(view -> {
            for (FloatingActionButton mediabutton : mediabuttons) {
                mediabutton.hide();
            }
            start.show();
            stopReading();
        });
        play.setOnClickListener(view -> {
            pause.show();
            play.hide();
            resumeReading();
        });
        pause.setOnClickListener(view -> {
            play.show();
            pause.hide();
            pauseReading();
        });
        ff.setOnClickListener(view -> {
            gotoNext();
        });
        rew.setOnClickListener(view -> {
            gotoPrevious();
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                this.progress = progress;
                textViewPercent.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                seeking = true;

                float whichPage = pages.size() * (float) progress / seekBar.getMax();

                float webViewHeight = (webView.getContentHeight() * webView.getScale()) - webView.getHeight();
                float franction = whichPage - ((int) whichPage);
                final int whichScroll = (int) (webViewHeight * franction);

                if (pages.size() >= 0 && pages.size() > whichPage) {
                    webView.loadUrl("file://" + pages.get((int) whichPage));
                    webView.postDelayed(() -> {
                        webView.scrollTo(0, whichScroll);
                        seeking = false;
                    }, 300);
                }
            }
        });
        refreshEpub = MainActivity.getInstance().refreshEpub;

        //Unzip and Show Epub
        path = getIntent().getStringExtra("path");
        bookTitle = getIntent().getStringExtra("title");
        getSupportActionBar().setTitle(bookTitle);
        unzipEpub = new UnzipEpub(context, pagesRef, pages);
        unzipEpub.Unzip(path);
        if (pages.size() > 0) {
            if (sharedPreferences.getBoolean("where_i_left", false) == true) {
                if (getIntent().getStringExtra("currentPage") != null) {
                    pageNumber = Integer.parseInt(getIntent().getStringExtra("currentPage"));
                } else {
                    pageNumber = 0;
                }
                if (getIntent().getStringExtra("currentScroll") != null) {
                    webViewScrollAmount = Integer.parseInt(getIntent().getStringExtra("currentScroll"));
                }
            }
            webView.loadUrl("file://" + pages.get(pageNumber));
        } else {
            finish();
            Toast.makeText(context, "Unable to open", Toast.LENGTH_LONG).show();
        }


        //Navigation Drawer
        drawer = findViewById(R.id.drawer_layout);
        navigationViewContent = findViewById(R.id.nav_view_content);
        for (int i = 0; i < pages.size(); i++) {
            String[] firstSplittedLink = pages.get(i).split("/");
            String[] secondSplittedLink = firstSplittedLink[firstSplittedLink.length - 1].split("\\.");
            navigationViewContent.getMenu().add(secondSplittedLink[0]);
            navigationViewContent.getMenu().getItem(i).setCheckable(true);

            navigationViewContent.getMenu().getItem(i).setOnMenuItemClickListener(item -> {
                for (int i1 = 0; i1 < pages.size(); i1++) {
                    String[] firstSplittedLink1 = pages.get(i1).split("/");
                    String[] secondSplittedLink1 = firstSplittedLink1[firstSplittedLink1.length - 1].split("\\.");
                    if (secondSplittedLink1[0].equals(item.toString())) {
                        webView.loadUrl("file://" + pages.get(i1));
                        webViewScrollAmount = 0;
                        break;
                    }
                }
                drawer.closeDrawer(GravityCompat.START);
                return false;
            });
        }

        checkSharedPreferences();

                /*
        MEDIA SECTION
         */
        readingwebView = findViewById(R.id.read_WebView);
        readingwebView.getSettings().setJavaScriptEnabled(true);
        readingwebView.getSettings().setAllowFileAccess(true);
        readingwebView.getSettings().setDefaultTextEncodingName("utf-8");
        readingwebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                String subtitle = readableStrings.get(0);
                String title = readableStrings.get(1);
                String lyrics = "[\"" + TextUtils.join("\" , \"", readableStrings.subList(2, readableStrings.size())) + "\"]";
                int hindex = lastSentencereaded;

                String data = "{ author: '" + title + "' , song: '" + subtitle + "' , albumart: '" + "', lyrics: " + lyrics + ", highlight_index: " + hindex + " }";

                readingwebView.loadUrl("javascript:loadSong(" + data + ")");
            }
        });

    }

    //On Activity Stop
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (drawer.isDrawerOpen(GravityCompat.END)) {
            drawer.closeDrawer(GravityCompat.END);
        } else {
            stopReading();
            super.onBackPressed();
        }
    }
    @Override
    public void onStop() {
        finish();
        super.onStop();
    }

    @Override
    protected void onDestroy() {


        //Close the Text to Speech Library
        if(tts != null) {

            tts.stop();
            tts.shutdown();
            Log.d("TTS", "TTS Destroyed");
        }
        super.onDestroy();
    }

    private void startReading() {
        lastSentencereaded = 0;
        webView.postDelayed(() -> {
            readableStrings = webView.getMyInterface().getContentSentences();
            readableStrings.removeAll(Arrays.asList(null, ""));

            if (readableStrings.isEmpty()) readNextChapter();
            else readChapter();

        }, 500);


    }

    private void readChapter() {
        readingwebView.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);

        readingwebView.postDelayed(() -> {
            readingwebView.loadUrl("file:///android_asset/index.html");

            readcurrent();
        }, 500);

    }

    private void readcurrent() {
        tts.speak(readableStrings.get(lastSentencereaded), TextToSpeech.QUEUE_FLUSH, null, TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED);
    }

    public void pauseReading() {
        if (tts.isSpeaking()) {
            tts.stop();
        }
    }

    public void resumeReading() {
        if (!tts.isSpeaking()) {
            readcurrent();
        }
    }

    public void stopReading() {
        if (tts.isSpeaking()) {
            tts.stop();
            remove_highlight_sentence(lastSentencereaded);
            lastSentencereaded = 0;
        }
        webView.setVisibility(View.VISIBLE);
        readingwebView.setVisibility(View.GONE);

    }

    public void gotoNext() {
        if (tts.isSpeaking()) {
            tts.stop();
        }
        remove_highlight_sentence(lastSentencereaded);

        lastSentencereaded++;
        readcurrent();

    }

    public void gotoPrevious() {
        if (tts.isSpeaking()) {
            tts.stop();
        }
        remove_highlight_sentence(lastSentencereaded);

        lastSentencereaded--;
        readcurrent();
    }

    public void readNextChapter() {

        pageNumber++;
        webView.loadUrl("file://" + pages.get(pageNumber));
        seekBar.setProgress(seekBar.getMax() * pageNumber / pages.size());
        webViewScrollAmount = 0;

        startReading();

    }

    public void readPreviousChapter() {

        pageNumber--;
        webView.loadUrl("file://" + pages.get(pageNumber));
        seekBar.setProgress(seekBar.getMax() * pageNumber / pages.size());
        webViewScrollAmount = (int) (webView.getContentHeight() * webView.getScale()) - webView.getHeight();
        lastSentencereaded = 0;


    }


    public void highlight_sentence(int x) {

        readingwebView.loadUrl("javascript:highlight_sentence(" + x + ")");

    }

    public void remove_highlight_sentence(int x) {
        readingwebView.loadUrl("javascript:remove_highlight(" + x + ")");
    }

    //Check Shared Preferences
    public void checkSharedPreferences() {
        if (sharedPreferences.getBoolean("keep_screen_on", false) == true) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if (sharedPreferences.getBoolean("rotation_lock", false) == true) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkSharedPreferences();
    }

    //Custom Shared Preferences
    public static final String myPref = "preferenceName";

    public String getFromPreferences(String key) {
        SharedPreferences sharedPreferences = getSharedPreferences(myPref, 0);
        String str = sharedPreferences.getString(key, "null");
        return str;
    }

    public void setToPreferences(String key, String thePreference) {
        SharedPreferences.Editor editor = getSharedPreferences(myPref, 0).edit();
        editor.putString(key, thePreference);
        editor.commit();
    }

    //Menu Search
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.epub_viewer_menu, menu);
        whichFontFamily(menu);
        whichFontSize(menu);
        whichFontStyle(menu);
        whichFontWeight(menu);
        whichTextAlign(menu);
        whichLineHeight(menu);
        whichMargin(menu);
        whichTheme(menu);
        whichHighlight(menu);
        webView.postDelayed(() -> webView.reload(), 150);

        MenuItem menuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setMaxWidth(500);
        menu.findItem(R.id.find_next).setVisible(false);
        searchView.setOnSearchClickListener(v -> mainMenu.findItem(R.id.find_next).setVisible(true));
        searchView.setOnCloseListener(() -> {
            mainMenu.findItem(R.id.find_next).setVisible(false);
            return false;
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                webView.findAllAsync(newText);
                return false;
            }
        });
        searchView.findViewById(R.id.search_src_text).setOnLongClickListener(v -> {
            searchViewLongClick = true;
            return false;
        });

        return true;
    }

    //Menu Font Family, Font Size, Font Style, Font Weight, Text-Align, Line Height, Margin, Theme
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.find_next:
                webView.findNext(true);
                return true;

            case R.id.sans_serif:
                setToPreferences("font-family", "sans-serif");
                whichFontFamily(mainMenu);
                return true;
            case R.id.monospace:
                setToPreferences("font-family", "monospace");
                whichFontFamily(mainMenu);
                return true;
            case R.id.serif:
                setToPreferences("font-family", "serif");
                whichFontFamily(mainMenu);
                return true;
            case R.id.cursive:
                setToPreferences("font-family", "cursive");
                whichFontFamily(mainMenu);
                return true;
            case R.id.default_family:
                setToPreferences("font-family", "default");
                whichFontFamily(mainMenu);
                return true;

            case R.id.ninety:
                setToPreferences("font-size", "90%");
                whichFontSize(mainMenu);
                return true;
            case R.id.ninety_five:
                setToPreferences("font-size", "95%");
                whichFontSize(mainMenu);
                return true;
            case R.id.hundred:
                setToPreferences("font-size", "100%");
                whichFontSize(mainMenu);
                return true;
            case R.id.hundred_five:
                setToPreferences("font-size", "105%");
                whichFontSize(mainMenu);
                return true;
            case R.id.hundred_ten:
                setToPreferences("font-size", "110%");
                whichFontSize(mainMenu);
                return true;

            case R.id.normal_style:
                setToPreferences("font-style", "normal");
                whichFontStyle(mainMenu);
                return true;
            case R.id.italic:
                setToPreferences("font-style", "italic");
                whichFontStyle(mainMenu);
                return true;
            case R.id.default_style:
                setToPreferences("font-style", "default");
                whichFontStyle(mainMenu);
                return true;

            case R.id.normal_weight:
                setToPreferences("font-weight", "normal");
                whichFontWeight(mainMenu);
                return true;
            case R.id.bold:
                setToPreferences("font-weight", "bold");
                whichFontWeight(mainMenu);
                return true;
            case R.id.default_weight:
                setToPreferences("font-weight", "default");
                whichFontWeight(mainMenu);
                return true;

            case R.id.left:
                setToPreferences("text-align", "left");
                whichTextAlign(mainMenu);
                return true;
            case R.id.right:
                setToPreferences("text-align", "right");
                whichTextAlign(mainMenu);
                return true;
            case R.id.center:
                setToPreferences("text-align", "center");
                whichTextAlign(mainMenu);
                return true;
            case R.id.justify:
                setToPreferences("text-align", "justify");
                whichTextAlign(mainMenu);
                return true;
            case R.id.default_align:
                setToPreferences("text-align", "default");
                whichTextAlign(mainMenu);
                return true;

            case R.id.onetwo:
                setToPreferences("line-height", "1.2");
                whichLineHeight(mainMenu);
                return true;
            case R.id.onefour:
                setToPreferences("line-height", "1.4");
                whichLineHeight(mainMenu);
                return true;
            case R.id.onesix:
                setToPreferences("line-height", "1.6");
                whichLineHeight(mainMenu);
                return true;
            case R.id.oneeight:
                setToPreferences("line-height", "1.8");
                whichLineHeight(mainMenu);
                return true;
            case R.id.two:
                setToPreferences("line-height", "2");
                whichLineHeight(mainMenu);
                return true;

            case R.id.zeropercent:
                setToPreferences("margin", "0%");
                whichMargin(mainMenu);
                return true;
            case R.id.onepercent:
                setToPreferences("margin", "1%");
                whichMargin(mainMenu);
                return true;
            case R.id.twopercent:
                setToPreferences("margin", "2%");
                whichMargin(mainMenu);
                return true;
            case R.id.threepercent:
                setToPreferences("margin", "3%");
                whichMargin(mainMenu);
                return true;
            case R.id.fourpercent:
                setToPreferences("margin", "4%");
                whichMargin(mainMenu);
                return true;
            case R.id.fivepercent:
                setToPreferences("margin", "5%");
                whichMargin(mainMenu);
                return true;

            case R.id.ghostwhite:
                setToPreferences("themeback", "GhostWhite");
                setToPreferences("themefront", "DarkSlateGray");
                whichTheme(mainMenu);
                return true;
            case R.id.darkslategray:
                setToPreferences("themeback", "DarkSlateGray");
                setToPreferences("themefront", "GhostWhite");
                whichTheme(mainMenu);
                return true;
            case R.id.bisque:
                setToPreferences("themeback", "Bisque");
                setToPreferences("themefront", "DimGrey");
                whichTheme(mainMenu);
                return true;
            case R.id.highlighton:
                setToPreferences("highlight", "on");
                whichHighlight(mainMenu);
                return true;
            case R.id.highlightoff:
                setToPreferences("highlight", "off");
                whichHighlight(mainMenu);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    Menu mainMenu;

    public void whichFontFamily(Menu mainMenu) {
        this.mainMenu = mainMenu;
        if (getFromPreferences("font-family").equals("sans-serif")) {
            mainMenu.findItem(R.id.sans_serif).setTitle(Html.fromHtml("<font face='sans-serif' color='#008577'>Sans Serif</font>"));
            mainMenu.findItem(R.id.serif).setTitle(Html.fromHtml("<font face='serif' color='black'>Serif</font>"));
            mainMenu.findItem(R.id.monospace).setTitle(Html.fromHtml("<font face='monospace' color='black'>Monospace</font>"));
            mainMenu.findItem(R.id.cursive).setTitle(Html.fromHtml("<font face='cursive' color='black'>Cursive</font>"));
            mainMenu.findItem(R.id.default_family).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else if (getFromPreferences("font-family").equals("serif")) {
            mainMenu.findItem(R.id.sans_serif).setTitle(Html.fromHtml("<font face='sans-serif' color='black'>Sans Serif</font>"));
            mainMenu.findItem(R.id.serif).setTitle(Html.fromHtml("<font face='serif' color='#008577'>Serif</font>"));
            mainMenu.findItem(R.id.monospace).setTitle(Html.fromHtml("<font face='monospace' color='black'>Monospace</font>"));
            mainMenu.findItem(R.id.cursive).setTitle(Html.fromHtml("<font face='cursive' color='black'>Cursive</font>"));
            mainMenu.findItem(R.id.default_family).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else if (getFromPreferences("font-family").equals("monospace")) {
            mainMenu.findItem(R.id.sans_serif).setTitle(Html.fromHtml("<font face='sans-serif' color='black'>Sans Serif</font>"));
            mainMenu.findItem(R.id.serif).setTitle(Html.fromHtml("<font face='serif' color='black'>Serif</font>"));
            mainMenu.findItem(R.id.monospace).setTitle(Html.fromHtml("<font face='monospace' color='#008577'>Monospace</font>"));
            mainMenu.findItem(R.id.cursive).setTitle(Html.fromHtml("<font face='cursive' color='black'>Cursive</font>"));
            mainMenu.findItem(R.id.default_family).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else if (getFromPreferences("font-family").equals("cursive")) {
            mainMenu.findItem(R.id.sans_serif).setTitle(Html.fromHtml("<font face='sans-serif' color='black'>Sans Serif</font>"));
            mainMenu.findItem(R.id.serif).setTitle(Html.fromHtml("<font face='serif' color='black'>Serif</font>"));
            mainMenu.findItem(R.id.monospace).setTitle(Html.fromHtml("<font face='monospace' color='black'>Monospace</font>"));
            mainMenu.findItem(R.id.cursive).setTitle(Html.fromHtml("<font face='cursive' color='#008577'>Cursive</font>"));
            mainMenu.findItem(R.id.default_family).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else {
            setToPreferences("font-family", "default");
            mainMenu.findItem(R.id.sans_serif).setTitle(Html.fromHtml("<font face='sans-serif' face='sans-serif' color='black'>Sans Serif</font>"));
            mainMenu.findItem(R.id.serif).setTitle(Html.fromHtml("<font face='serif' color='black'>Serif</font>"));
            mainMenu.findItem(R.id.monospace).setTitle(Html.fromHtml("<font face='monospace' color='black'>Monospace</font>"));
            mainMenu.findItem(R.id.cursive).setTitle(Html.fromHtml("<font face='cursive' color='black'>Cursive</font>"));
            mainMenu.findItem(R.id.default_family).setTitle(Html.fromHtml("<font color='#008577'>Default</font>"));
        }
        webView.reload();
    }

    public void whichFontSize(Menu mainMenu) {
        this.mainMenu = mainMenu;
        if (getFromPreferences("font-size").equals("90%")) {
            mainMenu.findItem(R.id.ninety).setTitle(Html.fromHtml("<font color='#008577'>90%</font>"));
            mainMenu.findItem(R.id.ninety_five).setTitle(Html.fromHtml("<font color='black'>95%</font>"));
            mainMenu.findItem(R.id.hundred).setTitle(Html.fromHtml("<font color='black'>100%</font>"));
            mainMenu.findItem(R.id.hundred_five).setTitle(Html.fromHtml("<font color='black'>105%</font>"));
            mainMenu.findItem(R.id.hundred_ten).setTitle(Html.fromHtml("<font color='black'>110%</font>"));
        } else if (getFromPreferences("font-size").equals("95%")) {
            mainMenu.findItem(R.id.ninety).setTitle(Html.fromHtml("<font color='black'>90%</font>"));
            mainMenu.findItem(R.id.ninety_five).setTitle(Html.fromHtml("<font color='#008577'>95%</font>"));
            mainMenu.findItem(R.id.hundred).setTitle(Html.fromHtml("<font color='black'>100%</font>"));
            mainMenu.findItem(R.id.hundred_five).setTitle(Html.fromHtml("<font color='black'>105%</font>"));
            mainMenu.findItem(R.id.hundred_ten).setTitle(Html.fromHtml("<font color='black'>110%</font>"));
        } else if (getFromPreferences("font-size").equals("105%")) {
            mainMenu.findItem(R.id.ninety).setTitle(Html.fromHtml("<font color='black'>90%</font>"));
            mainMenu.findItem(R.id.ninety_five).setTitle(Html.fromHtml("<font color='black'>95%</font>"));
            mainMenu.findItem(R.id.hundred).setTitle(Html.fromHtml("<font color='black'>100%</font>"));
            mainMenu.findItem(R.id.hundred_five).setTitle(Html.fromHtml("<font color='#008577'>105%</font>"));
            mainMenu.findItem(R.id.hundred_ten).setTitle(Html.fromHtml("<font color='black'>110%</font>"));
        } else if (getFromPreferences("font-size").equals("110%")) {
            mainMenu.findItem(R.id.ninety).setTitle(Html.fromHtml("<font color='black'>90%</font>"));
            mainMenu.findItem(R.id.ninety_five).setTitle(Html.fromHtml("<font color='black'>95%</font>"));
            mainMenu.findItem(R.id.hundred).setTitle(Html.fromHtml("<font color='black'>100%</font>"));
            mainMenu.findItem(R.id.hundred_five).setTitle(Html.fromHtml("<font color='black'>105%</font>"));
            mainMenu.findItem(R.id.hundred_ten).setTitle(Html.fromHtml("<font color='#008577'>110%</font>"));
        } else {
            setToPreferences("font-size", "100%");
            mainMenu.findItem(R.id.ninety).setTitle(Html.fromHtml("<font color='black'>90%</font>"));
            mainMenu.findItem(R.id.ninety_five).setTitle(Html.fromHtml("<font color='black'>95%</font>"));
            mainMenu.findItem(R.id.hundred).setTitle(Html.fromHtml("<font color='#008577'>100%</font>"));
            mainMenu.findItem(R.id.hundred_five).setTitle(Html.fromHtml("<font color='black'>105%</font>"));
            mainMenu.findItem(R.id.hundred_ten).setTitle(Html.fromHtml("<font color='black'>110%</font>"));
        }
        webView.reload();
    }

    public void whichFontStyle(Menu mainMenu) {
        this.mainMenu = mainMenu;
        if (getFromPreferences("font-style").equals("normal")) {
            mainMenu.findItem(R.id.normal_style).setTitle(Html.fromHtml("<font color='#008577'>Normal</font>"));
            mainMenu.findItem(R.id.italic).setTitle(Html.fromHtml("<font color='black'>Italic</font>"));
            mainMenu.findItem(R.id.default_style).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else if (getFromPreferences("font-style").equals("italic")) {
            mainMenu.findItem(R.id.normal_style).setTitle(Html.fromHtml("<font color='black'>Normal</font>"));
            mainMenu.findItem(R.id.italic).setTitle(Html.fromHtml("<font color='#008577'>Italic</font>"));
            mainMenu.findItem(R.id.default_style).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else {
            setToPreferences("font-style", "default");
            mainMenu.findItem(R.id.normal_style).setTitle(Html.fromHtml("<font color='black'>Normal</font>"));
            mainMenu.findItem(R.id.italic).setTitle(Html.fromHtml("<font color='black'>Italic</font>"));
            mainMenu.findItem(R.id.default_style).setTitle(Html.fromHtml("<font color='#008577'>Default</font>"));
        }
        webView.reload();
    }

    public void whichHighlight(Menu mainMenu) {
        this.mainMenu = mainMenu;
        if (getFromPreferences("highlight").equals("off")) {
            mainMenu.findItem(R.id.highlighton).setTitle(Html.fromHtml("<font color='black'>On</font>"));
            mainMenu.findItem(R.id.highlightoff).setTitle(Html.fromHtml("<font color='#008577'>Off</font>"));
        } else {
            setToPreferences("highlight", "on");
            mainMenu.findItem(R.id.highlighton).setTitle(Html.fromHtml("<font color='#008577'>On</font>"));
            mainMenu.findItem(R.id.highlightoff).setTitle(Html.fromHtml("<font color='black'>Off</font>"));
        }
        webView.reload();
    }

    public void whichFontWeight(Menu mainMenu) {
        this.mainMenu = mainMenu;
        if (getFromPreferences("font-weight").equals("normal")) {
            mainMenu.findItem(R.id.normal_weight).setTitle(Html.fromHtml("<font color='#008577'>Normal</font>"));
            mainMenu.findItem(R.id.bold).setTitle(Html.fromHtml("<font color='black'>Bold</font>"));
            mainMenu.findItem(R.id.default_weight).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else if (getFromPreferences("font-weight").equals("bold")) {
            mainMenu.findItem(R.id.normal_weight).setTitle(Html.fromHtml("<font color='black'>Normal</font>"));
            mainMenu.findItem(R.id.bold).setTitle(Html.fromHtml("<font color='#008577'>Bold</font>"));
            mainMenu.findItem(R.id.default_weight).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else {
            setToPreferences("font-weight", "default");
            mainMenu.findItem(R.id.normal_weight).setTitle(Html.fromHtml("<font color='black'>Normal</font>"));
            mainMenu.findItem(R.id.bold).setTitle(Html.fromHtml("<font color='black'>Bold</font>"));
            mainMenu.findItem(R.id.default_weight).setTitle(Html.fromHtml("<font color='#008577'>Default</font>"));
        }
        webView.reload();
    }

    public void whichTextAlign(Menu mainMenu) {
        this.mainMenu = mainMenu;
        if (getFromPreferences("text-align").equals("left")) {
            mainMenu.findItem(R.id.left).setTitle(Html.fromHtml("<font color='#008577'>Left</font>"));
            mainMenu.findItem(R.id.right).setTitle(Html.fromHtml("<font color='black'>Right</font>"));
            mainMenu.findItem(R.id.center).setTitle(Html.fromHtml("<font color='black'>Center</font>"));
            mainMenu.findItem(R.id.justify).setTitle(Html.fromHtml("<font color='black'>Justify</font>"));
            mainMenu.findItem(R.id.default_align).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else if (getFromPreferences("text-align").equals("right")) {
            mainMenu.findItem(R.id.left).setTitle(Html.fromHtml("<font color='black'>Left</font>"));
            mainMenu.findItem(R.id.right).setTitle(Html.fromHtml("<font color='#008577'>Right</font>"));
            mainMenu.findItem(R.id.center).setTitle(Html.fromHtml("<font color='black'>Center</font>"));
            mainMenu.findItem(R.id.justify).setTitle(Html.fromHtml("<font color='black'>Justify</font>"));
            mainMenu.findItem(R.id.default_align).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else if (getFromPreferences("text-align").equals("center")) {
            mainMenu.findItem(R.id.left).setTitle(Html.fromHtml("<font color='black'>Left</font>"));
            mainMenu.findItem(R.id.right).setTitle(Html.fromHtml("<font color='black'>Right</font>"));
            mainMenu.findItem(R.id.center).setTitle(Html.fromHtml("<font color='#008577'>Center</font>"));
            mainMenu.findItem(R.id.justify).setTitle(Html.fromHtml("<font color='black'>Justify</font>"));
            mainMenu.findItem(R.id.default_align).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else if (getFromPreferences("text-align").equals("justify")) {
            mainMenu.findItem(R.id.left).setTitle(Html.fromHtml("<font color='black'>Left</font>"));
            mainMenu.findItem(R.id.right).setTitle(Html.fromHtml("<font color='black'>Right</font>"));
            mainMenu.findItem(R.id.center).setTitle(Html.fromHtml("<font color='black'>Center</font>"));
            mainMenu.findItem(R.id.justify).setTitle(Html.fromHtml("<font color='#008577'>Justify</font>"));
            mainMenu.findItem(R.id.default_align).setTitle(Html.fromHtml("<font color='black'>Default</font>"));
        } else {
            setToPreferences("text-align", "default");
            mainMenu.findItem(R.id.left).setTitle(Html.fromHtml("<font color='black'>Left</font>"));
            mainMenu.findItem(R.id.right).setTitle(Html.fromHtml("<font color='black'>Right</font>"));
            mainMenu.findItem(R.id.center).setTitle(Html.fromHtml("<font color='black'>Center</font>"));
            mainMenu.findItem(R.id.justify).setTitle(Html.fromHtml("<font color='black'>Justify</font>"));
            mainMenu.findItem(R.id.default_align).setTitle(Html.fromHtml("<font color='#008577'>Default</font>"));
        }
        webView.reload();
    }

    public void whichLineHeight(Menu mainMenu) {
        this.mainMenu = mainMenu;
        if (getFromPreferences("line-height").equals("1.2")) {
            mainMenu.findItem(R.id.onetwo).setTitle(Html.fromHtml("<font color='#008577'>1.2</font>"));
            mainMenu.findItem(R.id.onefour).setTitle(Html.fromHtml("<font color='black'>1.4</font>"));
            mainMenu.findItem(R.id.onesix).setTitle(Html.fromHtml("<font color='black'>1.6</font>"));
            mainMenu.findItem(R.id.oneeight).setTitle(Html.fromHtml("<font color='black'>1.8</font>"));
            mainMenu.findItem(R.id.two).setTitle(Html.fromHtml("<font color='black'>2</font>"));
        } else if (getFromPreferences("line-height").equals("1.4")) {
            mainMenu.findItem(R.id.onetwo).setTitle(Html.fromHtml("<font color='black'>1.2</font>"));
            mainMenu.findItem(R.id.onefour).setTitle(Html.fromHtml("<font color='#008577'>1.4</font>"));
            mainMenu.findItem(R.id.onesix).setTitle(Html.fromHtml("<font color='black'>1.6</font>"));
            mainMenu.findItem(R.id.oneeight).setTitle(Html.fromHtml("<font color='black'>1.8</font>"));
            mainMenu.findItem(R.id.two).setTitle(Html.fromHtml("<font color='black'>2</font>"));
        } else if (getFromPreferences("line-height").equals("1.8")) {
            mainMenu.findItem(R.id.onetwo).setTitle(Html.fromHtml("<font color='black'>1.2</font>"));
            mainMenu.findItem(R.id.onefour).setTitle(Html.fromHtml("<font color='black'>1.4</font>"));
            mainMenu.findItem(R.id.onesix).setTitle(Html.fromHtml("<font color='black'>1.6</font>"));
            mainMenu.findItem(R.id.oneeight).setTitle(Html.fromHtml("<font color='#008577'>1.8</font>"));
            mainMenu.findItem(R.id.two).setTitle(Html.fromHtml("<font color='black'>2</font>"));
        } else if (getFromPreferences("line-height").equals("2")) {
            mainMenu.findItem(R.id.onetwo).setTitle(Html.fromHtml("<font color='black'>1.2</font>"));
            mainMenu.findItem(R.id.onefour).setTitle(Html.fromHtml("<font color='black'>1.4</font>"));
            mainMenu.findItem(R.id.onesix).setTitle(Html.fromHtml("<font color='black'>1.6</font>"));
            mainMenu.findItem(R.id.oneeight).setTitle(Html.fromHtml("<font color='black'>1.8</font>"));
            mainMenu.findItem(R.id.two).setTitle(Html.fromHtml("<font color='#008577'>2</font>"));
        } else {
            setToPreferences("line-height", "1.6");
            mainMenu.findItem(R.id.onetwo).setTitle(Html.fromHtml("<font color='black'>1.2</font>"));
            mainMenu.findItem(R.id.onefour).setTitle(Html.fromHtml("<font color='black'>1.4</font>"));
            mainMenu.findItem(R.id.onesix).setTitle(Html.fromHtml("<font color='#008577'>1.6</font>"));
            mainMenu.findItem(R.id.oneeight).setTitle(Html.fromHtml("<font color='black'>1.8</font>"));
            mainMenu.findItem(R.id.two).setTitle(Html.fromHtml("<font color='black'>2</font>"));
        }
        webView.reload();
    }

    public void whichMargin(Menu mainMenu) {
        this.mainMenu = mainMenu;
        if (getFromPreferences("margin").equals("0%")) {
            mainMenu.findItem(R.id.zeropercent).setTitle(Html.fromHtml("<font color='#008577'>0%</font>"));
            mainMenu.findItem(R.id.onepercent).setTitle(Html.fromHtml("<font color='black'>1%</font>"));
            mainMenu.findItem(R.id.twopercent).setTitle(Html.fromHtml("<font color='black'>2%</font>"));
            mainMenu.findItem(R.id.threepercent).setTitle(Html.fromHtml("<font color='black'>3%</font>"));
            mainMenu.findItem(R.id.fourpercent).setTitle(Html.fromHtml("<font color='black'>4%</font>"));
            mainMenu.findItem(R.id.fivepercent).setTitle(Html.fromHtml("<font color='black'>5%</font>"));
        } else if (getFromPreferences("margin").equals("1%")) {
            mainMenu.findItem(R.id.zeropercent).setTitle(Html.fromHtml("<font color='black'>0%</font>"));
            mainMenu.findItem(R.id.onepercent).setTitle(Html.fromHtml("<font color='#008577'>1%</font>"));
            mainMenu.findItem(R.id.twopercent).setTitle(Html.fromHtml("<font color='black'>2%</font>"));
            mainMenu.findItem(R.id.threepercent).setTitle(Html.fromHtml("<font color='black'>3%</font>"));
            mainMenu.findItem(R.id.fourpercent).setTitle(Html.fromHtml("<font color='black'>4%</font>"));
            mainMenu.findItem(R.id.fivepercent).setTitle(Html.fromHtml("<font color='black'>5%</font>"));
        } else if (getFromPreferences("margin").equals("2%")) {
            mainMenu.findItem(R.id.zeropercent).setTitle(Html.fromHtml("<font color='black'>0%</font>"));
            mainMenu.findItem(R.id.onepercent).setTitle(Html.fromHtml("<font color='black'>1%</font>"));
            mainMenu.findItem(R.id.twopercent).setTitle(Html.fromHtml("<font color='#008577'>2%</font>"));
            mainMenu.findItem(R.id.threepercent).setTitle(Html.fromHtml("<font color='black'>3%</font>"));
            mainMenu.findItem(R.id.fourpercent).setTitle(Html.fromHtml("<font color='black'>4%</font>"));
            mainMenu.findItem(R.id.fivepercent).setTitle(Html.fromHtml("<font color='black'>5%</font>"));
        } else if (getFromPreferences("margin").equals("3%")) {
            mainMenu.findItem(R.id.zeropercent).setTitle(Html.fromHtml("<font color='black'>0%</font>"));
            mainMenu.findItem(R.id.onepercent).setTitle(Html.fromHtml("<font color='black'>1%</font>"));
            mainMenu.findItem(R.id.twopercent).setTitle(Html.fromHtml("<font color='black'>2%</font>"));
            mainMenu.findItem(R.id.threepercent).setTitle(Html.fromHtml("<font color='#008577'>3%</font>"));
            mainMenu.findItem(R.id.fourpercent).setTitle(Html.fromHtml("<font color='black'>4%</font>"));
            mainMenu.findItem(R.id.fivepercent).setTitle(Html.fromHtml("<font color='black'>5%</font>"));
        } else if (getFromPreferences("margin").equals("4%")) {
            mainMenu.findItem(R.id.zeropercent).setTitle(Html.fromHtml("<font color='black'>0%</font>"));
            mainMenu.findItem(R.id.onepercent).setTitle(Html.fromHtml("<font color='black'>1%</font>"));
            mainMenu.findItem(R.id.twopercent).setTitle(Html.fromHtml("<font color='black'>2%</font>"));
            mainMenu.findItem(R.id.threepercent).setTitle(Html.fromHtml("<font color='black'>3%</font>"));
            mainMenu.findItem(R.id.fourpercent).setTitle(Html.fromHtml("<font color='#008577'>4%</font>"));
            mainMenu.findItem(R.id.fivepercent).setTitle(Html.fromHtml("<font color='black'>5%</font>"));
        } else {
            setToPreferences("margin", "5%");
            mainMenu.findItem(R.id.zeropercent).setTitle(Html.fromHtml("<font color='black'>0%</font>"));
            mainMenu.findItem(R.id.onepercent).setTitle(Html.fromHtml("<font color='black'>1%</font>"));
            mainMenu.findItem(R.id.twopercent).setTitle(Html.fromHtml("<font color='black'>2%</font>"));
            mainMenu.findItem(R.id.threepercent).setTitle(Html.fromHtml("<font color='black'>3%</font>"));
            mainMenu.findItem(R.id.fourpercent).setTitle(Html.fromHtml("<font color='black'>4%</font>"));
            mainMenu.findItem(R.id.fivepercent).setTitle(Html.fromHtml("<font color='#008577'>5%</font>"));
        }
        webView.reload();
    }

    public void whichTheme(Menu mainMenu) {
        this.mainMenu = mainMenu;
        if (getFromPreferences("themeback").equals("GhostWhite")) {
            mainMenu.findItem(R.id.ghostwhite).setTitle(Html.fromHtml("<font color='#008577'>Ghost White</font>"));
            mainMenu.findItem(R.id.darkslategray).setTitle(Html.fromHtml("<font color='black'>Dark Slate Gray</font>"));
            mainMenu.findItem(R.id.bisque).setTitle(Html.fromHtml("<font color='black'>Bisque</font>"));
        } else if (getFromPreferences("themeback").equals("DarkSlateGray")) {
            mainMenu.findItem(R.id.ghostwhite).setTitle(Html.fromHtml("<font color='black'>Ghost White</font>"));
            mainMenu.findItem(R.id.darkslategray).setTitle(Html.fromHtml("<font color='#008577'>Dark Slate Gray</font>"));
            mainMenu.findItem(R.id.bisque).setTitle(Html.fromHtml("<font color='black'>Bisque</font>"));
        } else {
            setToPreferences("themeback", "Bisque");
            setToPreferences("themefront", "DimGrey");
            mainMenu.findItem(R.id.ghostwhite).setTitle(Html.fromHtml("<font color='black'>Ghost White</font>"));
            mainMenu.findItem(R.id.darkslategray).setTitle(Html.fromHtml("<font color='black'>Dark Slate Gray</font>"));
            mainMenu.findItem(R.id.bisque).setTitle(Html.fromHtml("<font color='#008577'>Bisque</font>"));
        }
        webView.reload();
    }

    //WebView Gesture
    private class CustomeGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;
            if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1) return false;
            else {
                try { // right to left swipe .. go to next page
                    if (e1.getX() - e2.getX() > 150 && Math.abs(velocityX) > 1000) {

                        return true;
                    } //left to right swipe .. go to prev page
                    else if (e2.getX() - e1.getX() > 150 && Math.abs(velocityX) > 1000) {

                        return true;
                    } //bottom to top, go to next document
                    else if (e1.getY() - e2.getY() > 150 && Math.abs(velocityY) > 1000 && webView.getScrollY() >= Math.round(webView.getContentHeight() * webView.getScale()) - webView.getHeight() - 10) {
                        if (pageNumber < pages.size() - 1) {
                            pageNumber++;
                            webView.loadUrl("file://" + pages.get(pageNumber));
                            seekBar.setProgress(seekBar.getMax() * pageNumber / pages.size());
                            webViewScrollAmount = 0;
                            lastSentencereaded = 0;
                        }
                        return true;
                    } //top to bottom, go to prev document
                    else if (e2.getY() - e1.getY() > 150 && Math.abs(velocityY) > 1000 && webView.getScrollY() <= 10) {
                        if (pageNumber > 0) {
                            pageNumber--;
                            webView.loadUrl("file://" + pages.get(pageNumber));
                            seekBar.setProgress(seekBar.getMax() * pageNumber / pages.size());
                            webViewScrollAmount = (int) (webView.getContentHeight() * webView.getScale()) - webView.getHeight();
                            lastSentencereaded = 0;
                        }
                        return true;
                    }
                } catch (Exception e) {
                }
                return false;
            }
        }
    }

    //Inject CSS to WebView
    private final static String CREATE_CUSTOM_SHEET =
            "if (typeof(document.head) != 'undefined' && typeof(customSheet) == 'undefined') {"
                    + "var customSheet = (function() {"
                    + "var style = document.createElement(\"style\");"
                    + "style.appendChild(document.createTextNode(\"\"));"
                    + "document.head.appendChild(style);"
                    + "return style.sheet;"
                    + "})();"
                    + "}";

    private void InjectCss(WebView webView, String... cssRules) {
        StringBuilder jsUrl = new StringBuilder("javascript:");
        jsUrl.append(CREATE_CUSTOM_SHEET).append("if (typeof(customSheet) != 'undefined') {");
        int cnt = 0;
        for (String cssRule : cssRules) {
            jsUrl.append("customSheet.insertRule('").append(cssRule).append("', ").append(cnt++).append(");");
        }
        jsUrl.append("}");
        webView.loadUrl(jsUrl.toString());
    }

    //Sync WebView Scroll and Seek Bar Progress
    public void SyncWebViewScrollSeekBar() {
        if (webView.getUrl() != null && (webView.getUrl().startsWith("http://") || webView.getUrl().startsWith("https://"))) {
            return;
        }

        int real = seekBar.getMax() * pageNumber / pages.size();

        float webViewHeight = (webView.getContentHeight() * webView.getScale()) - webView.getHeight();
        float partPerPage = seekBar.getMax() / pages.size();
        float fraction = ((float) webView.getScrollY()) / webViewHeight * partPerPage;

        seekBar.setProgress(real + ((int) fraction));
    }
}
