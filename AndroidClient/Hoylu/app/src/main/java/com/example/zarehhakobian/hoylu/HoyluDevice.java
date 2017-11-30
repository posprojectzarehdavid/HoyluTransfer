package com.example.zarehhakobian.hoylu;

/**
 * Created by Zareh Hakobian on 01.08.2017.
 */

public class HoyluDevice {
    String HoyluId, Name, BluetoothAddress, QrValue, NfcValue, PublicIp, DefaultGateway, SocketId;

    public HoyluDevice(String hoyluId, String name, String bluetoothAddress, String qrValue,
                       String nfcValue, String publicIp, String defaultGateway, String socketId) {
        this.HoyluId = hoyluId;
        this.Name = name;
        this.BluetoothAddress = bluetoothAddress;
        this.QrValue = qrValue;
        this.NfcValue = nfcValue;
        this.PublicIp = publicIp;
        this.DefaultGateway = defaultGateway;
        this.SocketId = socketId;
    }

    public String getHoyluId() {
        return HoyluId;
    }

    @Override
    public String toString() {
        return this.Name;
    }
}
