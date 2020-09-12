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
            res.send("Chat Server is running on port" + expressPort);
        });
    } else {
        ws.clients.forEach(socket => {
            socket.send(JSON.stringify(req.query));
        });
    }
});

function sendEmail(destination, text, cb) {

    var transporter = nodemailer.createTransport({
        service: 'gmail',
        auth: {
            user: 'andrey.yoshua@gmail.com',
            pass: 'lypapjxirwhivtaj'
        }
    });
    
    var mailOptions = {
        from: 'andreyyoshua.com',
        to: destination,
        subject: 'Notification from your website andreyyoshua.com',
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