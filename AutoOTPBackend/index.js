const express = require('express'),
    http = require('http'),
    app = express(),
    server = http.createServer(app);

var nodemailer = require('nodemailer');

const wsPort = 3000;
const expressPort = 3100;


const WebSocket = require('ws');
const ws = new WebSocket.Server({ port: wsPort }, () => { console.log("Web Socket running at port", wsPort) });
ws.on('connection', function(socket) {
    console.log("Someone Connected to websocket");
    socket.on('close', () => {
        console.log('Someone Disconnected');
    });
});
    
app.get('/', (req, res) => {
    
    console.log(req.query);
    const type = req.query.type;
    if (type == 'email') {
        sendEmail(req.query.dest, req.query.msg, () => {
            res.send(`Sending your email ${req.query.msg} to ${req.query.dest}`);
        });
    } else {
        if (req.query.msg != null && req.query.dest != null) {
            ws.clients.forEach(socket => {
                socket.send(JSON.stringify(req.query));
            });
            res.send(`Sending your sms ${req.query.msg} to ${req.query.dest}`);
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
        cb()
    });
}

server.listen(expressPort, () => {

    console.log('Node app is running on port', expressPort)

});