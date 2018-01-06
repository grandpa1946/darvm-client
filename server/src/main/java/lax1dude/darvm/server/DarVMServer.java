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

import java.awt.AWTException;
import java.awt.Robot;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author calder
 */
public class DarVMServer {
    
    public static final int DARVM_PACKET_PING                    = 0; //ping packet
    public static final int DARVM_PACKET_PONG                    = 1; //pong packet
    public static final int DARVM_PACKET_REQUEST_SCREEN_UPDATES  = 2; //requests screen changes
    public static final int DARVM_PACKET_RESPONSE_SCREEN_UPDATES = 3; //list of screen tiles to update
    public static final int DARVM_PACKET_REQUEST_ENTIRE_SCREEN   = 4; //requests update to entire screen
    public static final int DARVM_PACKET_RESPONSE_ENTIRE_SCREEN  = 5; //update to entire screen
    public static final int DARVM_PACKET_MOUSE_SET_POSITION      = 6; //mouse position update
    public static final int DARVM_PACKET_MOUSE_LEFT_DOWN         = 7; //left mouse button click
    public static final int DARVM_PACKET_MOUSE_LEFT_UP           = 8; //left mouse button release
    public static final int DARVM_PACKET_MOUSE_MIDDLE_DOWN       = 9; //middle mouse button click
    public static final int DARVM_PACKET_MOUSE_MIDDLE_UP         = 10; //middle mouse button release
    public static final int DARVM_PACKET_MOUSE_RIGHT_DOWN        = 11; //right mouse button release
    public static final int DARVM_PACKET_MOUSE_RIGHT_UP          = 12; //right mouse button release
    public static final int DARVM_PACKET_MOUSE_SCROLL_DOWN       = 13; //right mouse button release
    public static final int DARVM_PACKET_MOUSE_SCROLL_UP         = 14; //right mouse button release
    public static final int DARVM_PACKET_KEYBOARD_KEY_DOWN       = 15; //key down
    public static final int DARVM_PACKET_KEYBOARD_KEY_UP         = 16; //key up
    
    public static final Robot ROBOT;
    
    static{
        Robot deev;
        try{
            deev = new Robot();
        }catch(AWTException e){
            deev = null;
            e.printStackTrace();
            System.exit(-1);
        }
        ROBOT = deev;
    }
    
    private static final Random rng = new Random();
    private static final AtomicLong lastPing = new AtomicLong(0L);
    private static Socket sockster = null;
    
    public static void main(String[] args) throws IOException {
        
        Thread pingpong = new Thread(() -> {
            while(true){
                try{
                    Thread.sleep(5000L);
                    if(sockster != null && !sockster.isClosed()){
                        if(System.currentTimeMillis() - lastPing.get() > 15000L){
                            sockster.close();
                            continue;
                        }
                        OutputStream writer = sockster.getOutputStream();
                        writer.write(DARVM_PACKET_PING);
                        byte[] ping = new byte[64];
                        rng.nextBytes(ping);
                        writer.write(ping);
                        writer.flush();
                    }
                }catch(Throwable t){
                    t.printStackTrace();
                }
            }
        }, "pingpong");
        pingpong.setDaemon(true);
        pingpong.start();
        
        ServerSocket server = new ServerSocket(42059);
        
        while (true) {
            sockster = server.accept();
            lastPing.set(System.currentTimeMillis());
            try{
                InputStream  reader = sockster.getInputStream();
                OutputStream writer = sockster.getOutputStream();
                while(true){
                    if(reader.available() > 0){
                        int packettype = reader.read();
                        switch(packettype){
                            case DARVM_PACKET_PING:
                                byte[] pong = new byte[16];
                                if(reader.read(pong) < 16){
                                    sockster.close();
                                    break;
                                }
                                writer.write(DARVM_PACKET_PONG);
                                writer.write(pong);
                                writer.flush();
                                break;
                            case DARVM_PACKET_PONG:
                                byte[] pong2 = new byte[16];
                                if(reader.read(pong2) < 16){
                                    sockster.close();
                                    break;
                                }
                                lastPing.set(System.currentTimeMillis());
                                break;
                            case DARVM_PACKET_REQUEST_SCREEN_UPDATES:
                            case DARVM_PACKET_RESPONSE_SCREEN_UPDATES:
                            case DARVM_PACKET_REQUEST_ENTIRE_SCREEN:
                            case DARVM_PACKET_RESPONSE_ENTIRE_SCREEN:
                            case DARVM_PACKET_MOUSE_SET_POSITION:
                            case DARVM_PACKET_MOUSE_LEFT_DOWN:
                            case DARVM_PACKET_MOUSE_LEFT_UP:
                            case DARVM_PACKET_MOUSE_MIDDLE_DOWN:
                            case DARVM_PACKET_MOUSE_MIDDLE_UP:
                            case DARVM_PACKET_MOUSE_RIGHT_DOWN:
                            case DARVM_PACKET_MOUSE_RIGHT_UP:
                            case DARVM_PACKET_MOUSE_SCROLL_DOWN:
                            case DARVM_PACKET_MOUSE_SCROLL_UP:
                            case DARVM_PACKET_KEYBOARD_KEY_DOWN:
                            case DARVM_PACKET_KEYBOARD_KEY_UP:
                                break;
                        }
                        
                        if(sockster.isClosed()){
                            break;
                        }
                        
                    }
                }
                
                if(sockster.isClosed()){
                    continue;
                }
                
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
