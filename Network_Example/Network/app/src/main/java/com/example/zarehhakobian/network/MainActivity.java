package com.example.zarehhakobian.network;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    ArrayList<NetworkInterface>geraeteListe;
    ArrayList<String>al;
    ArrayAdapter aa;
    ListView lv;
    private int LoopCurrentIP = 0;
    String ad;
    TextView tvIp,tvDefaultGW,tvPublic,tvServer,tvSubnet;
    DhcpInfo d;
    WifiManager wifi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        al = new ArrayList<>();
        aa = new ArrayAdapter(this,android.R.layout.simple_list_item_1,al);
        //lv = (ListView) findViewById(R.id.listview);
        //lv.setAdapter(aa);
        //new NetworkSniffTask(this).execute();

        //geraeteListe = getConnectedDevices(getLocalIpAddress());
        /*try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for(NetworkInterface netint : Collections.list(nets)){
                geraeteListe.add(netint);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }*/
        //search();
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
        tvDefaultGW.setText("Default Gateway: "+String.valueOf(Formatter.formatIpAddress(d.gateway)));
        tvIp.setText("IP Address: "+String.valueOf(Formatter.formatIpAddress(d.ipAddress)));
        tvSubnet.setText("Subnet Mask: "+String.valueOf(Formatter.formatIpAddress(d.netmask)));
        tvServer.setText("Server IP: "+String.valueOf(Formatter.formatIpAddress(d.serverAddress)));
        tvPublic.setText("Public IP: "+doc.getElementById("yourip").select("h1").first().select("span").text());

        aa.notifyDataSetChanged();
    }

    public void search(){
        Vector<String> Available_Devices=new Vector<>();
        String myip = null;
        try {
            myip = InetAddress.getLocalHost().getHostAddress();
            String mynetworkips = new String();

            for(int i = myip.length(); i>0; --i) {
                if(myip.charAt(i-1) == '.'){
                    mynetworkips=myip.substring(0,i);
                    break;
                }
            }

            Log.i("hallo","My Device IP: " + myip+"\n");

            Log.i("hallo","Search log:");
            for(int i=1;i<=254;++i){
                try {
                    InetAddress addr=InetAddress.getByName(mynetworkips + new Integer(i).toString());
                    if (addr.isReachable(1000)){
                        System.out.println("Available: " + addr.getHostAddress());
                        Available_Devices.add(addr.getHostAddress());
                    }
                    else Log.i("hallo","Not available: "+ addr.getHostAddress());

                }catch (IOException ioex){}
            }

            Log.i("hallo","\nAll Connected devices(" + Available_Devices.size() +"):");
            for(int i=0; i<Available_Devices.size(); ++i) {
                al.add(Available_Devices.get(i));
                Log.i("hallo",Available_Devices.get(i));
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }



    }

    public String getLocalIpAddress(){
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (Exception ex) {
            Log.e("IP Address", ex.toString());
        }
        return null;
    }

    public ArrayList<String> getConnectedDevices(String YourPhoneIPAddress) {
        //ArrayList<InetAddress> ret = new ArrayList<InetAddress>();
        ArrayList<String> ret = new ArrayList<String>();

        LoopCurrentIP = 0;

        String IPAddress = "";
        String[] myIPArray = YourPhoneIPAddress.split("\\.");
        InetAddress currentPingAddr;


        for (int i = 0; i <= 255; i++) {
            try {

                // build the next IP address
                currentPingAddr = InetAddress.getByName(myIPArray[0] + "." +
                        myIPArray[1] + "." +
                        myIPArray[2] + "." +
                        Integer.toString(LoopCurrentIP));
                ad = currentPingAddr.toString();   /////////////////
                Log.d("MyApp",ad);                 //////////////

                // 50ms Timeout for the "ping"
                if (currentPingAddr.isReachable(50)) {

                    //ret.add(currentPingAddr);
                    ad = currentPingAddr.toString();  /////////////////
                    ret.add(ad);
                    Log.d("MyApp",ad);                     //////////////
                }
            } catch (UnknownHostException ex) {
            } catch (IOException ex) {
            }

            LoopCurrentIP++;
        }
        return ret;
    }

    class NetworkSniffTask extends AsyncTask<Void, Void, ArrayList<NetworkInterface>> {

        private static final String TAG = "AsyncTask";
        private ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);

        private WeakReference<Context> mContextRef;

        public NetworkSniffTask(Context context) {
            mContextRef = new WeakReference<Context>(context);
        }

        @Override
        protected ArrayList<NetworkInterface> doInBackground(Void... voids) {
            Log.d(TAG, "Let's sniff the network");
            ArrayList<NetworkInterface>list = new ArrayList<>();

            try {
                Context context = mContextRef.get();
                if (context != null) {

                    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

                    WifiInfo connectionInfo = wm.getConnectionInfo();
                    int ipAddress = connectionInfo.getIpAddress();
                    String ipString = Formatter.formatIpAddress(ipAddress);

                    Log.d(TAG, "activeNetwork: " + String.valueOf(activeNetwork));
                    Log.d(TAG, "ipString: " + String.valueOf(ipString));

                    String prefix = ipString.substring(0, ipString.lastIndexOf(".") + 1);
                    Log.d(TAG, "prefix: " + prefix);

                    InetAddress localhost = InetAddress.getLocalHost();
                    byte []ip = localhost.getAddress();

                    for (int i = 1; i < 255; i++) {
                        /*String testIp = prefix + String.valueOf(i);

                        InetAddress address = InetAddress.getByName(testIp);
                        boolean reachable = address.isReachable(1000);
                        String hostName = address.getCanonicalHostName();

                        Log.i("hallo", i+"");
                        if (reachable)
                            Log.i(TAG, "Host: " + String.valueOf(hostName) + "(" + String.valueOf(testIp) + ") is reachable!");
                            list.add(hostName);
                        if(i == 50){
                            break;
                        }*/

                        try{
                            ip[3] = (byte)i;
                            InetAddress address = InetAddress.getByAddress(ip);
                            if(address.isReachable(10)){
                                //list.add(address);
                            }
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Well that's not good.", t);
            }
            return list;
        }

        @Override
        protected void onPostExecute(ArrayList<NetworkInterface> f) {
            progressDialog.dismiss();
            geraeteListe.addAll(f);
            //Collections.sort(geraeteListe);
            aa.notifyDataSetChanged();
            super.onPostExecute(f);
        }

        @Override
        protected void onPreExecute() {
            progressDialog.setMessage("Suche Geräte im lokalen Netzwerk...");
            progressDialog.show();
        }
    }
}
