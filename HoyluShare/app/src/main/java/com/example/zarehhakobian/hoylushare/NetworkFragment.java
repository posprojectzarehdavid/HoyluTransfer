package com.example.zarehhakobian.hoylushare;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.zarehhakobian.hoylushare.NetworkClasses.NetworkDevice;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Zareh Hakobian on 13.07.2017.
 */

public class NetworkFragment extends Fragment {
    ListView lv;
    final public ArrayList<NetworkDevice> networkDevices = new ArrayList<>();
    ArrayAdapter aa;

    DhcpInfo d;
    WifiManager wifi;
    String publicIP, defaultGateway;
    private Socket socket;
    Document doc = null;

    DeviceSelectedListener listener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.network_list,container,false);
        lv = (ListView) v.findViewById(R.id.listview);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(listener != null){
                    listener.sendImageToServer(networkDevices.get(position).getId());
                }
            }
        });
        aa = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, networkDevices);
        lv.setAdapter(aa);

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof DeviceSelectedListener) {
            listener = (DeviceSelectedListener) activity;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        socket.disconnect();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            doc = Jsoup.connect("http://www.checkip.org").get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        wifi = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        d = wifi.getDhcpInfo();

        defaultGateway = String.valueOf(Formatter.formatIpAddress(d.gateway));
        publicIP = doc.getElementById("yourip").select("h1").first().select("span").text();
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                connectToServer(publicIP, defaultGateway);
                Log.i("hallo", System.currentTimeMillis()+"");
            }

        }, 0, 10000);
        super.onViewCreated(view, savedInstanceState);
    }

    public void connectToServer(final String pubIP, final String gateway) {

        if (socket != null) {
            socket.close();
        }

        try {
            socket = IO.socket("http://192.168.42.85:4200");
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.i("hallo", "connected");
                    socket.emit("client", "NetworkClient");
                    JSONObject j = new JSONObject();
                    try {
                        j.put("pub", pubIP);
                        j.put("gateway", gateway);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    socket.emit("addresses", j, new Ack() {
                        @Override
                        public void call(Object... args) {
                            networkDevices.clear();
                            try {
                                JSONObject j = (JSONObject) args[0];
                                JSONArray dev = j.getJSONArray("list");
                                for (int i = 0; i < dev.length(); i++) {
                                    JSONObject jsonObject = dev.getJSONObject(i);
                                    String id = jsonObject.getString("id");
                                    String name = jsonObject.getString("name");
                                    String pubIp = jsonObject.getString("publicIP");
                                    String defGate = jsonObject.getString("defaultGateway");
                                    NetworkDevice nd = new NetworkDevice(id, name, pubIp, defGate);
                                    networkDevices.add(nd);
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            aa.notifyDataSetChanged();
                                        }
                                    });
                                }
                                for (NetworkDevice d : networkDevices) {
                                    Log.i("hallo", d.toString());
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
