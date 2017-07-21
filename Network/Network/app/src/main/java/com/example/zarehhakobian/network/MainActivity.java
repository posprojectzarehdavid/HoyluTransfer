package com.example.zarehhakobian.network;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    final ArrayList<NetworkDevice> networkDevices = new ArrayList<>();
    ArrayAdapter aa;
    ListView lv;

    TextView tvIp, tvDefaultGW, tvPublic, tvServer, tvSubnet;
    DhcpInfo d;
    WifiManager wifi;
    String ipAddress, publicIP, defaultGateway;
    private Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        aa = new ArrayAdapter(this, android.R.layout.simple_list_item_1, networkDevices);
        lv = (ListView) findViewById(R.id.listview);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                NetworkDevice d = networkDevices.get(position);
                Toast.makeText(getApplicationContext(), "DefaultGateway: " + d.defaultgateway, Toast.LENGTH_LONG).show();
            }
        });
        lv.setAdapter(aa);

        tvIp = (TextView) findViewById(R.id.tvIpAddress);
        tvDefaultGW = (TextView) findViewById(R.id.tvDefaultGW);
        tvPublic = (TextView) findViewById(R.id.tvPublic);
        tvServer = (TextView) findViewById(R.id.tvServer);
        tvSubnet = (TextView) findViewById(R.id.tvSubnet);
        Document doc = null;
        try {
            doc = Jsoup.connect("http://www.checkip.org").get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        d = wifi.getDhcpInfo();

        ipAddress = String.valueOf(Formatter.formatIpAddress(d.ipAddress));
        defaultGateway = String.valueOf(Formatter.formatIpAddress(d.gateway));
        publicIP = doc.getElementById("yourip").select("h1").first().select("span").text();
        tvDefaultGW.setText("Default Gateway: " + String.valueOf(Formatter.formatIpAddress(d.gateway)));
        tvIp.setText("IP Address: " + String.valueOf(Formatter.formatIpAddress(d.ipAddress)));
        tvSubnet.setText("Subnet Mask: " + String.valueOf(Formatter.formatIpAddress(d.netmask)));
        tvServer.setText("Server IP: " + String.valueOf(Formatter.formatIpAddress(d.serverAddress)));
        tvPublic.setText("Public IP: " + publicIP);

        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                connectToServer(publicIP, defaultGateway);
                Log.i("hallo", System.currentTimeMillis()+"");
            }

        }, 0, 10000);

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
                                    runOnUiThread(new Runnable() {
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

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        socket.disconnect();
        super.onStop();
    }
}
