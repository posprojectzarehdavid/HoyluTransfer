package com.example.zarehhakobian.hoylushare.NetworkClasses;

/**
 * Created by Zareh Hakobian on 19.07.2017.
 */

public class NetworkDevice {
    String id, name, publicIP, defaultgateway;

    public NetworkDevice(String id, String name, String publicIP, String defaultgateway) {
        this.id = id;
        this.name = name;
        this.publicIP = publicIP;
        this.defaultgateway = defaultgateway;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id + ", " + name;
    }
}
