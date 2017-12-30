/* 
 * Copyright (C) 2017 Calder Young
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/* global __dirname */

//this is my fist node.js program so don't judge

const WebSocket = require('ws');
const crypto = require('crypto');
const fs = require('fs');
const Socket = require('net').Socket;

function l(text){console.log("[darvm websocket relay] "+text);}

l("starting darvm relay on port 42069...");

var wss = new WebSocket.Server({
    port:42069,
    clientTracking: true
});

function hashpass(pass){
    var hash = crypto.createHash('sha256');
    hash.update("#$%#yeeLer#^#^$^GFD"+pass+"YEE69%#$#@1");
    return hash.digest('hex');
}

l(" - loading password file...");
var passdb = JSON.parse(fs.readFileSync('passwords', 'utf8'));
var passmap = {};

for (var i = 0; i < passdb.length; i++) {
    passmap[passdb[i].username] = hashpass(passdb[i].password);
}

l(" - loading vm registry...");
var vm_list = JSON.parse(fs.readFileSync('vm_list', 'utf8'));

setInterval(function () {
    wss.clients.forEach(function (ws) {
        if(!ws.isAlive){
            l("Closing Broken Pipe: " + ws.remote_addr);
            ws.terminate();
        }else{
            ws.isAlive = false;
            ws.ping('', false, true);
        }
    });
}, 5000);

wss.on('connection', function(ws, req) {
    const remote_addr = req.socket.address().address;
    l("recieving connection from "+remote_addr);
    ws.yee = {}; //state information storage
    ws.yee.remote_addr = remote_addr;
    ws.yee.l = ws_log;
    ws.binaryType = "arraybuffer";
    ws.isAlive = true;
    ws.on('message', SocketMessage);
    ws.on('close', SocketClosed);
    ws.on('pong', heartbeat);
    ws.yee.rdpConnected = false;
    ws.yee.authenticated = false;
    ws.yee.vmlist = null;
    ws.yee.socket = new Socket();
});

function heartbeat() { this.isAlive = true; }

function heartbeat() {
    this.isAlive = true;
}

function ws_log(text){
    l("[connection/"+this.remote_addr+"] "+text);
}

function SocketClosed(){
    this.yee.l("websocket closed");
}

function SocketMessage(message){
    this.yee.l(message);
    try{
        if(this.yee.rdpConnected){
            
        }else{
            var json = JSON.parse(message);
            if(json){
                if(!this.yee.authenticated){
                    if(json.username && json.password && json.password == passmap[json.username] && (this.yee.vmlist = vm_list[json.username])){
                        this.yee.l("Authenticated as '"+json.username+"'");
                        this.yee.authenticated = true;
                        this.yee.user = json.username;
                        var clientvmlist = [];
                        for(var i = 0; i < this.yee.vmlist.length; i++) clientvmlist[i] = this.yee.vmlist[i].name;
                        this.send(JSON.stringify(clientvmlist));
                    }else{
                        this.yee.l("Invalid Auth Packet: "+message);
                        this.close();
                    }
                }else{
                    var vm;
                    if(!(vm = this.yee.vmlist[json.id])) throw 'invalid packet';
                    this.yee.l(this.yee.user+": selecting vm '"+vm.name+"'");
                    var ip_parts = vm.addr.split(":");
                    this.yee.socket.connect(ip_parts[1], ip_parts[0], function(){
                        this.yee.l(this.yee.user+": connected. switching to binary...");
                        this.send('{"connected":true}');
                    });
                }
            }else{
                throw 'invalid json!';
            }
        }
    }catch(yee){
        this.yee.l("ERROR: "+yee);
        this.close();
    }
}

l("yeeeeeee");