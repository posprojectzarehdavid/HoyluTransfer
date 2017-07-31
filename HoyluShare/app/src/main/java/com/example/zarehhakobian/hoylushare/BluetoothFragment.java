package com.example.zarehhakobian.hoylushare;

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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by David on 29.07.2017.
 */

public class BluetoothFragment extends Fragment {
    View view;
    BluetoothAdapter adapter;

    ArrayList<BluetoothDevice> deviceList;      //Own detected addresses
    ArrayList<BluetoothDevice> filteredDeviceList;  //Matching addresses
    ArrayList<BluetoothDeviceCustom> serverAquiredDeviceList; // Bluetoothaddresses from Server

    ArrayAdapter aa;


    Set<BluetoothDevice> pairedDevices;

    ListView lv;
    DeviceSelectedListener listener;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.bluetooth_list, container, false);
        lv = (ListView) view.findViewById(R.id.listview);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (listener != null) {
                    listener.sendImageToServer(filteredDeviceList.get(position).getAddress());             //Was mitgeben?
                }
            }
        });
        adapter = BluetoothAdapter.getDefaultAdapter();
        deviceList = new ArrayList<BluetoothDevice>();
        filteredDeviceList = new ArrayList<BluetoothDevice>();
        serverAquiredDeviceList = new ArrayList<BluetoothDeviceCustom>();

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
        for (BluetoothDevice d:
             deviceList) {
            for (BluetoothDeviceCustom dc:
                 serverAquiredDeviceList) {
                if(d.getAddress() == dc.bluetoothAddress)
                {
                    filteredDeviceList.add(d);
                    Log.i("MATCHED", "Device " +d.getName()+" matched");
                }
            }
        }
        aa.clear();
        aa.notifyDataSetChanged();
    }

    public void onDestroy() {
        getActivity().unregisterReceiver(mReceiver);

        super.onDestroy();
    }

}
