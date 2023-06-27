package com.example.songsong;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        TextView correctCountTextView = findViewById(R.id.correctCountTextView);
        TextView wrongCountTextView = findViewById(R.id.wrongCountTextView);
        Button backButton = findViewById(R.id.backButton);

        int correctCount = getIntent().getIntExtra("correctCount", 0);
        int wrongCount = getIntent().getIntExtra("wrongCount", 0);

        correctCountTextView.setText("맞힌 문제: " + correctCount);
        wrongCountTextView.setText("틀린 문제: " + wrongCount);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ResultActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
