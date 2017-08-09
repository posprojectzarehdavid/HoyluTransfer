package com.example.zarehhakobian.hoylushare;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.zarehhakobian.hoylushare.BluetoothClasses.BluetoothDeviceCustom;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by David on 29.07.2017.
 */

public class BluetoothFragment extends Fragment {
    View view;
    BluetoothAdapter adapter;

    ArrayList<BluetoothDevice> deviceList;      //Own detected addresses
    ArrayList<HoyluDevice> filteredDeviceList;  //Matching addresses
    ArrayList<HoyluDevice> serverAquiredDeviceList; // Bluetoothaddresses from Server

    ArrayAdapter aa;


    Set<BluetoothDevice> pairedDevices;

    ListView lv;
    DeviceSelectedListener listener;

    private Socket socket;



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.bluetooth_list, container, false);
        lv = (ListView) view.findViewById(R.id.listview);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (listener != null) {
                    listener.uploadImageToServer(filteredDeviceList.get(position).getHoyluId(), "BluetoothClient");             //Was mitgeben?
                }
            }
        });
        adapter = BluetoothAdapter.getDefaultAdapter();
        deviceList = new ArrayList<BluetoothDevice>();
        filteredDeviceList = new ArrayList<HoyluDevice>();
        serverAquiredDeviceList = new ArrayList<HoyluDevice>();

        aa = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, filteredDeviceList);
        lv.setAdapter(aa);

        pairedDevices = new Set<BluetoothDevice>() {
            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @NonNull
            @Override
            public Iterator<BluetoothDevice> iterator() {
                return null;
            }

            @NonNull
            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @NonNull
            @Override
            public <T> T[] toArray(@NonNull T[] a) {
                return null;
            }

            @Override
            public boolean add(BluetoothDevice bluetoothDevice) {
                return false;
            }

            @Override
            public boolean remove(Object o) {
                return false;
            }

            @Override
            public boolean containsAll(@NonNull Collection<?> c) {
                return false;
            }

            @Override
            public boolean addAll(@NonNull Collection<? extends BluetoothDevice> c) {
                return false;
            }

            @Override
            public boolean retainAll(@NonNull Collection<?> c) {
                return false;
            }

            @Override
            public boolean removeAll(@NonNull Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {

            }
        };

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof DeviceSelectedListener) {
            listener = (DeviceSelectedListener) activity;
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        connectToServer();
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                connectToServer();
            }

        }, 0, 10000);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        InitializeBluetoothDiscovery();
    }

    private void InitializeBluetoothDiscovery() {
        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        getActivity().registerReceiver(mReceiver, filter);
        pairedDevices = adapter.getBondedDevices();
        adapter.startDiscovery();
    }

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

    private void matchAddresses() {
        filteredDeviceList.clear();
        for (BluetoothDevice d:
             deviceList) {
            for (HoyluDevice dc:
                 serverAquiredDeviceList) {
                String dAdd = d.getAddress();
                if(d.getAddress().equals(dc.btAddress))
                {
                    filteredDeviceList.add(dc);
                    Log.i("MATCHED", "Device " +d.getName()+" matched");
                }
            }
        }
        aa.notifyDataSetChanged();
    }

    public void onDestroy() {
        getActivity().unregisterReceiver(mReceiver);

        super.onDestroy();
    }

    public void connectToServer() {
        if (socket != null) {
            socket.disconnect();
        }

        try {
            socket = IO.socket(MainActivity.CONNECTION_STRING);
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.i("hallo", "connected");
                    socket.emit("client", "BluetoothClient");

                    socket.emit("bluetoothAddresses", "", new Ack() {
                        @Override
                        public void call(Object... args) {
                            serverAquiredDeviceList.clear();
                            try {
                                JSONObject j = (JSONObject) args[0];
                                JSONArray dev = j.getJSONArray("list");
                                for (int i = 0; i < dev.length(); i++) {
                                    JSONObject jsonObject = dev.getJSONObject(i);
                                    String id = jsonObject.getString("hoyluId");
                                    String name = jsonObject.getString("name");
                                    String btAddress = jsonObject.getString("btAddress");
                                    String qrValue = jsonObject.getString("qrValue");
                                    String nfcValue = jsonObject.getString("nfcValue");
                                    String pubIp = jsonObject.getString("publicIp");
                                    String defGate = jsonObject.getString("defaultGateway");
                                    String socketId = jsonObject.getString("socketId");
                                    HoyluDevice hd = new HoyluDevice(id, name,btAddress,qrValue,nfcValue, pubIp, defGate,socketId);
                                    serverAquiredDeviceList.add(hd);
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            matchAddresses();
                                        }
                                    });
                                    socket.disconnect();
                                    socket.off();
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            aa.notifyDataSetChanged();
                                        }
                                    });
                                }
                                for (HoyluDevice d : serverAquiredDeviceList) {
                                    Log.i("ServerBluetoothDevices", d.toString() + " " + d.btAddress);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
            socket.connect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
