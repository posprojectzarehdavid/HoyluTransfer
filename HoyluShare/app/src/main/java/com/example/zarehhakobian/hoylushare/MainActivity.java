package com.example.zarehhakobian.hoylushare;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.analytics.Analytics;
import com.microsoft.azure.mobile.crashes.Crashes;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;
import net.hockeyapp.android.metrics.MetricsManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends Activity implements DeviceSelectedListener {

    private static final String TAG = "Main";
    public static final String CONNECTION_STRING = "http://40.114.246.211:4200";
    private static final int RC_Handle_CAMERA_AND_INTERNET_PERM_AND_READ_PERM = 2;
    public static boolean permissionsGranted = false;
    Socket socket;
    public static long start;
    public static long end;
    ProgressDialog progressDialog;
    public static boolean isWifiConn, isMobileConn;
    boolean receiverBenachrichtigt;

    Resources res;
    DisplayMetrics dm;
    Configuration conf;
    File imageFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLanguage();
        checkConnectivity();
        if (!isWifiConn && !isMobileConn) {
            AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
            ad.setTitle(R.string.no_inet_connection);
            ad.setMessage(R.string.want_connection);
            ad.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), 0);
                }
            });
            ad.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Toast.makeText(getApplication(), R.string.destroy_message,
                            Toast.LENGTH_LONG).show();
                    finish();
                }
            });
            ad.show();
        }
        if (!isWifiConn && isMobileConn) {
            AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
            ad.setTitle(R.string.attention);
            ad.setMessage(R.string.identify_message);
            ad.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), 0);
                }
            });
            ad.show();
        }
        if (isWifiConn) {
            startMethod();
        }
    }

    private void setLanguage() {
        String systemlanguage = Locale.getDefault().getDisplayLanguage();
        res = getResources();
        dm = res.getDisplayMetrics();
        conf = res.getConfiguration();

        if (systemlanguage.equals("Deutsch") || systemlanguage.equals("German")) {
            Log.i("syslang", "systemlang gesetzt " + systemlanguage);
            conf.locale = new Locale("de");
        } else {
            conf.locale = new Locale("en");
        }
        res.updateConfiguration(conf, dm);
    }

    private void startMethod() {
        start = System.currentTimeMillis();
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int i = ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        int readPerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (rc == PackageManager.PERMISSION_GRANTED &&
                i == PackageManager.PERMISSION_GRANTED &&
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            checkConnectivity();
            if (isWifiConn || isMobileConn) {
                startMethod();
            } else {
                finish();
            }
        }
    }

    public void checkConnectivity() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        isWifiConn = networkInfo.isConnected();
        networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        isMobileConn = networkInfo.isConnected();
    }

    @Override
    protected void onStop() {
        super.onStop();
        end = 0;
        start = 0;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (socket != null) {
            socket.disconnect();
            socket.off();
        }
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    private File getImageForServer() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                //return new File(getImagePathFromIntent(intent));
                return getFileFromIntent(intent);
            }
        }
        return null;
    }

    private File getFileFromIntent(Intent intent) {
        InputStream stream = null;
        try {
            UUID uuid = UUID.randomUUID();
            Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            String filename = null;
            if (imageUri.getScheme().equals("content")) {
                Cursor cursor = getContentResolver().query(imageUri, null, null, null, null);
                try {
                    if (cursor != null && cursor.moveToFirst()) {
                        filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } finally {
                    cursor.close();
                }
            } else{
                filename = uuid.toString();
            }
            stream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            stream.close();

            File filesDir = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            imageFile = new File(filesDir, filename);

            OutputStream os;
            os = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            os.flush();
            os.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageFile;
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
        return filePath;
    }

    private void requestPermissions() {
        Log.w(TAG, "Camera and internet permission is not granted. Requesting permission");
        final String[] permissions = new String[]{android.Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.INTERNET};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.INTERNET)
                && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
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

        if (grantResults.length != 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
                && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera and internet permission granted");
            startMethod();
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
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton("OK", listener)
                .show();
    }

    @Override
    public void uploadImageToServer(final String id, final String client) {
        MetricsManager.trackEvent("Uploading image");
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage(getResources().getString(R.string.uploading_message));
        progressDialog.show();

        Ion.with(getApplicationContext())
                .load("POST", "http://40.114.246.211:4200/file_upload")
                .setMultipartParameter("name", "source")
                .setMultipartFile("image", "image/jpeg", getImageForServer())
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        progressDialog.dismiss();
                        JSONObject j;
                        boolean uploaded = false;
                        String filename = "";
                        String originalName = "";
                        try {
                            j = new JSONObject(result);
                            uploaded = j.optBoolean("uploaded");
                            filename = j.optString("filename");
                            originalName = j.optString("originalName");
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                        if (uploaded) {
                            end = System.currentTimeMillis();
                            Map<String, String> time = new HashMap<>();
                            time.put("Zeit bis Bildupload", "" + (end - start));
                            MetricsManager.trackEvent(client, time);
                            final String finalFilename = filename;
                            final String finalOriginalName = originalName;
                            notifyServer(finalFilename, id, finalOriginalName);

                        } else {
                            AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
                            ad.setMessage(R.string.uploading_error);
                            ad.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    socket.off();
                                    socket.disconnect();
                                    finish();
                                }
                            });
                        }
                    }
                });
    }

    private void notifyServer(String filename, String id, String originalName) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("filename", filename);
            jsonObject.put("displayId", id);
            jsonObject.put("originalName", originalName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        connectToServer();

        socket.emit("uploadFinished", jsonObject, new Ack() {
            @Override
            public void call(Object... args) {
                boolean receiverbenachrichtigt = (boolean) args[0];
                Log.i("benachrichtigt", "hallo");
                if(receiverbenachrichtigt){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
                            ad.setMessage(R.string.upload_successful);
                            ad.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    socket.off();
                                    socket.disconnect();
                                    imageFile.delete();
                                    Toast.makeText(getApplication(),R.string.notify_server_true, Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            });
                            ad.show();
                        }
                    });

                }
                else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
                            ad.setMessage(R.string.uploading_error);
                            ad.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    socket.off();
                                    socket.disconnect();
                                    imageFile.delete();
                                    Toast.makeText(getApplication(),R.string.notify_server_false, Toast.LENGTH_SHORT).show();
                                }
                            });
                            ad.show();
                        }
                    });
                }
            }
        });
    }

    public void connectToServer() {
        try {
            socket = IO.socket(CONNECTION_STRING);
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.i("hallo", "connected");
                    socket.emit("client", "MainClient");
                }
            });
            socket.connect();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), R.string.server_off, Toast.LENGTH_SHORT).show();
        }
    }
}