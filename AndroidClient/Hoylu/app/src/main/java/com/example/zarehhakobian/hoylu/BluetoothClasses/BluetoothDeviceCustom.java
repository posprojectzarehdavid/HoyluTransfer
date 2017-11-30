package com.example.zarehhakobian.hoylu.BluetoothClasses;

/**
 * Created by David on 29.07.2017.
 */

public class BluetoothDeviceCustom {
    String id, name;
    public String bluetoothAddress;

    public BluetoothDeviceCustom(String id, String name, String bluetoothAddress) {
        this.id = id;
        this.name = name;
        this.bluetoothAddress = bluetoothAddress;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id + ", " + name;
    }
}
