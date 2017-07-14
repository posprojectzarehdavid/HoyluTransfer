package com.example.zarehhakobian.qr_code_scanner;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.vision.barcode.Barcode;

/**
 * Created by Zareh Hakobian on 13.07.2017.
 */

public class InfoFragment extends Fragment {
    TextView content;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.barcode_info,container,false);
        content = (TextView) v.findViewById(R.id.barcode_value);
        return v;
    }

    public void showQRCodeContent(Barcode code){
        content.setText(code.displayValue);
    }
}
