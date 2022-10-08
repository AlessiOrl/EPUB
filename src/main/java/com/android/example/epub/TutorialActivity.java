package com.android.example.epub;

import android.content.Context;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;

public class TutorialActivity extends AppCompatActivity {

    // ArrayList for person names
    ArrayList<String> gestureTitles = new ArrayList<>(Arrays.asList("Fist", "Open Hand", "Point Left", "Point Right"));
    ArrayList<String> gestureUsage = new ArrayList<>(Arrays.asList("Pause, Cancel skip", "Start, Play, Number 5", "Back X Chapters", "Forward X Chapters"));

    ArrayList<Integer> gestureImages = new ArrayList<>(Arrays.asList(R.drawable.fist, R.drawable.open_hand, R.drawable.point_left, R.drawable.point_right));
    Context context;
    TutorialAdapter customAdapter;
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        context = getApplicationContext();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView = (RecyclerView) findViewById(R.id.tutorial_recyclerView);
        recyclerView.setLayoutManager(linearLayoutManager);
        customAdapter = new TutorialAdapter(context, gestureTitles, gestureUsage, gestureImages);
        recyclerView.setAdapter(customAdapter);

    }
}
