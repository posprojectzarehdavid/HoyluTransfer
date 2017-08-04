package com.example.zarehhakobian.hoylushare;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.test.espresso.core.deps.guava.base.Splitter;
import android.support.test.espresso.core.deps.guava.collect.Iterables;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.analytics.Analytics;
import com.microsoft.azure.mobile.crashes.Crashes;
import com.oschrenk.io.Base64;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;
import net.hockeyapp.android.metrics.MetricsManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity implements DeviceSelectedListener {

    String imagePath;
    private static final String TAG = "Main";
    public static final String CONNECTION_STRING = "http://40.114.246.211:4200";
    private static final int RC_Handle_CAMERA_AND_INTERNET_PERM_AND_READ_PERM = 2;
    public static boolean permissionsGranted = false;
    Socket socket;
    public static long start;
    public static long end;
    UploadingAsyncTask uat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        start = System.currentTimeMillis();
        int rc = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA);
        int i = ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        int readPerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (rc == PackageManager.PERMISSION_GRANTED && i == PackageManager.PERMISSION_GRANTED &&
                readPerm == PackageManager.PERMISSION_GRANTED) {
            permissionsGranted = true;
            setContentView(R.layout.activity_main);
            Map<String, String> properties = new HashMap<>();
            properties.put("App", "app im oncreate");
            UpdateManager.register(this);
            CrashManager.register(this);
            MetricsManager.register(getApplication());
            MetricsManager.trackEvent("App started");

            MobileCenter.start(getApplication(), "bea474ff-9e7d-4701-8f21-5811cff16895", Analytics.class, Crashes.class);
            Analytics.setEnabled(true);

            Analytics.trackEvent("App started", properties);
            Log.i("App", "AppStarted");

        } else {
            requestPermissions();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        end = 0;
        start = 0;
    }

    private String getImageForServer() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                imagePath = getImagePathFromIntent(intent);
                File file = new File(imagePath);
                byte[]imageBytes = imageToByteArray(file);
                String imageDataString = encodeImage(imageBytes);
                Log.i("imageString", imageDataString);
                //writeToFile(imageInBytes);
                return imageDataString;
            }
        }
        return null;
    }

    public void writeToFile(byte[]x) {
        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File (root.getAbsolutePath() + "/Download");
        dir.mkdirs();
        File file = new File(dir, "imageInBytes.txt");

        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);
            for(int i = 0; i<x.length; i++){
                pw.println(x[i]);
                pw.flush();
            }

            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i(TAG, "******* File not found. Did you add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] imageToByteArray(File f){
        FileInputStream fis = null;
        byte[] bytesArray = new byte[(int) f.length()];
        try {
            fis = new FileInputStream(f);
            fis.read(bytesArray);
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytesArray;
    }

    public String encodeImage(byte[] imageByteArray) {
        return Base64.encodeBytes(imageByteArray);
    }

    public static byte[] decodeImage(String imageDataString) {
        return Base64.decode(imageDataString);
    }

    private String getImagePathFromIntent(Intent intent) {
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        String[] filePathColumn = {MediaStore.Images.Media.DATA};

        Cursor cursor = getContentResolver().query(
                imageUri, filePathColumn, null, null, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String filePath = cursor.getString(columnIndex);
        cursor.close();
        Toast.makeText(this, filePath, Toast.LENGTH_LONG).show();
        //return BitmapFactory.decodeFile(filePath);

        return filePath;
    }

    private void requestPermissions() {
        Log.w(TAG, "Camera and internet permission is not granted. Requesting permission");
        final String[] permissions = new String[]{android.Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.INTERNET};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.CAMERA)
                && !ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.INTERNET)
                && !ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, permissions, RC_Handle_CAMERA_AND_INTERNET_PERM_AND_READ_PERM);
            return;
        }
        ActivityCompat.requestPermissions(this, permissions, RC_Handle_CAMERA_AND_INTERNET_PERM_AND_READ_PERM);
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
                && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera and internet permission granted");
            permissionsGranted = true;
            setContentView(R.layout.activity_main);

            return;
        }
        setContentView(R.layout.activity_main);

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("QR-Code-Scanner")
                .setMessage("Keine Erlaubnis für Kamera")
                .setPositiveButton("OK", listener)
                .show();
    }

    @Override
    public void sendImageToServer(final String id, final String client) {
        MetricsManager.trackEvent("Sending image");
        if(uat != null){
            uat.cancel(true);
        }

        uat = new UploadingAsyncTask();
        uat.execute(id, client);
    }

    class UploadingAsyncTask extends AsyncTask<String, Void, String> {
        ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        boolean gotServerMessage = false;

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Daten werden an den Server geschickt...");
            dialog.show();
            super.onPreExecute();
        }

        protected String doInBackground(String... args) {
            final String[] serverMessage = {""};
            final String id = args[0];
            final String client = args[1];
            final String imageInBytes = getImageForServer();

            try {
                socket = IO.socket(CONNECTION_STRING);
                socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Log.i("hallo", "connected");

                        String[] parts  =
                                Iterables.toArray(
                                        Splitter
                                                .fixedLength(100000)
                                                .split(imageInBytes),
                                        String.class
                                );
                        for(int i = 0; i<parts.length; i++){
                            JSONObject jsonObject = new JSONObject();
                            try {
                                if(i == parts.length-1){
                                    jsonObject.put("imagePart", parts[i]);
                                    jsonObject.put("displayId", id);
                                    jsonObject.put("last", true);
                                } else{
                                    jsonObject.put("imagePart", parts[i]);
                                    jsonObject.put("displayId", id);
                                    jsonObject.put("last", false);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            socket.emit("main_client", jsonObject, new Ack() {
                                @Override
                                public void call(Object... args) {
                                    serverMessage[0] = (String) args[0];
                                    Log.i("mainclient", serverMessage[0]);
                                    gotServerMessage = true;
                                    /*end = System.currentTimeMillis();
                                    Map<String, String> time = new HashMap<>();
                                    time.put("Zeit bis Bildempfang", ""+(end-start));
                                    MetricsManager.trackEvent(client, time);*/

                                }
                            });
                        }
                        onPostExecute(serverMessage[0]);

                        /*socket.on("sendChecksum", new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
                                Log.i("checksum", "hallo");
                                byte[] thedigest = null;
                                try {
                                    byte[] bytesOfMessage = imageInBytes.getBytes("UTF-8");
                                    MessageDigest md = MessageDigest.getInstance("MD5");
                                    thedigest = md.digest(bytesOfMessage);
                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }

                                JSONObject checksumJson = new JSONObject();
                                try {
                                    checksumJson.put("check", thedigest);
                                    checksumJson.put("id", id);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                socket.emit("checksum", checksumJson);

                            }
                        });*/
                    }
                });
                socket.connect();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return serverMessage[0];
        }

        protected void onPostExecute(final String result){
            if(gotServerMessage){
                socket.disconnect();
                socket.off();
                dialog.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
                        ad.setMessage(result);
                        ad.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        ad.show();
                    }
                });
                gotServerMessage = false;
            }

        }
    }
}