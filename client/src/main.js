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

var crypto = require('crypto');
var rdp = require('./node-rdpjs');
var $   = require('jquery');

var authenticated = false;

var sock = null;
var address = null;
var loadtext = null;
var rdpconnected = false;

$(function(){
    
    $("#ip").keydown(testValidConnect);
    $("#user").keydown(testValidConnect);
    $("#pass").keydown(testValidConnect);
    
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
    var hash = crypto.createHash('sha256');
    hash.update("#$%#yeeLer#^#^$^GFD"+$("#pass").val().trim()+"YEE69%#$#@1"); //yes I know it's vulnerable to man in the middle attacks
    sock.send(JSON.stringify({
        username:($("#user").val().trim()),
        password:(hash.digest('hex'))
    }));
}

function onSocketMessage(event){
    if(rdpconnected){
        
    }else{
        var json = JSON.parse(event.data);
        if(!authenticated){ //recieve vm list
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
                rdpconnected = true;
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
    $("#choosevm").hide();
    $("#loadingscreen").hide();
    $("#main").hide();
    $("#login").show();
}