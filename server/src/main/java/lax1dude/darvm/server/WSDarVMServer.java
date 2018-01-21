/*
 * Copyright (C) 2018 Calder Young
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
package lax1dude.darvm.server;

import java.awt.event.InputEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import org.apache.commons.lang3.ArrayUtils;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

/**
 *
 * @author calder
 */
public class WSDarVMServer extends WebSocketServer {

    //public static final int DARVM_PACKET_PING = 0;  //ping packet
    //public static final int DARVM_PACKET_PONG = 1;  //pong packet
    public static final int DARVM_PACKET_STREAM_INFORMATION = 2;  //contains screen width and height, sent on connect
    public static final int DARVM_PACKET_SCREEN_UPDATES = 4;  //updates changed pixels on the screen
    public static final int DARVM_PACKET_FULL_SCREEN_UPDATE = 5;  //provides update to entire screen
    public static final int DARVM_PACKET_REQUEST_FULL_SCREEN_UPDATE = 6;  //requests update to entire screen
    public static final int DARVM_PACKET_MOUSE_SET_POSITION = 7;  //mouse position update
    public static final int DARVM_PACKET_MOUSE_LEFT_DOWN = 8;  //left mouse button click
    public static final int DARVM_PACKET_MOUSE_LEFT_UP = 9;  //left mouse button release
    public static final int DARVM_PACKET_MOUSE_MIDDLE_DOWN = 10; //middle mouse button click
    public static final int DARVM_PACKET_MOUSE_MIDDLE_UP = 11; //middle mouse button release
    public static final int DARVM_PACKET_MOUSE_RIGHT_DOWN = 12; //right mouse button release
    public static final int DARVM_PACKET_MOUSE_RIGHT_UP = 13; //right mouse button release
    public static final int DARVM_PACKET_MOUSE_SCROLL_DOWN = 14; //right mouse button release
    public static final int DARVM_PACKET_MOUSE_SCROLL_UP = 15; //right mouse button release
    public static final int DARVM_PACKET_KEYBOARD_KEY_DOWN = 16; //key down
    public static final int DARVM_PACKET_KEYBOARD_KEY_UP = 17; //key up

    public static void main(String[] argile) {
        WSDarVMServer yigg = new WSDarVMServer(new InetSocketAddress(42059));
        yigg.setConnectionLostTimeout(15);
        yigg.setTcpNoDelay(true);
        yigg.start();
    }

    private WSDarVMServer(InetSocketAddress e) {
        super(e);
    }

    private volatile boolean connected = false;
    private volatile InetSocketAddress addr = null;
    private volatile WebSocket client = null;

    public static final DirectRobot robot;
    
    static{
        
        DirectRobot yee = null;
        
        try{
            yee = new DirectRobot();
        }catch(Throwable t){
            //System.err.println(t.getMessage());
        }
        
        robot = yee;
        
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if (!connected) {
            connected = true;
            addr = conn.getRemoteSocketAddress();
            client = conn;
            l("new connection from " + addr.getHostString());
            client.send(ArrayUtils.add(ScreenCapture.getScreenSize(), 0, (byte)DARVM_PACKET_STREAM_INFORMATION));
            l("sent DARVM_PACKET_STREAM_INFORMATION");
            try {
                byte[] updates;
                synchronized(ScreenCapture.class){
                    ScreenCapture.captureScreen();
                    updates = ScreenCapture.getFullUpdate();
                }
                ByteArrayOutputStream s = new ByteArrayOutputStream();
                s.write(DARVM_PACKET_FULL_SCREEN_UPDATE);
                s.write(EncodingUtils.intToBytes(updates.length));
                s.write(updates);
                s.close();
                client.send(s.toByteArray());
                l("sent DARVM_PACKET_FULL_SCREEN_UPDATE");
            } catch (IOException ex) {
                //System.err.println(ex.getMessage());
                conn.close();
                return;
            }
        } else {
            conn.close();
        }
    }

    @Override
    protected boolean onConnect(SelectionKey key) {
        connected = this.connections().size() > 0;
        return !connected;
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if (client != null && client.getRemoteSocketAddress().equals(conn.getRemoteSocketAddress())) {
            l("closed");
            addr = null;
            client = null;
            connected = false;
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        l("got some random text: " + message);
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer buffer) {
        try{
            buffer.rewind();
            byte packettype = buffer.get();
            switch (packettype) {
                case DARVM_PACKET_REQUEST_FULL_SCREEN_UPDATE:
                    l("recieved DARVM_PACKET_REQUEST_FULL_SCREEN_UPDATE");
                    synchronized (ScreenCapture.class) {
                        ScreenCapture.captureScreen();
                        byte[] updates2 = ScreenCapture.getFullUpdate();
                        ByteArrayOutputStream s = new ByteArrayOutputStream();
                        s.write(DARVM_PACKET_FULL_SCREEN_UPDATE);
                        s.write(EncodingUtils.intToBytes(updates2.length));
                        s.write(updates2);
                        s.close();
                        conn.send(s.toByteArray());
                        l("sent DARVM_PACKET_FULL_SCREEN_UPDATE");
                    }
                    break;
                case DARVM_PACKET_MOUSE_SET_POSITION:
                    byte[] x = new byte[4];
                    byte[] y = new byte[4];
                    buffer.get(x);
                    buffer.get(y);
                    int ix = EncodingUtils.bytesToInt(x);
                    int iy = EncodingUtils.bytesToInt(y);
                    l("recieved DARVM_PACKET_MOUSE_SET_POSITION " + ix + " " + iy);
                    robot.mouseMove(ix, iy);
                    break;
                case DARVM_PACKET_MOUSE_LEFT_DOWN:
                    l("recieved DARVM_PACKET_MOUSE_LEFT_DOWN");
                    robot.mousePress(InputEvent.BUTTON1_MASK);
                    break;
                case DARVM_PACKET_MOUSE_LEFT_UP:
                    l("recieved DARVM_PACKET_MOUSE_LEFT_UP");
                    robot.mouseRelease(InputEvent.BUTTON1_MASK);
                    break;
                case DARVM_PACKET_MOUSE_MIDDLE_DOWN:
                    l("recieved DARVM_PACKET_MOUSE_MIDDLE_DOWN");
                    robot.mousePress(InputEvent.BUTTON2_MASK);
                    break;
                case DARVM_PACKET_MOUSE_MIDDLE_UP:
                    l("recieved DARVM_PACKET_MOUSE_MIDDLE_UP");
                    robot.mouseRelease(InputEvent.BUTTON2_MASK);
                    break;
                case DARVM_PACKET_MOUSE_RIGHT_DOWN:
                    l("recieved DARVM_PACKET_MOUSE_RIGHT_DOWN");
                    robot.mousePress(InputEvent.BUTTON3_MASK);
                    break;
                case DARVM_PACKET_MOUSE_RIGHT_UP:
                    l("recieved DARVM_PACKET_MOUSE_RIGHT_UP");
                    robot.mouseRelease(InputEvent.BUTTON3_MASK);
                    break;
                case DARVM_PACKET_MOUSE_SCROLL_DOWN:
                    l("recieved DARVM_PACKET_MOUSE_SCROLL_DOWN");
                    robot.mouseWheel(-1);
                    break;
                case DARVM_PACKET_MOUSE_SCROLL_UP:
                    l("recieved DARVM_PACKET_MOUSE_SCROLL_UP");
                    robot.mouseWheel(1);
                    break;
                case DARVM_PACKET_KEYBOARD_KEY_DOWN:
                    l("recieved DARVM_PACKET_KEYBOARD_KEY_DOWN");
                    byte[] key = new byte[4];
                    buffer.get(key);
                    int key2 = EncodingUtils.bytesToInt(key);
                    robot.keyPress(key2);
                    break;
                case DARVM_PACKET_KEYBOARD_KEY_UP:
                    l("recieved DARVM_PACKET_KEYBOARD_KEY_UP");
                    byte[] key3 = new byte[4];
                    buffer.get(key3);
                    int key4 = EncodingUtils.bytesToInt(key3);
                    robot.keyRelease(key4);
                    break;
                default:
                    break;
            }
        }catch(Exception e){
            ////System.err.println(e.getMessage());
            conn.close();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        l("error!");
        ////System.err.println(ex.getMessage());
        conn.close();
    }

    @Override
    public void onStart() {
        
        Thread framesender = new Thread(() -> {
            while(true){
                try{
                    if(client != null && client.isOpen()){
                        try{
                            synchronized(ScreenCapture.class){
                                ScreenCapture.captureScreen();
                                byte[] updates = ScreenCapture.getUpdatedPixels();
                                if(updates != null){
                                    l("update size: "+updates.length);
                                    client.send(ArrayUtils.add(updates, 0, (byte)DARVM_PACKET_SCREEN_UPDATES));
                                    l("sent DARVM_PACKET_SCREEN_UPDATES");
                                }
                            }
                        }catch(IOException e){
                            //System.err.println(e.getMessage());
                            client.close();
                        }
                    }else{
                        Thread.sleep(1000L);
                    }
                }catch(Throwable t){
                    //System.err.println(t.getMessage());
                }
            }
        }, "framesender");
        framesender.setDaemon(true);
        framesender.start();
        
        l("started darvm");
    }

    public static void l(String t) {
        System.out.println("[darvm server] " + t);
    }
}
