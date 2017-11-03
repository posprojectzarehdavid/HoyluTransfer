package com.example.zarehhakobian.hoylushare;

import java.io.Serializable;

/**
 * Created by David on 29.10.2017.
 */

public class Statistic implements Serializable {
    String methodUsed;
    double secondsTaken;
    double millisecondsTaken;
    double uploadSpeedinKBPS;

    public Statistic(String methodUsed, double secondsTaken, double millisecondsTaken, double uploadSpeedinKBPS) {
        this.methodUsed = methodUsed;
        this.secondsTaken = secondsTaken;
        this.millisecondsTaken = millisecondsTaken;
        this.uploadSpeedinKBPS = uploadSpeedinKBPS;
    }
}
