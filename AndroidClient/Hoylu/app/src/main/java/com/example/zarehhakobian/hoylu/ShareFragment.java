package com.example.zarehhakobian.hoylu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zarehhakobian.hoylu.CameraClasses.BarcodeGraphic;
import com.example.zarehhakobian.hoylu.CameraClasses.BarcodeTrackerFactory;
import com.example.zarehhakobian.hoylu.CameraClasses.CameraSource;
import com.example.zarehhakobian.hoylu.CameraClasses.CameraSourcePreview;
import com.example.zarehhakobian.hoylu.CameraClasses.GraphicOverlay;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.microsoft.azure.mobile.analytics.Analytics;

import net.hockeyapp.android.metrics.MetricsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Zareh Hakobian on 17.08.2017.
 */

public class ShareFragment extends Fragment implements BarcodeGraphic.BoundingBoxDrawListener{

    private static final String TAG = "Barcode-reader";
    // intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    private ArrayList<String> scannedBarcodeValues = new ArrayList<>();

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;

    ListView lv;
    TextView tv;
    final public ArrayList<HoyluDevice> hoyluDevices = new ArrayList<>();
    ArrayAdapter aa;

    BluetoothAdapter adapter;

    ArrayList<BluetoothDevice> deviceList;      //Own detected addresses
    ArrayList<HoyluDevice> filteredDeviceList;  //Matching addresses
    ArrayList<HoyluDevice> serverAquiredDeviceList; // Bluetoothaddresses from Server

    DhcpInfo d;
    WifiManager wifi;
    String publicIP, defaultGateway;
    private Socket networkSocket;
    Document doc = null;
    Timer t;

    private ScaleGestureDetector scaleGestureDetector;
    BarcodeDetector barcodeDetector;

    private Socket cameraSocket;
    Barcode barcode;
    boolean isGueltigeID;

    DeviceSelectedListener listener;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();


            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {                             //Startes searching for Devices
                Toast.makeText(getActivity(), "Bluetoothdiscovery has been started", Toast.LENGTH_LONG);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {                      //Ends searching for Devices
                Toast.makeText(getActivity(), "Bluetoothdiscovery has finished", Toast.LENGTH_LONG);
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {                                    //Found a Device, now compare its adress with the registered ones on the server
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i("DEVICE FOUND", "Name: "+device.getName()+ ", Address: " +device.getAddress());
                deviceList.add(device);
                matchAddresses();
            }

        }
    };

    public ShareFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.hoylushare_view, container, false);
        mPreview = (CameraSourcePreview) view.findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay<BarcodeGraphic>) view.findViewById(R.id.graphicOverlay);
        mPreview.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                boolean b = scaleGestureDetector.onTouchEvent(e);
                //boolean c = gestureDetector.onTouchEvent(e);
                return b;
            }
        });
        if (ShareActivity.permissionsGranted) {
            createCameraSource(true, false);
            //gestureDetector = new GestureDetector(getActivity(), new CaptureGestureListener());
            scaleGestureDetector = new ScaleGestureDetector(getActivity(), new ScaleListener());
        }

        adapter = BluetoothAdapter.getDefaultAdapter();
        deviceList = new ArrayList<BluetoothDevice>();
        filteredDeviceList = new ArrayList<HoyluDevice>();
        serverAquiredDeviceList = new ArrayList<HoyluDevice>();

        tv = (TextView) view.findViewById(R.id.textView);
        lv = (ListView) view.findViewById(R.id.listview);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(listener != null){
                    HoyluDevice hd = hoyluDevices.get(position);
                    ShareActivity.end = System.currentTimeMillis();
                    Map<String, String> time = new HashMap<>();
                    time.put("Zeit bis uploadImage Aufruf", ""+(ShareActivity.end- ShareActivity.start));
                    MetricsManager.trackEvent("NetworkClient", time);
                    ShareActivity.start = System.currentTimeMillis();
                    listener.uploadImageToServer(hd.getHoyluId(), "NetworkOrBluetoothClient");
                    if (networkSocket != null) {
                        networkSocket.disconnect();
                        networkSocket.off();
                    }
                    if(t != null){
                        t.cancel();
                    }
                    Map<String, String> properties = new HashMap<>();
                    properties.put("Device", hd.getHoyluId());
                    MetricsManager.trackEvent("Device selected", properties);
                    Analytics.trackEvent("Device selected", properties);
                }
            }
        });
        aa = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, hoyluDevices);
        lv.setAdapter(aa);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if(ShareActivity.isWifiConn){
            try {
                doc = Jsoup.connect("http://icanhazip.com").get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            wifi = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
            d = wifi.getDhcpInfo();

            defaultGateway = String.valueOf(Formatter.formatIpAddress(d.gateway));
            publicIP = doc.body().getAllElements().first().text();
            t = new Timer();
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    connectToServer(publicIP, defaultGateway);
                }
            }, 0, 10000);
        } else{
            tv.setText(R.string.turn_on_wifi);
        }
        super.onViewCreated(view, savedInstanceState);
    }

    public void connectToServer(final String pubIP, final String gateway) {
        if (networkSocket != null) {
            networkSocket.disconnect();
        }

        try {
            networkSocket = IO.socket(ShareActivity.CONNECTION_STRING);
            networkSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.i("hallo", "connected");
                    networkSocket.emit("client", "NetworkClient");
                    JSONObject j = new JSONObject();
                    try {
                        j.put("pub", pubIP);
                        j.put("gateway", gateway);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    networkSocket.emit("addresses", j, new Ack() {
                        @Override
                        public void call(Object... args) {
                            hoyluDevices.clear();
                            try {
                                JSONObject j = (JSONObject) args[0];
                                JSONArray dev = j.getJSONArray("list");
                                if(dev.length() > 0){
                                    for (int i = 0; i < dev.length(); i++) {
                                        JSONObject jsonObject = dev.getJSONObject(i);
                                        hoyluDevices.add(JSONToHoyluDevice(jsonObject));
                                    }
                                    if(getActivity() != null){
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                aa.notifyDataSetChanged();
                                            }
                                        });
                                    }

                                    for (HoyluDevice d : hoyluDevices) {
                                        Log.i("NetworkDevicesServer", d.toString());
                                    }
                                } else{
                                    if(getActivity() != null){
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                aa.notifyDataSetChanged();
                                                Toast.makeText(getActivity(), R.string.no_devices,Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            networkSocket.disconnect();
                            networkSocket.off();
                        }
                    });
                    Log.d("Before BT emit", "Emitting BTClient now");
                    networkSocket.emit("client", "BluetoothClient");
                    Log.d("Before BT address emit", "Emitting BTAddresses now");
                    networkSocket.emit("bluetoothAddresses", "", new Ack() {

                        @Override
                        public void call(Object... args) {
                            Log.d("BTAddressesCalled", "BTAddressesCall to Server");
                            serverAquiredDeviceList.clear();
                            try {
                                JSONObject j = (JSONObject) args[0];
                                JSONArray dev = j.getJSONArray("list");
                                Log.d("BTLIST", dev.toString());
                                for (int i = 0; i < dev.length(); i++) {
                                    JSONObject jsonObject = dev.getJSONObject(i);
                                    serverAquiredDeviceList.add(JSONToHoyluDevice(jsonObject));
                                    if(getActivity() != null) {
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                matchAddresses();
                                            }
                                        });
                                    }
                                    networkSocket.disconnect();
                                    networkSocket.off();
                                    if(getActivity() != null) {
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                aa.notifyDataSetChanged();
                                            }
                                        });
                                    }
                                }
                                for (HoyluDevice d : serverAquiredDeviceList) {
                                    Log.i("ServerBluetoothDevices", d.toString() + " " + d.BluetoothAddress);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
            networkSocket.connect();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), R.string.server_off, Toast.LENGTH_SHORT).show();
        }
        if(networkSocket.connected()==false){
            Toast.makeText(getActivity(), R.string.server_off, Toast.LENGTH_SHORT).show();
        }
    }

    private HoyluDevice JSONToHoyluDevice(JSONObject jsonObject) throws JSONException {
        String id = jsonObject.getString("hoyluId");
        String name = jsonObject.getString("name");
        String btAddress = jsonObject.getString("btAddress");
        String qrValue = jsonObject.getString("qrValue");
        String nfcValue = jsonObject.getString("nfcValue");
        String pubIp = jsonObject.getString("publicIp");
        String defGate = jsonObject.getString("defaultGateway");
        String socketId = jsonObject.getString("socketId");
        HoyluDevice hd = new HoyluDevice(id, name,btAddress,qrValue,nfcValue, pubIp, defGate,socketId);
        return hd;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof DeviceSelectedListener) {
            listener = (DeviceSelectedListener) activity;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        startCameraSource();
        InitializeBluetoothDiscovery();
    }

    private void InitializeBluetoothDiscovery() {
        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        getActivity().registerReceiver(mReceiver, filter);
        adapter.startDiscovery();
    }

    @Override
    public void onStop() {
        if(cameraSocket != null && cameraSocket.connected()){
            cameraSocket.disconnect();
            cameraSocket.off();
            mPreview.release();
        }

        if(networkSocket != null){
            networkSocket.disconnect();
            networkSocket.off();
        }
        if(t != null){
            t.cancel();
        }
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        startCameraSource();
    }

    public void onDestroy() {
        getActivity().unregisterReceiver(mReceiver);

        super.onDestroy();
    }

    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        Context context = getActivity().getApplicationContext();
        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, track the barcodes, and maintain
        // graphics for each barcode on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.
        barcodeDetector = new BarcodeDetector.Builder(context).build();
        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(mGraphicOverlay, this);
        barcodeDetector.setProcessor(
                new MultiProcessor.Builder<>(barcodeFactory).build());

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int height = metrics.heightPixels;
        int width = metrics.widthPixels;

        CameraSource.Builder builder = new CameraSource.Builder(getActivity().getApplicationContext(), barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(width, height)
                .setRequestedFps(15.0f);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder = builder.setFocusMode(
                    autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);
        }

        mCameraSource = builder
                .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                .build();
    }

    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getActivity().getApplicationContext());

        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    private void matchAddresses() {
       // filteredDeviceList.clear();
        for (BluetoothDevice d:
                deviceList) {
            for (HoyluDevice dc:
                    serverAquiredDeviceList) {
                String dAdd = d.getAddress();
                if(d.getAddress().equals(dc.BluetoothAddress))
                {
                    hoyluDevices.add(dc);
                    Log.i("MATCHED", "Device " +d.getName()+" matched");
                }
            }
        }
        aa.notifyDataSetChanged();
    }

    public void bestCodeCaptured(Barcode best) {
        barcode = best;

        if(!scannedBarcodeValues.contains(barcode.displayValue)) {
            if (cameraSocket != null) {
                cameraSocket.close();
            }

            try {
                cameraSocket = IO.socket(ShareActivity.CONNECTION_STRING);
                cameraSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Log.i("hallo", "connected");
                        cameraSocket.emit("client", "CameraClient");
                        cameraSocket.emit("qr_code", barcode.displayValue, new Ack() {
                            @Override
                            public void call(Object... args) {
                                isGueltigeID = (boolean) args[0];
                                if(isGueltigeID) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            scannedBarcodeValues.add(barcode.displayValue);
                                            Toast.makeText(getActivity(), R.string.valid, Toast.LENGTH_SHORT).show();
                                            if (listener != null) {
                                                ShareActivity.end = System.currentTimeMillis();
                                                Map<String, String> time = new HashMap<>();
                                                time.put("Zeit bis uploadImage Aufruf", "" + (ShareActivity.end - ShareActivity.start));
                                                MetricsManager.trackEvent("CameraClient", time);
                                                ShareActivity.start = System.currentTimeMillis();
                                                listener.uploadImageToServer(barcode.displayValue, "CameraClient");
                                                if (cameraSocket != null) {
                                                    cameraSocket.disconnect();
                                                    cameraSocket.off();
                                                }
                                            }
                                        }
                                    });
                                    mPreview.release();

                                }else {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            scannedBarcodeValues.add(barcode.displayValue);
                                            Toast.makeText(getActivity(), R.string.invalid, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                cameraSocket.disconnect();
                                cameraSocket.off();
                            }
                        });
                    }
                });
                cameraSocket.connect();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), R.string.server_off, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBoundingBoxDrawn(Barcode code) {
        if(!scannedBarcodeValues.contains(code.displayValue)){
            bestCodeCaptured(code);
            Map<String, String> properties = new HashMap<>();
            properties.put("Barcode", code.displayValue);
            MetricsManager.trackEvent("Best barcode captured", properties);
            Analytics.trackEvent("Best barcode captured", properties);
        }
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {
        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         * Once a scale has ended, {@link ScaleGestureDetector#getFocusX()}
         * and {@link ScaleGestureDetector#getFocusY()} will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mCameraSource.doZoom(detector.getScaleFactor());
        }


    }
}
