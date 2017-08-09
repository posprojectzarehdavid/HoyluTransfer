
'use strict';
var express = require('express');
var uuidv4 = require('uuid/v4');
var multer = require('multer');
var http = require("http");
//var url = require("url");
var fs = require("fs");

var storage = multer.diskStorage({ 
    destination: function (req, file, cb) {
        cb(null, __dirname + '/shared')
    },
    filename: function (req, file, cb) {
        cb(null, uuidv4());
    }
});

var upload = multer({ storage: storage });
var app = express();
var server = require('http').createServer(app);
var io = require('socket.io')(server);
app.use(express.static(__dirname + '/node_modules'));

app.get('/file_for_download/:filename', function (request, response) {
    var filename = request.params.filename;
    console.log(filename.substring(1));
    var file = 'hoylutransfer/socket-app/home/ts/shared/' + filename.substring(1);
    console.log(file.toString());
    response.download(file);
    response.end(file);
});

app.post('/file_upload', upload.any(), function (req, res) {
    console.log(req.files);
    var tmp_path = req.files[0].path;
    var response = null;
    fs.readFile(tmp_path, function (err, data) {
        if (err) {
            console.log(err);
            response = {
                uploaded: false,
                path: null
            }
        } else {
            response = {
                uploaded: true,
                path: req.files[0].filename
            };
        }
        res.end(JSON.stringify(response));
    });
});

var imagePath = null;
var hoyluDevices = new Array();

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

    socket.on('uploadFinished', function (data) {
        imagePath = data.imagePath;
        var id = data.displayId;
        if (imagePath != null) {
            var d = getHoyluDeviceWithId(id);
            if (d != null) {
                console.log(d.socketId+' wird benachrichtigt');
                socket.to(d.socketId).emit('getImage', imagePath);
                global.gc();
            }            
        }     
        imagePath = null;
    });

    socket.on('disconnect', function () {
        console.info('Client with SocketId ' + socket.id + ' disconnected.');
        console.log('--------------------------------------------------------------------');
        for (var d in hoyluDevices) {
            console.log(hoyluDevices[d].Name+', '+hoyluDevices[d].socketId + ', aktuelles socket: ' + socket.id);
            if (hoyluDevices[d].socketId === socket.id) {
                hoyluDevices.splice(hoyluDevices.indexOf(d), 1);
            }
        }
        console.log('--------------------------------------------------------------------');
        console.log('hoyludevices: ' + hoyluDevices.length);
    });
});
server.listen(4200);
