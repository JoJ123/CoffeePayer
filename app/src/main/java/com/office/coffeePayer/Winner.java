package com.office.coffeePayer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class Winner extends AppCompatActivity {
    TextView tv_winner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_winner);

        tv_winner = (findViewById(R.id.tv_winner));
        String name = getIntent().getStringExtra(MainActivity.WINNER);

        tv_winner.setText(name);
    }
}
