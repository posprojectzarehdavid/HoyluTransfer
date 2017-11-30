package com.example.zarehhakobian.hoylu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;

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
        File file = itemList.get(position);

        name.setText(file.getName());
        size.setText("Size: "+file.length());
        Glide.with(mContext).load(itemList.get(position));
        return rowView;

    }
}
