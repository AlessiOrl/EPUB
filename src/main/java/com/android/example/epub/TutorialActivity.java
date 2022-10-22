package com.android.example.epub;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ImageView;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;

public class TutorialActivity extends AppCompatActivity {

    Context context;
    SharedPreferences sharedPreferences;

    AlertDialog.Builder builder;

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
        builder = new AlertDialog.Builder(this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);


        if (!sharedPreferences.getBoolean("showed_tutorial", false)) {
            sharedPreferences.edit().putBoolean("showed_tutorial", true).commit();

            //Setting message manually and performing action on button click
            builder.setMessage("Hi, before start using our app we invite you to take a look at the tutorial. It could seems useless but it can  greatly increase the experience.")
                    .setCancelable(false)
                    .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            AlertDialog alert = builder.create();
            alert.setTitle("Welcome");
            alert.show();
        }


    }
    //On Navigation Up
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
