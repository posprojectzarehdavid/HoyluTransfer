package com.example.zarehhakobian.hoylushare;

/**
 * Created by Zareh Hakobian on 01.08.2017.
 */

public class HoyluDevice {
    String hoyluId, name, btAddress, qrValue, nfcValue, publicIp, defaultGateway;

    public HoyluDevice(String hoyluId, String name, String btAddress, String qrValue,
                       String nfcValue, String publicIp, String defaultGateway) {
        this.hoyluId = hoyluId;
        this.name = name;
        this.btAddress = btAddress;
        this.qrValue = qrValue;
        this.nfcValue = nfcValue;
        this.publicIp = publicIp;
        this.defaultGateway = defaultGateway;
    }

    public String getHoyluId() {
        return hoyluId;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
