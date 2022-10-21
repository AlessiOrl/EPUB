package com.android.example.epub;

import android.content.Context;
import android.os.Bundle;
import android.widget.ImageView;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;

public class TutorialActivity extends AppCompatActivity {

    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        getSupportActionBar().setTitle("Tutorial");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        context = getApplicationContext();

        ImageView mImageView;
        mImageView = (ImageView) findViewById(R.id.tutorial_imageview);
        mImageView.setImageResource(R.drawable.tutorial_image);
    }
    //On Navigation Up
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
