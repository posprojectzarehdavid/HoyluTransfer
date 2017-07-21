package com.example.zarehhakobian.hoylushare.CameraClasses;

/**
 * Created by Zareh Hakobian on 13.07.2017.
 */

import android.app.Fragment;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

/**
 * Factory for creating a tracker and associated graphic to be associated with a new barcode.  The
 * multi-processor uses this factory to create barcode trackers as needed -- one for each barcode.
 */
public class BarcodeTrackerFactory implements MultiProcessor.Factory<Barcode> {
    private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;
    private Fragment fragment;

    public BarcodeTrackerFactory(GraphicOverlay<BarcodeGraphic> barcodeGraphicOverlay, Fragment fragment) {
        mGraphicOverlay = barcodeGraphicOverlay;
        this.fragment = fragment;
    }

    @Override
    public Tracker<Barcode> create(Barcode barcode) {
        BarcodeGraphic graphic = new BarcodeGraphic(mGraphicOverlay, fragment);
        return new BarcodeGraphicTracker(mGraphicOverlay, graphic);
    }
}
