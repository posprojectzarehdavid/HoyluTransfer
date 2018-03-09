package com.example.zarehhakobian.hoylu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Zareh Hakobian on 17.08.2017.
 */

public class FileAdapter extends BaseAdapter {

    private Context mContext;
    ArrayList<File> itemList = new ArrayList<File>();

    public FileAdapter(Context c) {
        mContext = c;
    }

    void add(File file) {
        itemList.add(file);
    }

    @Override
    public int getCount() {
        return itemList.size();
    }

    @Override
    public Object getItem(int arg0) {
        return itemList.get(arg0);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.listview_item_view, parent, false);
        TextView name = (TextView) rowView.findViewById(R.id.textViewName);
        TextView size = (TextView) rowView.findViewById(R.id.textViewSize);
        TextView modified = (TextView) rowView.findViewById(R.id.lastModified);
        File file = itemList.get(position);

        name.setText(file.getName());
        size.setText(mContext.getResources().getString(R.string.size)+getStringSizeLengthFile(file.length()));
        modified.setText(mContext.getResources().getString(R.string.received)+getDate(file.lastModified()));
        Glide.with(mContext).load(itemList.get(position));
        return rowView;

    }

    public static String getDate(long datesize){
        Date date = new Date(datesize);
        SimpleDateFormat dateformat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        return " "+dateformat.format(date);
    }

    public static String getStringSizeLengthFile(long size) {

        DecimalFormat df = new DecimalFormat("0.00");
        float sizeKb = 1024.0f;
        float sizeMb = sizeKb * sizeKb;
        float sizeGb = sizeMb * sizeKb;
        float sizeTerra = sizeGb * sizeKb;

        if(size < sizeMb)
            return " "+df.format(size / sizeKb)+ " KB";
        else if(size < sizeGb)
            return " "+df.format(size / sizeMb) + " MB";
        else if(size < sizeTerra)
            return " "+df.format(size / sizeGb) + " GB";

        return "";
    }
}
