package com.example.zarehhakobian.hoylu;

import org.json.JSONException;
import org.json.JSONObject;

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

    public String getName() {
        return Name;
    }

    public String getBluetoothAddress() {
        return BluetoothAddress;
    }

    public String getQrValue() {
        return QrValue;
    }

    public String getNfcValue() {
        return NfcValue;
    }

    public String getPublicIp() {
        return PublicIp;
    }

    public String getDefaultGateway() {
        return DefaultGateway;
    }

    @Override
    public String toString() {
        return this.Name;
    }

    public String toJson(){
        JSONObject jsonObject = new JSONObject();
        try{
            jsonObject.put("Name", getName());
            jsonObject.put("HoyluId", getHoyluId());
            jsonObject.put("BluetoothAddress", getBluetoothAddress());
            jsonObject.put("QrValue", getQrValue());
            jsonObject.put("NfcValue",getNfcValue());
            jsonObject.put("PublicIp", getPublicIp());
            jsonObject.put("DefaultGateway", getDefaultGateway());
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }
}
