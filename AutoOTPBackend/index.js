const express = require('express'),
    http = require('http'),
    app = express(),
    server = http.createServer(app);

var nodemailer = require('nodemailer');

const wsPort = 3000;
const expressPort = 3100;

var connectedSocket;
const WebSocket = require('ws');
const ws = new WebSocket.Server({ port: wsPort }, () => { console.log("Web Socket running at port", wsPort) });
ws.on('connection', function(socket) {
    console.log("Someone Connected to websocket", ws.clients.size);
    connectedSocket = socket;
    socket.on('close', () => {
        console.log('Someone Disconnected', ws.clients.size);
        connectedSocket = null;
    });
});
    
app.get('/', (req, res) => {
    
    console.log(req.query);
    const type = req.query.type;
    if (type == 'email') {
        sendEmail(req.query.dest, req.query.msg, (error, info) => {
            if (error) {
                res.status(500).json({
                    error: error,
                    info: info,
                    desc: `Failed sending your email ${req.query.msg} to ${req.query.dest}`
                });
            } else {
                res.status(200).json({
                    error: error,
                    info: info,
                    desc: `Send your email ${req.query.msg} to ${req.query.dest}`
                });
            }
        });
    } else {
        if (req.query.msg != null && req.query.dest != null) {
            if (connectedSocket == null) {

                res.status(500).json({
                    error: "No device connected",
                    info: "Please connect an android",
                    desc: `Failed sending your email ${req.query.msg} to ${req.query.dest}`
                });
            } else {
                connectedSocket.on('message', function incoming(data) {
                    const dataJson = JSON.parse(data);
                    res.status(dataJson.code).json({
                        error: dataJson.code == 200 ? null : dataJson.message,
                        info: dataJson.message,
                        desc: dataJson.message
                    });
                });
                connectedSocket.send(JSON.stringify(req.query));
            }
        }
    }
});

function sendEmail(destination, text, cb) {

    var transporter = nodemailer.createTransport({
        service: 'gmail',
        auth: {
            user: 'developer.waterhub@gmail.com',
            pass: 'waterhubid1234'
        }
    });
    
    var mailOptions = {
        from: 'Waterhub.id',
        to: destination,
        subject: 'do-not-reply waterhub',
        text: text
    };
    
    transporter.sendMail(mailOptions, function(error, info){
        if (error) {
            console.log(error);
        } else {
            console.log('Email sent: ' + info.response);
        }
        cb(error, info);
    });
}

server.listen(expressPort, () => {

    console.log('Node app is running on port', expressPort)

});