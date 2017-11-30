package com.example.zarehhakobian.hoylushare;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class ShowStatistic extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_statistic);

        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();

        Statistic beforeUpload = (Statistic) bundle.getSerializable("beforeUpload");
        Statistic afterUpload = (Statistic) bundle.getSerializable("afterUpload");
        setValues(beforeUpload, afterUpload);
    }

    private void setValues(Statistic beforeUpload, Statistic afterUpload) {
        TextView uploadMethod = (TextView) findViewById(R.id.uploadMethod);

        TextView secBU = (TextView) findViewById(R.id.secondsTakenBU);
        TextView mlsecBU = (TextView) findViewById(R.id.milliSecondsTakenBU);

        TextView secAU = (TextView) findViewById(R.id.secondsTakenAU);
        TextView mlsecAU = (TextView) findViewById(R.id.milliSecondsTakenAU);
        TextView uploadSpeedAU = (TextView) findViewById(R.id.uploadSpeedAU);

        uploadMethod.setText(beforeUpload.methodUsed.toString());
        secBU.setText("Seconds before Upload: " + beforeUpload.secondsTaken);
        mlsecBU.setText("Milliseconds before Upload: " + beforeUpload.millisecondsTaken);

        secAU.setText("Seconds after Upload: " + afterUpload.secondsTaken);
        mlsecAU.setText("Milliseconds after Upload: " + afterUpload.millisecondsTaken);
        uploadSpeedAU.setText("Uploadspeed in KBPS: " + afterUpload.uploadSpeedinKBPS);
    }
}
