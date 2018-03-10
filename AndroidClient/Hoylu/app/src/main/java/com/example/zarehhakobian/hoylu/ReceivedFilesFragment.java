package com.example.zarehhakobian.hoylu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
    File[] filesToShow;
    ListView listView;

    public ReceivedFilesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        int id = v.getId();
        if(id==R.id.listview){
            getActivity().getMenuInflater().inflate(R.menu.menu, menu);
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        final File f = (File) fileAdapter.getItem(info.position);
        int id = item.getItemId();

        if(id == R.id.action_delete) {
            AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
            ab.setMessage(getResources().getString(R.string.delete));
            ab.setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    f.delete();
                    showFiles(listView);
                }
            });
            ab.setNegativeButton(getResources().getString(R.string.no), null);
            ab.create().show();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.received_files_view, container, false);
        listView = (ListView) v.findViewById(R.id.listview);
        registerForContextMenu(listView);
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

    private void showFiles(ListView listView) {
        File files = new File(Environment.getExternalStorageDirectory() + File.separator + "Hoylu");
        filesToShow = files.listFiles();
        fileAdapter = new FileAdapter(getActivity());
        if(filesToShow != null) {
            ArrayList<File> list = new ArrayList<>();
            for (File f : filesToShow) {
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
        }
        listView.setAdapter(fileAdapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
