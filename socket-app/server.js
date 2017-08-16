
'use strict';
var express = require('express');
var uuidv4 = require('uuid/v4');
var multer = require('multer');
var http = require("http");
//var url = require("url");
var fs = require("fs");

var storage = multer.diskStorage({ 
    destination: function (req, file, cb) {
        cb(null, '../../shared');
    },
    filename: function (req, file, cb) {
        cb(null, uuidv4());
    }
});

var upload = multer({ storage: storage });
var app = express();
var server = require('http').createServer(app);
var io = require('socket.io')(server);
var file;
var duplicates;
app.use(express.static(__dirname + '/node_modules'));

app.get('/file_for_download/:filename', function (request, response) {
    var filename = request.params.filename;
    console.log(filename.substring(1));
    file = '/home/ts/shared/' + filename.substring(1);
    console.log(file.toString());
    response.download(file);
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
                filename: null,
                originalName: null
            };
        } else {
            response = {
                uploaded: true,
                filename: req.files[0].filename,
                originalName: req.files[0].originalname
            };
        }
        res.end(JSON.stringify(response));
    });
});

var filename = null;
var originalname = null;
var hoyluDevices = new Array();
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
    var d = getNetworkClients(data.pub, data.gateway);
    //console.log('Public IP: ' + data.pub);
    //console.log('Default Gateway: ' + data.gateway);
    return cb({ list: d });
};

var sendBluetoothMatchesToClient = function(data, cb) {
    var d = new Array();
    for (var device in hoyluDevices) {
        if (hoyluDevices[d].btAddress !== null) {
            d.push(hoyluDevices[device]);
        }
    }
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
        console.log(hoyluDevices[d].name + ', ' + hoyluDevices[d].socketId + ', hoyluid: ' + hoyluDevices[d].id);
    }
}

function showConnectedClients() {
    for (var c in connectedClients) {
        console.log('io.sockets: ' + connectedClients[c].id);
    }
}

setInterval(showConnectedClients, 1000 * 5);
setInterval(showHoyluDevices, 1000 * 5);

io.on('connection', function (socket) {
    
    /*if (connectedClients.length == 0) {
        connectedClients.push(socket);
    } else {
        for (var c in connectedClients) {
            if (connectedClients[c].id == socket.id) {
                socket.disconnect();
            } else {
                connectedClients.push(socket);
            }
        }
    }*/

       
    socket.once('client', function (data) {
        console.log(data + ' with SocketId ' + socket.id + ' connected...');
       

        /*if (connectedClients.length == 0) {
            connectedClients.push(socket);
        } else {
            if (connectedClients.indexOf(socket) != -1) {
                socket.disconnect();
            } else {
                connectedClients.push(socket);
            }
        }*/
        //console.log(data + ' with SocketId ' + socket.id + ' connected...');
        console.log('--------------------------------------------------------------------');
        showHoyluDevices();
        console.log('--------------------------------------------------------------------');
        //showConnectedClients();
    });

    socket.once('windowsClient', function (data) {
        console.log('WindowsClient with SocketId ' + socket.id + ' connected...');
        var v = false;
        for (var c in connectedClients) {
            if (connectedClients[c].id === socket.id) {
                v = true;
            }
        }
        if (v === false) {
            connectedClients.push(socket);
        } else {
            duplicates = true;
            console.log(socket.id + ' schon vorhanden');
            socket.disconnect();
        }

        var object = JSON.parse(data);
        var vorhanden = false;
        var hoylu = new HoyluDevice(object.HoyluId, object.Name, object.BluetoothAddress, object.QrValue, object.NfcValue, object.PublicIp, object.DefaultGateway, socket.id);
        for (var h in hoyluDevices) {
            if (hoyluDevices[h].hoyluId === hoylu.hoyluId) {
                vorhanden = true;
            }
        }
        if (vorhanden === false) {
            hoyluDevices.push(hoylu);
            console.log(hoylu.name + ' hinzugefügt');
            socket.emit('device_registered');
        }
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

    socket.on('bluetoothAddresses', sendBluetoothMatchesToClient);

    socket.on('uploadFinished', function (data,cb) {
        filename = data.filename;
        originalname = data.originalName;
        var id = data.displayId;
        var receiverBenachrichtigt;
        if (filename !== null) {
            console.log('filename: '+filename);
            var d = getHoyluDeviceWithId(id);
            if (d !== null) {
                //showConnectedClients();
                console.log(d.socketId + ' wird benachrichtigt');
                socket.to(d.socketId).emit('getImage', { Filename: filename, Originalname: originalname });
                receiverBenachrichtigt = true;
            } else {
                receiverBenachrichtigt = false;
            }
        }
        return cb(receiverBenachrichtigt);
    });

    socket.on("imageReceived", function () {
        fs.unlinkSync(file);
        console.log(file + ' gelöscht');
        file = null;
    });

    socket.on('disconnect', function () {
        console.info('Client with SocketId ' + socket.id + ' disconnected.');
        var counter = 0;
        showConnectedClients();
        /*if (duplicates) {
            for (var c in connectedClients) {
                if (connectedClients[c].id == socket.id) {
                    counter++;
                }
            }
        }
        duplicates = false;
        console.log('anzahl duplicates: ' + counter);
        for (var c in connectedClients) {
            if (connectedClients[c].id == socket.id) {
                console.log('connectedclient with id ' + connectedClients[c].id + ' removed');
                connectedClients.splice(connectedClients.indexOf(socket), 1);
                showConnectedClients();
            }
        }*/
        
        console.log('--------------------------------------------------------------------');
        showHoyluDevices();
        for (var d in hoyluDevices) {
            if (hoyluDevices[d].socketId == socket.id) {
                console.log('hoyludevice with socketid ' + hoyluDevices[d].socketId + ' removed');
                hoyluDevices.splice(hoyluDevices.indexOf(hoyluDevices[d]), 1);
                showHoyluDevices();
            }
        }
        console.log('--------------------------------------------------------------------');
        console.log('hoyludevices: ' + hoyluDevices.length +' connectedClients: '+connectedClients.length);
    });
});
server.listen(4200);
