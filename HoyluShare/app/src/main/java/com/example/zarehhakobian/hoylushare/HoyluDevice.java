package com.example.zarehhakobian.hoylushare;

/**
 * Created by Zareh Hakobian on 01.08.2017.
 */

public class HoyluDevice {
    String hoyluId, name, btAddress, qrValue, nfcValue, publicIp, defaultGateway, socketId;

    public HoyluDevice(String hoyluId, String name, String btAddress, String qrValue,
                       String nfcValue, String publicIp, String defaultGateway, String socketId) {
        this.hoyluId = hoyluId;
        this.name = name;
        this.btAddress = btAddress;
        this.qrValue = qrValue;
        this.nfcValue = nfcValue;
        this.publicIp = publicIp;
        this.defaultGateway = defaultGateway;
        this.socketId = socketId;
    }

    public String getHoyluId() {
        return hoyluId;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
