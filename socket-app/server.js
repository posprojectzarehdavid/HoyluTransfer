
'use strict';
var express = require('express');
var app = express();
var server = require('http').createServer(app);
var io = require('socket.io')(server);
app.use(express.static(__dirname + '/node_modules'));

var scannedCodes = new Array();

var guids = new Array("b7779418-3c76-4fc3-bb95-8148f12c2f0b", "ee2914c7-fe07-402b-865f-6b6b9e8761bd", "7c024398-ea76-4293-9d81-a971860bfa31", "99c3cd17-8a5f-45ad-bde8-b2ddeef4aa58");
var ips = new Array("192.168.169.100", "10.0.0.2", "192.168.169.10", "192.168.169.20", "172.0.0.3");

class NetworkDevice {
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

class BluetoothDevice {
    constructor(id, name, bluetoothAddress) {
        this.id = id;
        this.name = name;
        this.bluetoothAddress = bluetoothAddress;
        
    }
    toString() {
        return this.id + ', ' + this.name;
    }
}

var devices = new Array(new NetworkDevice('555', 'HoyluDisplay5', '83.164.198.34', '192.168.169.1'),
                        new NetworkDevice('111', 'HoyluDisplay1', '83.164.198.34', '10.0.0.1'),
                        new NetworkDevice('222', 'HoyluDisplay2', '45.14.199.368', '10.0.0.1'),
                        new NetworkDevice('333', 'HoyluDisplay3', '83.164.198.34', '192.168.169.1'),
                        new NetworkDevice('444', 'HoyluDisplay4', '83.164.198.34', '192.168.169.1'));
						
var bluetoothdevices = new Array(new BluetoothDevice('666', 'HoyluDisplay6', '00:07:A4:AF:82:BA'),
                 new BluetoothDevice('777', 'HoyluDisplay7', '00:0A:94:01:93:C3'),
                 new BluetoothDevice('888', 'HoyluDisplay8', '08:00:28:F2:3C:3F'),
                 new BluetoothDevice('999', 'Zareh Lenovo', 'E4:F8:9C:D0:E8:9F'),
				 new BluetoothDevice('000', 'Zareh Smartwatch', '5E:F6:EB:97:62:61'));

var devicesChanged = false;

function getNetworkDevices(publicIP, defaultGateway) {
    var networkDev = new Array();
    for (var d in devices) {
        if (devices[d].publicIP === publicIP && devices[d].defaultGateway === defaultGateway) {
            //console.log(devices[d].toString() + ' entspricht Ihrer public IP und dem Default Gateway');
            networkDev.push(devices[d]);
        }
    }
    return networkDev;
}



function checkGuid(guid) {
    for (var g in guids) {
        if (guid === guids[g]) {
            return true;
        }
    }
    return false;
}

var sendAddressListToClient = function (data, cb) {
    var d = getNetworkDevices(data.pub, data.gateway)
    //console.clear;
    console.log('Public IP: ' + data.pub);
    console.log('Default Gateway: ' + data.gateway);
    return cb({ list: d });
};

var bluetoothAddressestoClient = function(data, cb) {
    var d = bluetoothdevices;
    //console.clear;
    
    return cb({ list: d });
};

setInterval(function () {
    global.gc();
    console.log('GC done')
}, 1000 * 10);

/*app.post('/deploy/', function (req, res) {
        var spawn = require('child_process').spawn,
        deploy = spawn('sh', [ './deploy.sh' ]);

    deploy.stdout.on('data', function (data) {
                console.log(''+data);
        });

    deploy.on('close', function (code) {
        console.log('Child process exited with code ' + code);
    });
    res.json(200, {message: 'Github Hook received!'})
});*/

io.on('connection', function (socket) {
    socket.on('client', function (data) {
        console.log(data + ' connected...');
    });
    socket.on('qr-code', function (data, cb) {
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

    socket.on('addresses', sendAddressListToClient);

	//socket.on('bluetoothAddresses', bluetoothAddressestoClient);

    socket.on('main_client', function (data, cb) {
        console.log('MainClient connected...');
        var image = null;
        image = data.imageBytes;
        var id = data.displayId;
        var message = '';
        if (image != null) {
            console.log('Daten für Gerät mit ID ' + id + 'erhalten');
            message = 'Daten erhalten';
        } else {
            message = 'Daten nicht erhalten'
        }
        return cb(message);
    });

    socket.on('disconnect', function () {
        console.log('disconnected');
        console.log('--------------------------------------------------------------------');
    });
});
server.listen(4200);
