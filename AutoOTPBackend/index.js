const express = require('express'),
    http = require('http'),
    app = express(),
    server = http.createServer(app);

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
    ws.clients.forEach(socket => {
        socket.send(JSON.stringify(req.query));
    });
    res.send("Chat Server is running on port", expressPort)
});


server.listen(expressPort, () => {

    console.log('Node app is running on port', expressPort)

});