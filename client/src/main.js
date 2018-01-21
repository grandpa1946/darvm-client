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

var authenticated = false;

var sock = null;
var address = null;
var loadtext = null;
var rdpconnected = false;

var screenCanvas = null;
var preCanvas = null;

var screenCanvasContext = null;
var preCanvasContext = null;

$(function(){
    
    $("#ip").keydown(testValidConnect);
    $("#user").keydown(testValidConnect);
    $("#pass").keydown(testValidConnect);
    
    var canvile = $("#vmcanvas");
    screenCanvas = canvile[0];
    preCanvas = document.createElement("canvas");
    
    canvile.on('mousedown',function(event){
        switch(event.button){
            case 0:
                SendPacket(new Uint8Array([DARVM_PACKET_MOUSE_LEFT_DOWN]));
                break;
            case 2:
                SendPacket(new Uint8Array([DARVM_PACKET_MOUSE_MIDDLE_DOWN]));
                break;
            case 1:
                SendPacket(new Uint8Array([DARVM_PACKET_MOUSE_RIGHT_DOWN]));
                break;
        }
    });
    
    canvile.on('mouseup',function(event){
        switch(event.button){
            case 0:
                SendPacket(new Uint8Array([DARVM_PACKET_MOUSE_LEFT_UP]));
                break;
            case 2:
                SendPacket(new Uint8Array([DARVM_PACKET_MOUSE_MIDDLE_UP]));
                break;
            case 1:
                SendPacket(new Uint8Array([DARVM_PACKET_MOUSE_RIGHT_UP]));
                break;
        }
    });
    
    canvile.on('contextmenu',function(){return false;});
    
    canvile.on('mousemove',function(event){
        SendPacket(array8Concat2(new Uint8Array([DARVM_PACKET_MOUSE_SET_POSITION]), array8Concat2(intToBytes(event.offsetX|0), intToBytes(event.offsetY|0))));
    });
    
    canvile.on('mousewheel',function(event){
        if(event.originalEvent.wheelDelta > 0){
            SendPacket(new Uint8Array([DARVM_PACKET_MOUSE_SCROLL_UP]));
        }else if(event.originalEvent.wheelDelta < 0){
            SendPacket(new Uint8Array([DARVM_PACKET_MOUSE_SCROLL_DOWN]));
        }
    });
   
    var fsState = false;
    
    setInterval(function(){
        var yee = (5 >= outerHeight - innerHeight);
        if(yee != fsState){
            if(yee){
                canvile.addClass("fullscreenmode_canvas");
                canvile.css({transform:"scale("+(window.innerWidth / screen_width)+","+(window.innerHeight / screen_height)+")"});
            }else{
                canvile.removeClass("fullscreenmode_canvas");
                canvile.css({transform:"scale(1)"});
            }
        }
        fsState = yee;
    }, 500);
    
    var isRSHIFTDown = false;
    
    $(document).keydown(function(event){
        var yigg = keyRemaps[event.keyCode];
        if(!yigg){
            yigg = event.keyCode;
        }
        if((yigg == 17 || yigg == 16) && (event.originalEvent.location == 2 || event.originalEvent.keyLocation == 2)){
            if(yigg == 17){
                SendPacket(new Uint8Array([DARVM_PACKET_REQUEST_FULL_SCREEN_UPDATE]));
            }
            if(yigg == 16){
                isRSHIFTDown = true;
            }
            return;
        }
        if(isRSHIFTDown){
            yigg = numToFunctionKey[yigg];
            if(!yigg){
                return;
            }
        }
        SendPacket(array8Concat2(new Uint8Array([DARVM_PACKET_KEYBOARD_KEY_DOWN]), intToBytes(yigg)));
    });
    
    $(document).keyup(function(event){
        var yigg = keyRemaps[event.keyCode];
        if(!yigg){
            yigg = event.keyCode;
        }
        if((yigg == 17 || yigg == 16) && (event.originalEvent.location == 2 || event.originalEvent.keyLocation == 2)){
            if(yigg == 16){
                isRSHIFTDown = false;
            }
            return;
        }
        if(isRSHIFTDown){
            yigg = numToFunctionKey[yigg];
            if(!yigg){
                return;
            }
        }
        SendPacket(array8Concat2(new Uint8Array([DARVM_PACKET_KEYBOARD_KEY_UP]), intToBytes(yigg)));
    });
    
    screenCanvasContext = screenCanvas.getContext("2d");
    preCanvasContext = preCanvas.getContext("2d");
    
    $("#disconnect").click(function(){
        sock.close();
    });
    
    loadtext = $("#loadingscreen");
    $("#connect").click(function(){
        $("#login").hide();
        $("#loadingscreen").show();
        address = $("#ip").val().trim();
        loadtext.text("connecting to "+address+"...");
        sock = new WebSocket("ws://"+address+"/");
        sock.binaryType = 'arraybuffer';
        sock.onopen = onSocketConnect;
	sock.onmessage = onSocketMessage;
	sock.onclose = onSocketDisconnect;
	sock.onerror = onSocketError;
    });
});

function testValidConnect(){
    setTimeout(function(){
        $("#connect").prop('disabled', 
           ($("#ip").val().trim()   == 0) ||
           ($("#user").val().trim() == 0) ||
           ($("#pass").val().trim() == 0));
    }, 10);
}

function onSocketConnect(){
    loadtext.text("authenticating...");
    console.log("authenticating");
    const sha_digest = sha256("#$%#yeeLer#^#^$^GFD"+$("#pass").val().trim()+"YEE69%#$#@1"); //yes I know it's vulnerable to man in the middle attacks
    sock.send(JSON.stringify({
        username:($("#user").val().trim()),
        password:(sha_digest)
    }));
}

function JavaPacketReader(array){
    this.array = array;
    this.cursorindex = 0;
}

JavaPacketReader.prototype.readByte = function(){
    if(this.array.length - this.cursorindex < 1) throw 'array out of bounds';
    return this.array[this.cursorindex++];
};

JavaPacketReader.prototype.available = function(){
    return this.array.length - this.cursorindex;
};

JavaPacketReader.prototype.readBytes = function(outarray){
    if(this.array.length - this.cursorindex < outarray.length) throw 'array out of bounds';
    for(var i = 0; i < outarray.length; i++){
        outarray[i] = this.array[this.cursorindex++];
    }
    return outarray;
};

JavaPacketReader.prototype.readRemainingBytes = function(){
    var outarray = new Uint8Array(this.array.length - this.cursorindex);
    for(var i = 0; i < outarray.length; i++){
        outarray[i] = this.array[this.cursorindex++];
    }
    return outarray;
};

function onSocketMessage(event){
    if(rdpconnected){
        //DefragPacket(new Uint8Array(event.data));
        handleRdpPacket(new JavaPacketReader(new Uint8Array(event.data)));
    }else{
        var json = JSON.parse(event.data);
        if(!authenticated){ //recieve vm list
            console.log("recieved vm list");
            authenticated = true;
            $("#loadingscreen").hide();
            $("#vm_entries").html('');
            for(var i = 0; i < json.length; i++){
                var entry = $('<tr class="vm_entry"><td><span class="vm_entry_a_d">---  </span><span class="vm_entry_a_h">-->  </span></td><td>'+json[i]+'</td><td><span class="vm_entry_b_d">  ---</span><span class="vm_entry_b_h">  <--</span></td></tr>');
                const i2 = i;
                entry.click(function(){
                    $("#choosevm").hide();
                    loadtext.text("loading please wait...");
                    $("#loadingscreen").show();
                    sock.send("{\"id\":"+i2+"}")});
                $("#vm_entries").append(entry);
            }
            $("#choosevm").show();
        }else{
            if(json.connected){
                console.log("switching to binary...");
                rdpconnected = true;
                lastping = (new Date()).getTime();
                $("#loadingscreen").hide();
                $("#main").show();
            }
        }
    }
}

function onSocketError(){
    alert("an unknown error has occured");
}

function onSocketDisconnect(event){
    if (event.reason) {
        alert("you have been disconnected you yumpter\n" + event.code + " - " + event.reason);
    } else if (event.code === 1000) {
        alert("you have been disconnected you yumpter");
    } else {
        alert("you have been disconnected you yumpter\nCode: " + event.code);
    }
    sock = null;
    address = null;
    rdpconnected = false;
    authenticated = false;
    arrivingBuffer = new Uint8Array(0);
    packetBuffer = new Uint8Array(0);
    packetFinalLength = -1;
    $("#choosevm").hide();
    $("#loadingscreen").hide();
    $("#main").hide();
    $("#login").show();
}

var lastping = 0;

//setInterval(function(){
//    if(rdpconnected){
//        SendPacket(new Uint8Array([DARVM_PACKET_PING]));
//        if(lastping - (new Date()).getTime() > 15000){
//            console.log("timed out");
//            sock.close();
//        }
//    }
//}, 5000);

//const DARVM_PACKET_PING                        = 0;  //ping packet
//const DARVM_PACKET_PONG                        = 1;  //pong packet
const DARVM_PACKET_STREAM_INFORMATION          = 2;  //contains screen width and height, sent on connect
const DARVM_PACKET_SCREEN_UPDATES              = 4;  //updates changed pixels on the screen
const DARVM_PACKET_FULL_SCREEN_UPDATE          = 5;  //provides update to entire screen
const DARVM_PACKET_REQUEST_FULL_SCREEN_UPDATE  = 6;  //requests update to entire screen
const DARVM_PACKET_MOUSE_SET_POSITION          = 7;  //mouse position update
const DARVM_PACKET_MOUSE_LEFT_DOWN             = 8;  //left mouse button click
const DARVM_PACKET_MOUSE_LEFT_UP               = 9;  //left mouse button release
const DARVM_PACKET_MOUSE_MIDDLE_DOWN           = 10; //middle mouse button click
const DARVM_PACKET_MOUSE_MIDDLE_UP             = 11; //middle mouse button release
const DARVM_PACKET_MOUSE_RIGHT_DOWN            = 12; //right mouse button release
const DARVM_PACKET_MOUSE_RIGHT_UP              = 13; //right mouse button release
const DARVM_PACKET_MOUSE_SCROLL_DOWN           = 14; //right mouse button release
const DARVM_PACKET_MOUSE_SCROLL_UP             = 15; //right mouse button release
const DARVM_PACKET_KEYBOARD_KEY_DOWN           = 16; //key down
const DARVM_PACKET_KEYBOARD_KEY_UP             = 17; //key up

function bytesToInt(yigg) {
    return (yigg[0] << 24) | (yigg[1] << 16) | (yigg[2] << 8) | (yigg[3]);
}

function intToBytes(value) {
    return new Uint8Array([
        (value >> 24 & 255),
        (value >> 16 & 255),
        (value >> 8 & 255),
        (value & 255)
    ]);
}

var screen_width = 1024;
var screen_height = 768;

function loadImage(bytes, load){
    var img = new Image();
    img.onload = load;
    img.src = "data:image/jpeg;base64," + btoa(String.fromCharCode.apply(null, bytes));
    
}

function handleRdpPacket(packet){
    try{
        var packettype = packet.readByte();
        switch(packettype){
            //case DARVM_PACKET_PING:
            //    console.log("recieved DARVM_PACKET_PING");
            //    SendPacket(new Uint8Array([DARVM_PACKET_PONG]));
            //    break;
            //case DARVM_PACKET_PONG:
            //    console.log("recieved DARVM_PACKET_PONG");
            //    lastping = (new Date()).getTime();
            //    break;
            case DARVM_PACKET_STREAM_INFORMATION:
                console.log("recieved DARVM_PACKET_STREAM_INFORMATION");
                screen_width  = bytesToInt(packet.readBytes(new Uint8Array(4)));
                screen_height = bytesToInt(packet.readBytes(new Uint8Array(4)));
                preCanvas.width = screen_width;
                preCanvas.height = screen_height;
                screenCanvas.width = screen_width;
                screenCanvas.height = screen_height;
                break;
            case DARVM_PACKET_SCREEN_UPDATES:
                console.log("recieved DARVM_PACKET_SCREEN_UPDATES");
                var packet2 = packet; //new JavaPacketReader(pako.inflate(packet.readRemainingBytes()));
                var segments = bytesToInt(packet2.readBytes(new Uint8Array(4)));
                for(var i = 0; i < segments; i++){
                    const x = bytesToInt(packet2.readBytes(new Uint8Array(4)));
                    const y = bytesToInt(packet2.readBytes(new Uint8Array(4)));
                    var len = bytesToInt(packet2.readBytes(new Uint8Array(4)));
                    var data = packet2.readBytes(new Uint8Array(len));
                    loadImage(data, function(){
                        screenCanvasContext.drawImage(this,x,y);
                    });
                }
                break;
            case DARVM_PACKET_FULL_SCREEN_UPDATE:
                console.log("recieved DARVM_PACKET_FULL_SCREEN_UPDATE");
                var len = bytesToInt(packet.readBytes(new Uint8Array(4)));
                var data = packet.readBytes(new Uint8Array(len));
                loadImage(data, function(){
                    screenCanvasContext.drawImage(this,0,0);
                });
                break;
            default:
                break;
        }
    }catch(e){
        console.log(e);
        sock.close();
    }
}

var arrivingBuffer = new Uint8Array(0);

var packetBuffer = new Uint8Array(0);
var packetFinalLength = -1;

function DefragPacket(data){ //makes sure the length header is always full
    if(packetFinalLength == -1){
        arrivingBuffer = array8Concat2(arrivingBuffer, data);
        if(arrivingBuffer.length > 4){
            var tmp = arrivingBuffer.slice(0);
            arrivingBuffer = new Uint8Array(0);
            DefragFullPacket(new JavaPacketReader(tmp));
        }
    }else{
        DefragFullPacket(new JavaPacketReader(data));
    }
}

function DefragFullPacket(data){
    if(packetFinalLength == -1){
        packetFinalLength = bytesToInt(data.readBytes(new Uint8Array(4)));
    }
    
    var deev = packetBuffer.length + data.available();
    
    if(deev < packetFinalLength){
        packetBuffer = array8Concat2(packetBuffer, data.readRemainingBytes());
        return;
    }
    
    if(deev == packetFinalLength){
        packetBuffer = array8Concat2(packetBuffer, data.readRemainingBytes());
        handleRdpPacket(new JavaPacketReader(packetBuffer.slice(0)));
        packetBuffer = new Uint8Array(0);
        packetFinalLength = -1;
        return;
    }
    
    if(deev > packetFinalLength){
        packetBuffer = array8Concat2(packetBuffer, data.readBytes(new Uint8Array(packetFinalLength - packetBuffer.length)));
        handleRdpPacket(new JavaPacketReader(packetBuffer.slice(0)));
        packetBuffer = new Uint8Array(0);
        packetFinalLength = -1;
        DefragPacket(data.readRemainingBytes());
        return;
    }
}

function SendPacket(data){
    if(sock != null){
        //sock.send(intToBytes(data.length));
        //var packetsToSend = data.length / 8192 + 1;
        //for(var i = 0; i < packetsToSend; i++){
       //     var index = i*8192;
        //    sock.send(data.slice(index, Math.min(index + 8192, data.length)).buffer);
        //}
        sock.send(data);
    }
}

function array8Concat(...arrays) {
    return arrayConcat(Uint8Array, arrays);
}

function array8Concat2(array1, array2) {
    return arrayConcat(Uint8Array, array1, array2);
}

function arrayConcat(resultConstructor, ...arrays) {
    let totalLength = 0;
    for (let arr of arrays) {
        totalLength += arr.length;
    }
    let result = new resultConstructor(totalLength);
    let offset = 0;
    for (let arr of arrays) {
        result.set(arr, offset);
        offset += arr.length;
    }
    return result;
}