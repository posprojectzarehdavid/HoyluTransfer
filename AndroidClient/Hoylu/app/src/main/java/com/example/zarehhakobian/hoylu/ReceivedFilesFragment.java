package com.example.zarehhakobian.hoylu;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Zareh Hakobian on 17.08.2017.
 */

public class ReceivedFilesFragment extends Fragment {

    FileAdapter fileAdapter;
    ListView listView;
    public ReceivedFilesFragment() {
    }

    @Override
    public void onResume() {
        if(listView != null){
            showFiles(listView);
            fileAdapter.notifyDataSetChanged();
        }
        super.onResume();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.received_files_view, container, false);
        listView = (ListView) v.findViewById(R.id.listview);
        showFiles(listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File selectedFile = (File) fileAdapter.getItem(position);
                try {
                    FileOpen.openFile(getActivity(),selectedFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        return v;
    }

    public void showFiles(ListView listView){
        File files = new File(Environment.getExternalStorageDirectory() + File.separator + "Hoylu");
        File[] filesToShow = files.listFiles();
        fileAdapter = new FileAdapter(getActivity());
        if(filesToShow != null){
            ArrayList<File> list = new ArrayList<>();
            for (File f: filesToShow) {
                list.add(f);
            }
            Collections.sort(list, new Comparator<File>(){

                public int compare(File f1, File f2)
                {
                    return Long.compare(f2.lastModified(), f1.lastModified());
                }
            });
            for (File f: list) {
                fileAdapter.add(f);
            }
            listView.setAdapter(fileAdapter);
        }
        listView.setAdapter(fileAdapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

}
