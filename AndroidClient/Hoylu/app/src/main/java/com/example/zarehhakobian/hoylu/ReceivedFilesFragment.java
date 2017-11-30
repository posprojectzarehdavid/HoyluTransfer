package com.example.zarehhakobian.hoylu;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.File;

/**
 * Created by Zareh Hakobian on 17.08.2017.
 */

public class ReceivedFilesFragment extends Fragment {

    FileAdapter fileAdapter;
    File[] filesToShow;

    public ReceivedFilesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.received_files_view, container, false);
        final ListView listView = (ListView) v.findViewById(R.id.listview);
        File files = new File(Environment.getExternalStorageDirectory() + File.separator + "Hoylu");
        filesToShow = files.listFiles();
        fileAdapter = new FileAdapter(getActivity());
        if(filesToShow != null){
            for (int i = 0; i<filesToShow.length; i++){
                fileAdapter.add(filesToShow[i]);
            }
        }
        listView.setAdapter(fileAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File selectedFile = (File) fileAdapter.getItem(position);
                //showSelectedImage(imagePath);
            }
        });
        return v;
    }

    private void showSelectedImage(final String imagePath) {
        AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
        final LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.selected_image_layout, null);
        ImageView iv = (ImageView) v.findViewById(R.id.selectedImage);
        iv.setImageBitmap(BitmapFactory.decodeFile(imagePath));
        ab.setView(v);
        ab.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        ab.setPositiveButton(R.string.share, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ShareActivity.fileToSend = new File(imagePath);
                Intent i = new Intent(getActivity(), ShareActivity.class);
                startActivity(i);
            }
        });
        ab.create().show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
