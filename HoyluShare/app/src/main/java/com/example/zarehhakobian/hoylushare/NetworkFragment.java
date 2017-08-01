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
import android.widget.Toast;

import com.example.zarehhakobian.hoylushare.NetworkClasses.NetworkDevice;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
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
 * Created by Zareh Hakobian on 13.07.2017.
 */

public class NetworkFragment extends Fragment {
    ListView lv;
    final public ArrayList<HoyluDevice> hoyluDevices = new ArrayList<>();
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
                    HoyluDevice hd = hoyluDevices.get(position);
                    MainActivity.end = System.currentTimeMillis();
                    Map<String, String> time = new HashMap<>();
                    time.put("Zeit bis Async Aufruf", ""+(MainActivity.end-MainActivity.start));
                    MetricsManager.trackEvent("NetworkClient", time);
                    MainActivity.start = System.currentTimeMillis();
                    listener.sendImageToServer(hd.getHoyluId(), "NetworkClient");
                    Map<String, String> properties = new HashMap<>();
                    properties.put("Device", hd.getHoyluId());
                    MetricsManager.trackEvent("Device selected", properties);
                    Analytics.trackEvent("Device selected", properties);
                }
            }
        });
        aa = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, hoyluDevices);
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
            }

        }, 0, 10000);
        super.onViewCreated(view, savedInstanceState);
    }

    public void connectToServer(final String pubIP, final String gateway) {
        if (socket != null) {
            socket.disconnect();
        }

        try {
            socket = IO.socket(MainActivity.CONNECTION_STRING);
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
                            hoyluDevices.clear();
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
                                    HoyluDevice hd = new HoyluDevice(id, name,btAddress,qrValue,nfcValue, pubIp, defGate);
                                    hoyluDevices.add(hd);
                                    socket.disconnect();
                                    socket.off();
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            aa.notifyDataSetChanged();
                                            Toast.makeText(getActivity(), "Neu gefüllt",Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                for (HoyluDevice d : hoyluDevices) {
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
