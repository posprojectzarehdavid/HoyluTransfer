package com.example.zarehhakobian.hoylushare;

/**
 * Created by David on 29.10.2017.
 */

public class Statistic {
    String methodUsed;
    long secondsTaken, millisecondsTaken;
    double uploadSpeedinKBPS;

    public Statistic(String methodUsed, long secondsTaken, long millisecondsTaken, double uploadSpeedinKBPS) {
        this.methodUsed = methodUsed;
        this.secondsTaken = secondsTaken;
        this.millisecondsTaken = millisecondsTaken;
        this.uploadSpeedinKBPS = uploadSpeedinKBPS;
    }
}
