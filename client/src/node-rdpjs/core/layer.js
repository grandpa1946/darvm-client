/*
 * Copyright (c) 2014-2015 Sylvain Peyrefitte
 *
 * This file is part of node-rdpjs.
 *
 * node-rdpjs is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

var inherits = require('util').inherits;
var type = require('./type');
var log = require('./log');
var events = require('events');

/**
 * Buffer data from socket to present
 * well formed packets
 */
function BufferLayer(websocket) {
	//buffer data
	this.socket = websocket;
        const self = this;
        this.socket.onmessage = function(message){
            log.info("recv: "+new Buffer(message.data));
            self.recv(new Buffer(message.data));
        };
        
	this.buffers = [];
	this.bufferLength = 0;
	//expected size
	this.expectedSize = 0;
}

inherits(BufferLayer, events.EventEmitter);

/**
 * Call from tcp layer
 * @param data tcp stream
 */
BufferLayer.prototype.recv = function(data) {
	this.buffers[this.buffers.length] = data;
	this.bufferLength += data.length;

	while(this.bufferLength >= this.expectedSize) {
		//linear buffer
		var expectedData = new type.Stream(this.expectedSize);

		//create expected data
		while(expectedData.availableLength() > 0) {

			var rest = expectedData.availableLength();
			var buffer = this.buffers.shift();

			if(buffer.length > expectedData.availableLength()) {
				this.buffers.unshift(buffer.slice(rest));
				new type.BinaryString(buffer, { readLength : new type.CallableValue(expectedData.availableLength()) }).write(expectedData);
			}
			else {
				new type.BinaryString(buffer).write(expectedData);
			}
		}

		this.bufferLength -= this.expectedSize;
		expectedData.offset = 0;
		this.emit('data', expectedData);
	}
};

/**
 * Call tcp socket to write stream
 * @param {type.Type} packet
 */
BufferLayer.prototype.send = function(data) {
	var s = new type.Stream(data.size());
	data.write(s);
        log.info("send: "+s.buffer);
	this.socket.send(s.buffer);
};

/**
 * Wait expected size data before call callback function
 * @param {number} expectSize	size expected
 */
BufferLayer.prototype.expect = function(expectedSize) {
	this.expectedSize = expectedSize;
};

/**
 * close stack
 */
BufferLayer.prototype.close = function() {
	this.socket.close();
};

/**
 * Module exports
 */
module.exports = {
	BufferLayer : BufferLayer
};
