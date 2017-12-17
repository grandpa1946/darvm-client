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
var fs = require('fs');
var type = require('./type');
var log = require('./log');
var events = require('events');

/**
 * Buffer data from socket to present
 * well formed packets
 */
function BufferLayer(socket) {
    
}

inherits(BufferLayer, events.EventEmitter);

/**
 * Call from tcp layer
 * @param data tcp stream
 */
BufferLayer.prototype.recv = function(data) {
    
};

/**
 * Call tcp socket to write stream
 * @param {type.Type} packet
 */
BufferLayer.prototype.send = function(data) {
    
};

/**
 * Wait expected size data before call callback function
 * @param {number} expectSize	size expected
 */
BufferLayer.prototype.expect = function(expectedSize) {
    
};

/**
 * Convert connection to TLS connection
 * Use nodejs starttls module
 * @param callback {func} when connection is done
 */
BufferLayer.prototype.startTLS = function(callback) {
    
};

/**
 * Convert connection to TLS server
 * @param keyFilePath	{string} key file path
 * @param crtFilePath	{string} certificat file path
 * @param callback	{function}
 */
BufferLayer.prototype.listenTLS = function(keyFilePath, crtFilePath, callback) {
    
};

/**
 * close stack
 */
BufferLayer.prototype.close = function() {
    
};

/**
 * Module exports
 */
module.exports = {
	BufferLayer : BufferLayer
};
