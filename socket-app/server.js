
'use strict';
var express = require('express');
var app = express();
var server = require('http').createServer(app);
var io = require('socket.io')(server);
app.use(express.static(__dirname + '/node_modules'));
require('tls').SLAB_BUFFER_SIZE = 100 * 1024;

var image = null;
var connectedClients = new Array();

class HoyluDevice {
    constructor(hoyluId, name, btAddress, qrValue, nfcValue, publicIp, defaultGateway, socketId) {
        this.hoyluId = hoyluId;
        this.name = name;
        this.btAddress = btAddress;
        this.qrValue = qrValue;
        this.nfcValue = nfcValue;
        this.publicIp = publicIp;
        this.defaultGateway = defaultGateway;
        this.socketId = socketId;
    }
    toString() {
        return this.name;
    }
}

class NetworkClient {
    constructor(id, name, publicIP, defaultGateway) {
        this.id = id;
        this.name = name;
        this.publicIP = publicIP;
        this.defaultGateway = defaultGateway;
    }
    toString() {
        return this.id + ', ' + this.name;
    }
}

class BluetoothClient {
    constructor(id, name, bluetoothAddress) {
        this.id = id;
        this.name = name;
        this.bluetoothAddress = bluetoothAddress;
        
    }
    toString() {
        return this.id + ', ' + this.name;
    }
}

var hoyluDevices2 = new Array(new HoyluDevice('b7779418-3c76-4fc3-bb95-8148f12c2f0b', 'HoyluDisplay1', '00:07:A4:AF:82:BA', 'qr_111', 'nfc_111', '83.164.198.34', '192.168.169.1', 'I-086nX50zFy1WcnAAAK'),
                             new HoyluDevice('ee2914c7-fe07-402b-865f-6b6b9e8761bd', 'HoyluDisplay2', '00:0A:94:01:93:C3', 'qr_222', 'nfc_222', '83.164.198.34', '10.0.0.1', 'I-086nX50zFy1WcnBBBK'),
                             new HoyluDevice('7c024398-ea76-4293-9d81-a971860bfa31', 'HoyluDisplay3', '08:00:28:F2:3C:3F', 'qr_333', 'nfc_333', '45.14.199.368', '10.0.0.1', 'I-086nX50zFy1WcnDDDK'),
                             new HoyluDevice('99c3cd17-8a5f-45ad-bde8-b2ddeef4aa58', 'HoyluDisplay4', 'E4:F8:9C:D0:E8:9F', 'qr_444', 'nfc_444', '83.164.198.34', '192.168.169.1', 'I-086nX50zFy1WcnFFFK'),
                             new HoyluDevice('b7779418-3c76-4fc3-bb95-8148f12c2f0b', 'HoyluDisplay5', '5E:F6:EB:97:62:61', 'qr_555', 'nfc_555', '83.164.198.34', '192.168.169.1', 'I-086nX50zFy1WcnEEEK'));
				
var hoyluDevices = new Array();

var bluetoothClients  = new Array(new BluetoothClient('666', 'HoyluDisplay6', '00:07:A4:AF:82:BA'),
                 new BluetoothClient('777', 'HoyluDisplay7', '00:0A:94:01:93:C3'),
                 new BluetoothClient('888', 'HoyluDisplay8', '08:00:28:F2:3C:3F'),
                 new BluetoothClient('999', 'Zareh Lenovo', 'E4:F8:9C:D0:E8:9F'),
				 new BluetoothClient('000', 'Zareh Smartwatch', '5E:F6:EB:97:62:61'));

function getNetworkClients(publicIP, defaultGateway) {
    var networkDev = new Array();
    for (var d in hoyluDevices) {
        if (hoyluDevices[d].publicIp === publicIP && hoyluDevices[d].defaultGateway === defaultGateway) {
            networkDev.push(hoyluDevices[d]);
        }
    }
    return networkDev;
}

function checkGuid(qrValue) {
    for (var d in hoyluDevices) {
        if (hoyluDevices[d].qrValue === qrValue) {
            return true;
        }
    }
    return false;
}

var sendNetworkMatchesToClient = function (data, cb) {
    var d = getNetworkClients(data.pub, data.gateway)
    //console.clear;
    console.log('Public IP: ' + data.pub);
    console.log('Default Gateway: ' + data.gateway);
    return cb({ list: d });
};

var sendBluetoothMatchesToClient = function(data, cb) {
    var d = bluetoothClients ;
    //console.clear;
    
    return cb({ list: d });
};

function getHoyluDeviceWithId(id) {
    for (var d in hoyluDevices) {
        if (hoyluDevices[d].hoyluId === id) {
            return hoyluDevices[d];
        }
    }
    return null;
}

var garcol = function () {
    global.gc();
};

function showHoyluDevices() {
    for (var d in hoyluDevices) {
        console.log(hoyluDevices[d].name + ', ' + hoyluDevices[d].defaultGateway + ', ' + hoyluDevices[d].btAddress);
    }
};

setInterval(garcol, 1000 * 5);
setInterval(showHoyluDevices, 1000 * 5);

io.on('connection', function (socket) {
    connectedClients.push(socket);
    socket.on('client', function (data) {
        console.log(data + ' with SocketId ' + socket.id + ' connected...');
    });

    socket.on('device_properties', function (data) {
        var object = JSON.parse(data);
        hoyluDevices.push(new HoyluDevice(object.HoyluId, object.Name, object.BluetoothAddress, object.QrValue, object.NfcValue, object.PublicIp, object.DefaultGateway, socket.id));
    });

    socket.on('qr_code', function (data, cb) {
        var checkMessage;
        if (checkGuid(data)) {
            checkMessage = true;
            console.log(data + ' ist gültig');
        } else {
            checkMessage = false;
            console.log(data + ' ist ungültig');
        }
        return cb(checkMessage);
    });

    socket.on('addresses', sendNetworkMatchesToClient);

	//socket.on('bluetoothAddresses', sendBluetoothMatchesToClient);

    socket.on('main_client', function (data, cb) {
        console.log('MainClient with SocketId '+socket.id+' connected...');
        image = data.imagePart;
        var id = data.displayId;
        var last = data.last;
        var message = '';
        if (image != null) {
            var d = getHoyluDeviceWithId(id);
            if (d != null) {
                socket.to(d.socketId).emit('receiveImage', image);
            }
            else {
                message = 'Gerät nicht gefunden';
            }
            
        } else {
            message = 'Daten nicht erhalten'
        }
        if (last) {
            console.log('Daten für Gerät mit ID ' + id + 'erhalten');
            message = 'Daten erhalten';
            //socket.emit('sendChecksum');
            socket.to(d.socketId).emit('receiveChecksum', data.check);
        }
        image = null;
        return cb(message);
    });

    socket.on('checksum', function (data) {
        var d = getHoyluDeviceWithId(data.id);
        if (d != null) {
            socket.to(d.socketId).emit('receiveChecksum', data.check);
        }
        
    });

    socket.on('disconnect', function () {
        for (var d in connectedClients) {
            console.log(connectedClients[d].Name + ', ' + connectedClients.indexOf(d));
        }

        for (var d in hoyluDevices) {
            console.log(hoyluDevices[d].socketId + ', ' + socket.id);
            if (hoyluDevices[d].socketId === socket.id) {
                hoyluDevices.splice(hoyluDevices.indexOf(d), 1);
            }
        }

        var index = connectedClients.indexOf(socket);
 
        if (index != -1) {
            connectedClients.splice(index, 1);
            console.info('Client with SocketId ' + socket.id + ' disconnected.');
        }
        console.log('--------------------------------------------------------------------');
    });
});
server.listen(4200);
