package com.example.zarehhakobian.hoylu;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;
import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.analytics.Analytics;
import com.microsoft.azure.mobile.crashes.Crashes;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;
import net.hockeyapp.android.metrics.MetricsManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ShareActivity extends AppCompatActivity implements DeviceSelectedListener {

    private static final String TAG = "Share";
    public static final String CONNECTION_STRING = "http://linz-eng-test.westeurope.cloudapp.azure.com:4200";
    private static final int RC_Handle_CAMERA_AND_INTERNET_PERM_AND_READ_PERM = 2;
    public static boolean permissionsGranted = false;
    Socket socket;
    private Socket nfcSocket;
    public static long start;
    public static long end;
    ProgressDialog progressDialog;
    public static boolean isWifiConn, isMobileConn;

    Resources res;
    DisplayMetrics dm;
    Configuration conf;
    public static File fileToSend = null;
    public File downloadedFile = null;

    private NfcAdapter nfcAdapter;
    PendingIntent mPendingIntent;

    BluetoothAdapter adapter;
    public static ArrayList<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();      //Own detected addresses


    public final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {                             //Startes searching for Devices
               // Toast.makeText(ShareActivity.Context, "Bluetoothdiscovery has been started", Toast.LENGTH_LONG).show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {                      //Ends searching for Devices
               // Toast.makeText(getApplicationContext(), "Bluetoothdiscovery has finished", Toast.LENGTH_LONG).show();
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {                                    //Found a Device, now compare its adress with the registered ones on the server
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i("DEVICE FOUND", "Name: "+device.getName()+ ", Address: " +device.getAddress());
                deviceList.add(device);
                //matchAddresses();
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLanguage();
        checkConnectivity();
        if (!isWifiConn && !isMobileConn) {
            AlertDialog.Builder ad = new AlertDialog.Builder(ShareActivity.this);
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
            AlertDialog.Builder ad = new AlertDialog.Builder(ShareActivity.this);
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

        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
        }
        else if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC is disabled.", Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(this, "NFC is ready to be used!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
        if(mReceiver!=null && mReceiver.isOrderedBroadcast()){
            unregisterReceiver(mReceiver);
        }
    }

    @Override
    protected void onNewIntent(Intent intent){
        getTagInfo(intent);
    }

    private void getTagInfo(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if(tag != null){
            Ndef ndefTag = Ndef.get(tag);
            NdefMessage ndefMesg = ndefTag.getCachedNdefMessage();
            NdefRecord record = ndefMesg.getRecords()[0];
            String value = new String(record.getPayload());
            Log.i("NFC TAG SCANNED", value + " || Size: " + ndefTag.getMaxSize());
            Log.i("NFC TAG SCANNED fixed", value.substring(3) + " || Size: " + ndefTag.getMaxSize());
            uploadImageToServer(value.substring(3), "NFCClient");
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
            setContentView(R.layout.activity_share);

            //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
            adapter = BluetoothAdapter.getDefaultAdapter();
            InitializeBluetoothDiscovery();
        } else {
            requestPermissions();
        }
    }

    private void InitializeBluetoothDiscovery() {
        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mReceiver, filter);
        adapter.startDiscovery();
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
        if (socket != null) {
            socket.disconnect();
            socket.off();
        }
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
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

    private File getFileForServer() {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            return getFileFromIntent(intent);
        } else {
            return fileToSend;
        }
    }

    private File getFileFromIntent(final Intent intent) {
        InputStream stream = null;
        try {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                String uriString = uri.toString();
                if (uriString.startsWith("content://media")) {
                    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                    try {
                        if (cursor != null && cursor.moveToFirst()) {
                            String path = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
                            return new File(path);
                        }
                    } finally {
                        cursor.close();
                    }
                } else if (uri.getScheme().equals("file")) {
                    return new File(new File(uri.getPath()).getAbsolutePath());

                } else {
                    File filesDir = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    File x = new File(uri.getPath());
                    String n = x.getName();
                    downloadedFile = new File(filesDir, n);
                    stream = getContentResolver().openInputStream(uri);
                    OutputStream os = new FileOutputStream(downloadedFile);
                    byte buf[] = new byte[1024];
                    int len;
                    while ((len = stream.read(buf)) > 0) os.write(buf, 0, len);
                    os.flush();
                    os.close();
                    stream.close();
                    return downloadedFile;
                }
            } else{
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
        progressDialog = new ProgressDialog(ShareActivity.this);
        progressDialog.setMessage(getResources().getString(R.string.uploading_message));
        progressDialog.setCancelable(false);
        progressDialog.setProgressNumberFormat(null);
        progressDialog.setProgressPercentFormat(NumberFormat.getPercentInstance());
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.show();
        fileToSend = getFileForServer();
        if(fileToSend!=null) {
            Ion.with(getApplicationContext())
                    .load("POST", CONNECTION_STRING + "/file_upload")
                    .uploadProgressHandler(new ProgressCallback() {
                        @Override
                        public void onProgress(long uploaded, long total) {
                            progressDialog.setMax((int)total);
                            progressDialog.setProgress((int)uploaded);
                            progressDialog.show();
                        }
                    })
                    .setMultipartParameter("Name", "source")
                    .setMultipartFile("File", fileToSend)
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
                                time.put("Zeit bis Upload", "" + (end - start));
                                MetricsManager.trackEvent(client, time);
                                final String finalFilename = filename;
                                final String finalOriginalName = originalName;
                                notifyServer(finalFilename, id, finalOriginalName);

                            } else {
                                AlertDialog.Builder ad = new AlertDialog.Builder(ShareActivity.this);
                                ad.setCancelable(false);
                                ad.setMessage(R.string.uploading_error);
                                ad.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        if(socket != null){
                                            socket.off();
                                            socket.disconnect();
                                        }
                                        if(downloadedFile != null){
                                            downloadedFile.delete();
                                        }
                                        finish();
                                    }
                                });
                                ad.create().show();
                            }
                        }
                    });
        } else{
            AlertDialog.Builder ad = new AlertDialog.Builder(ShareActivity.this);
            ad.setCancelable(false);
            ad.setMessage(R.string.uploading_error);
            ad.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    if(socket != null){
                        socket.off();
                        socket.disconnect();
                    }
                    if(downloadedFile != null){
                        downloadedFile.delete();
                    }
                    finish();
                }
            });
            ad.create().show();
        }
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
                if (receiverbenachrichtigt) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder ad = new AlertDialog.Builder(ShareActivity.this);
                            ad.setCancelable(false);
                            ad.setMessage(R.string.upload_successful);
                            ad.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    socket.off();
                                    socket.disconnect();
                                    if(downloadedFile != null){
                                        downloadedFile.delete();
                                    }
                                    Toast.makeText(getApplication(), R.string.notify_server_true, Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            });
                            ad.show();
                        }
                    });

                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder ad = new AlertDialog.Builder(ShareActivity.this);
                            ad.setMessage(R.string.uploading_error);
                            ad.setCancelable(false);
                            ad.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    socket.off();
                                    socket.disconnect();
                                    if(downloadedFile != null){
                                        downloadedFile.delete();
                                    }
                                    Toast.makeText(getApplication(), R.string.notify_server_false, Toast.LENGTH_SHORT).show();
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