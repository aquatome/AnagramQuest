package com.example.anagramquestapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import java.io.IOException;

public class WinActivity extends AppCompatActivity {

    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_win);

        mediaPlayer = MediaPlayer.create(WinActivity.this, R.raw.winsound);

        mediaPlayer.start();

        Button retryButton = findViewById(R.id.retryButton);
        retryButton.setOnClickListener(view -> {

            Intent myIntent = new Intent(WinActivity.this, MainActivity.class);
            WinActivity.this.startActivity(myIntent);

        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.release();
            }

        });

    }


}