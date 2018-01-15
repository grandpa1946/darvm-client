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

import java.awt.Robot;
import java.awt.event.InputEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author calder
 */
public class DarVMServer {
    
    public static final int DARVM_PACKET_PING                        = 0;  //ping packet
    public static final int DARVM_PACKET_PONG                        = 1;  //pong packet
    public static final int DARVM_PACKET_STREAM_INFORMATION          = 2;  //contains screen width and height, sent on connect
    public static final int DARVM_PACKET_SCREEN_UPDATES              = 4;  //updates changed pixels on the screen
    public static final int DARVM_PACKET_FULL_SCREEN_UPDATE          = 5;  //provides update to entire screen
    public static final int DARVM_PACKET_REQUEST_FULL_SCREEN_UPDATE  = 6;  //requests update to entire screen
    public static final int DARVM_PACKET_MOUSE_SET_POSITION          = 7;  //mouse position update
    public static final int DARVM_PACKET_MOUSE_LEFT_DOWN             = 8;  //left mouse button click
    public static final int DARVM_PACKET_MOUSE_LEFT_UP               = 9;  //left mouse button release
    public static final int DARVM_PACKET_MOUSE_MIDDLE_DOWN           = 10; //middle mouse button click
    public static final int DARVM_PACKET_MOUSE_MIDDLE_UP             = 11; //middle mouse button release
    public static final int DARVM_PACKET_MOUSE_RIGHT_DOWN            = 12; //right mouse button release
    public static final int DARVM_PACKET_MOUSE_RIGHT_UP              = 13; //right mouse button release
    public static final int DARVM_PACKET_MOUSE_SCROLL_DOWN           = 14; //right mouse button release
    public static final int DARVM_PACKET_MOUSE_SCROLL_UP             = 15; //right mouse button release
    public static final int DARVM_PACKET_KEYBOARD_KEY_DOWN           = 16; //key down
    public static final int DARVM_PACKET_KEYBOARD_KEY_UP             = 17; //key up
    
    private static final AtomicLong lastPing = new AtomicLong(0L);
    private static Socket sockster = null;
    
    public static final Robot robot;
    
    static{
        
        Robot yee = null;
        
        try{
            yee = new Robot();
        }catch(Throwable t){
            t.printStackTrace();
        }
        
        robot = yee;
        
    }
    
    public static void l(String t){
        System.out.println("[darvm server] "+t);
    }
    
    public static void main(String[] args) throws IOException {
        
        l("starting...");
        
        Thread pingpong = new Thread(() -> {
            while(true){
                try{
                    Thread.sleep(5000L);
                    if(sockster != null && !sockster.isClosed()){
                        OutputStream writer = sockster.getOutputStream();
                        while(true){
                            try{
                                if(System.currentTimeMillis() - lastPing.get() > 15000L){
                                    l(sockster.getRemoteSocketAddress().toString()+" timed out");
                                    sockster.close();
                                    break;
                                }
                                writer.write(DARVM_PACKET_PING);
                                writer.flush();
                            }catch(IOException e){
                                e.printStackTrace();
                                sockster.close();
                                break;
                            }
                            Thread.sleep(5000L);
                        }
                    }
                }catch(Throwable t){
                    t.printStackTrace();
                }
            }
        }, "pingpong");
        pingpong.setDaemon(true);
        pingpong.start();
        
        Thread framesender = new Thread(() -> {
            while(true){
                try{
                    if(sockster != null && !sockster.isClosed()){
                        try{
                            synchronized(ScreenCapture.class){
                                ScreenCapture.captureScreen();
                                byte[] updates = ScreenCapture.getUpdatedPixels();
                                OutputStream writer = sockster.getOutputStream();
                                synchronized(writer){
                                    ByteArrayOutputStream s = new ByteArrayOutputStream();
                                    s.write(DARVM_PACKET_SCREEN_UPDATES);
                                    s.write(updates);
                                    s.close();
                                    writer.write(s.toByteArray());
                                    writer.flush();
                                    l("sent DARVM_PACKET_SCREEN_UPDATES");
                                }
                            }
                        }catch(IOException e){
                            e.printStackTrace();
                            sockster.close();
                        }
                    }else{
                        Thread.sleep(1000L);
                    }
                }catch(Throwable t){
                    t.printStackTrace();
                }
            }
        }, "framesender");
        framesender.setDaemon(true);
        framesender.start();
        
        l("port 42059");
        
        ServerSocket server = new ServerSocket(42059);
        
        while (true) {
            sockster = server.accept();
            l("recieving connection from "+sockster.getRemoteSocketAddress().toString());
            lastPing.set(System.currentTimeMillis());
            try{
                InputStream  reader = sockster.getInputStream();
                OutputStream writer = sockster.getOutputStream();
                
                synchronized(writer){
                    ByteArrayOutputStream s = new ByteArrayOutputStream();
                    s.write(DARVM_PACKET_STREAM_INFORMATION);
                    s.write(ScreenCapture.getScreenSize());
                    s.close();
                    writer.write(s.toByteArray());
                    writer.flush();
                    l("sent DARVM_PACKET_STREAM_INFORMATION");
                }
                
                ScreenCapture.captureScreen();
                byte[] updates = ScreenCapture.getFullUpdate();
                
                synchronized(writer){
                    ByteArrayOutputStream s = new ByteArrayOutputStream();
                    s.write(DARVM_PACKET_FULL_SCREEN_UPDATE);
                    s.write(EncodingUtils.intToBytes(updates.length));
                    s.write(updates);
                    s.close();
                    writer.write(s.toByteArray());
                    writer.flush();
                    l("sent DARVM_PACKET_FULL_SCREEN_UPDATE");
                }
                
                while(true){
                    if(reader.available() > 0){
                        int packettype = reader.read();
                        switch(packettype){
                            case DARVM_PACKET_PING:
                                synchronized(writer){
                                    writer.write(DARVM_PACKET_PONG);
                                    writer.flush();
                                    l("sent DARVM_PACKET_PONG");
                                }
                                break;
                            case DARVM_PACKET_PONG:
                                l("recieved DARVM_PACKET_PONG");
                                lastPing.set(System.currentTimeMillis());
                                break;
                            case DARVM_PACKET_REQUEST_FULL_SCREEN_UPDATE:
                                l("recieved DARVM_PACKET_REQUEST_FULL_SCREEN_UPDATE");
                                synchronized(ScreenCapture.class){
                                    ScreenCapture.captureScreen();
                                    byte[] updates2 = ScreenCapture.getFullUpdate();
                                    synchronized(writer){
                                        ByteArrayOutputStream s = new ByteArrayOutputStream();
                                        s.write(DARVM_PACKET_FULL_SCREEN_UPDATE);
                                        s.write(EncodingUtils.intToBytes(updates2.length));
                                        s.write(updates2);
                                        s.close();
                                        writer.write(s.toByteArray());
                                        writer.flush();
                                        l("sent DARVM_PACKET_FULL_SCREEN_UPDATE");
                                    }
                                }
                                break;
                            case DARVM_PACKET_MOUSE_SET_POSITION:
                                byte[] x = new byte[4];
                                byte[] y = new byte[4];
                                reader.read(x);
                                reader.read(y);
                                int ix = EncodingUtils.bytesToInt(x);
                                int iy = EncodingUtils.bytesToInt(y);
                                robot.mouseMove(ix, iy);
                                break;
                            case DARVM_PACKET_MOUSE_LEFT_DOWN:
                                robot.mousePress(InputEvent.BUTTON1_MASK);
                                break;
                            case DARVM_PACKET_MOUSE_LEFT_UP:
                                robot.mouseRelease(InputEvent.BUTTON1_MASK);
                                break;
                            case DARVM_PACKET_MOUSE_MIDDLE_DOWN:
                                robot.mousePress(InputEvent.BUTTON3_MASK);
                                break;
                            case DARVM_PACKET_MOUSE_MIDDLE_UP:
                                robot.mouseRelease(InputEvent.BUTTON3_MASK);
                                break;
                            case DARVM_PACKET_MOUSE_RIGHT_DOWN:
                                robot.mousePress(InputEvent.BUTTON2_MASK);
                                break;
                            case DARVM_PACKET_MOUSE_RIGHT_UP:
                                robot.mouseRelease(InputEvent.BUTTON2_MASK);
                                break;
                            case DARVM_PACKET_MOUSE_SCROLL_DOWN:
                                robot.mouseWheel(-1);
                                break;
                            case DARVM_PACKET_MOUSE_SCROLL_UP:
                                robot.mouseWheel(1);
                                break;
                            case DARVM_PACKET_KEYBOARD_KEY_DOWN:
                                byte[] key = new byte[4];
                                reader.read(key);
                                int key2 = EncodingUtils.bytesToInt(key);
                                robot.keyPress(key2);
                                break;
                            case DARVM_PACKET_KEYBOARD_KEY_UP:
                                byte[] key3 = new byte[4];
                                reader.read(key3);
                                int key4 = EncodingUtils.bytesToInt(key3);
                                robot.keyRelease(key4);
                                break;
                            default:
                                break;
                        }
                        
                        if(sockster.isClosed()){
                            break;
                        }
                        
                    }
                    
                    if(sockster.isClosed()){
                        continue;
                    }
                    
                }
                
                l("connection closed");
                
            }catch(IOException yee){
                yee.printStackTrace();
                try{
                    sockster.close();
                }catch(IOException yee2){
                    yee2.printStackTrace();
                }
            }
        }
    }

}
