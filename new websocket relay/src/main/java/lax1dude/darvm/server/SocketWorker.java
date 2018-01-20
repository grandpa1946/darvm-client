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
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import lax1dude.darvm.server.WebSocketRelay.VMData;
import org.java_websocket.WebSocket;
import org.json.JSONObject;

/**
 *
 * @author calder
 */
public class SocketWorker extends Thread {
    
    private WebSocket ws;
    private String remoteAddress;
    private ArrayList eventQueue = new ArrayList();
    
    public final Object lock = new Object();
    
    public SocketWorker(WebSocket ws){
        super("socketworker/"+ws.getRemoteSocketAddress().toString());
        this.remoteAddress = ws.getRemoteSocketAddress().toString();
        this.ws = ws;
    }
    
    private void l(String e){
        System.out.println("[darvm websocket relay][socketworker/"+remoteAddress+"] " + e);
    }
    
    @Override
    public void run(){
        l("starting worker for connection "+remoteAddress);
        while(ws.isOpen()){
            try{
                synchronized(lock){ lock.wait(2000L); }
                ArrayList arr;
                synchronized(eventQueue){
                    arr = (ArrayList)eventQueue.clone();
                    eventQueue.clear();
                }
                for(Object o : arr){
                    if(o instanceof String){
                        this.handleMessage((String)o);
                    }else if(o instanceof ByteBuffer){
                        this.handleMessage((ByteBuffer)o);
                    }
                }
            }catch(Throwable t){
                t.printStackTrace();
                try{
                    ws.close();
                }catch(Throwable t2){
                    t2.printStackTrace();
                }
                return;
            }
        }
    }
    
    private boolean isAuthenticated = false;
    private boolean isSocketConnected = false;
    
    public ServerToClientForwardWorker serverToClientThread = null;
    
    private Socket sockler = null;
    private OutputStream socklerOut = null;
    
    private VMData VMList = null;
    
    private void handleMessage(String string) throws IOException{
        JSONObject json = new JSONObject(string);
        if(!isAuthenticated){
            String username = json.getString("username");
            String password = json.getString("password");
            VMData vmdata = WebSocketRelay.getUserVMs(username, password);
            if(vmdata != null){
                isAuthenticated = true;
                VMList = vmdata;
                l("logged in as: "+username);
                this.ws.send(VMList.sendToClient);
            }else{
                ws.close();
                l("invalid user/pass combination: "+username+"; [pass not shown]");
            }
        }else{
            int vmid = json.getInt("id");
            if(vmid < VMList.addrs.length){
                l("connecting to vm '"+VMList.names[vmid]+"' ("+VMList.addrs[vmid]+")");
                String[] yee = VMList.addrs[vmid].split(":");
                sockler = new Socket(yee[0],Integer.parseInt(yee[1]));
                socklerOut = sockler.getOutputStream();
                isSocketConnected = true;
                this.ws.send("{\"connected\":true}");
                serverToClientThread = new ServerToClientForwardWorker(ws, sockler);
                serverToClientThread.setDaemon(true); serverToClientThread.start();
            }else{
                ws.close();
                l("vm id is invalid");
            }
        }
    }
    
    private void handleMessage(ByteBuffer bb) throws IOException{
        if(isSocketConnected){
            socklerOut.write(bb.array()); //client never sends more than, like, 10 bytes at a time so this is fine
            socklerOut.flush();
        }
    }
    
    public void onMessage(String string) {
        synchronized(eventQueue){
            eventQueue.add(string);
        }
        synchronized(lock){
            lock.notifyAll();
        }
    }
    
    public void onMessage(ByteBuffer bb) {
        synchronized(eventQueue){
            eventQueue.add(bb);
        }
        synchronized(lock){
            lock.notifyAll();
        }
    }
    
}
