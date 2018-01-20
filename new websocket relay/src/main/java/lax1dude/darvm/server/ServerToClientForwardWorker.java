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

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import org.java_websocket.WebSocket;

/**
 *
 * @author calder
 */
public class ServerToClientForwardWorker extends Thread {
    
    private final WebSocket client;
    public final Socket server;
    public final InputStream serverStream;
    
    public ServerToClientForwardWorker(WebSocket client, Socket server) throws IOException{
        super("serversocketworker/"+client.getRemoteSocketAddress().toString());
        this.client = client;
        this.server = server;
        this.serverStream = server.getInputStream();
    }
    
    @Override
    public void run() {
        while(!server.isClosed()){
            try {
                int initialByte = this.serverStream.read(); //block the thread
                System.out.println("recieveing");
                if(initialByte != -1){
                    int avail = this.serverStream.available();
                    if(avail > 0){
                        System.out.println(avail);
                        byte[] yee = new byte[avail+1];
                        yee[0] = (byte)initialByte;
                        this.serverStream.read(yee, 1, avail);
                        this.client.send(yee);
                        //byte[] bytus = new byte[8192];
                        //if(avail > 8191){
                        //    System.out.println("sending frags");
                        //    bytus[0] = (byte) initialByte;
                        //    int yee = this.serverStream.read(bytus, 1, 8191);
                        //    this.client.send(ByteBuffer.wrap(bytus, 0, yee));
                        //    avail = this.serverStream.available();
                        //    int num = avail / 8192;
                        //    for(int i = 0; i <= num; i++){
                        //        bytus = new byte[8192];
                        //        int yeee = this.serverStream.read(bytus, 0, Math.min(8192, avail - i*8192));
                        //        this.client.send(ByteBuffer.wrap(bytus, 0, yeee));
                        //    }
                        //}else{
                        //    System.out.println("sending frag");
                        //    bytus[0] = (byte) initialByte;
                        //    int yee = this.serverStream.read(bytus, 1, avail);
                        //    this.client.send(ByteBuffer.wrap(bytus, 0, yee));
                        //}
                    }else{
                        System.out.println("sent one byte");
                        this.client.send(ByteBuffer.wrap(new byte[]{(byte)initialByte}));
                    }
                }else{
                    this.client.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                try {
                    server.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                }
                this.client.close();
            }
        }
        if(!(this.client.isClosing() || this.client.isClosed())) this.client.close();
    }
    
}
