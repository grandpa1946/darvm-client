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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HashMap;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WebSocketRelay extends WebSocketServer {
    
    private static final HashMap<InetSocketAddress,SocketWorker> workerThreads = new HashMap();
    private static final HashMap<String,String> userPassHashTable = new HashMap();
    private static final HashMap<String,VMData> userVMTable = new HashMap();
    
    public static void main(String[] args) throws IOException,JSONException {
        l("loading configuration files...");
        JSONArray usertable = new JSONArray(new String(Files.readAllBytes(Paths.get("passwords"))));
        JSONObject vmtable = new JSONObject(new String(Files.readAllBytes(Paths.get("vm_list"))));
        for(Object o : usertable){
            JSONObject o2 = (JSONObject)o;
            String u = o2.getString("username");
            userPassHashTable.put(u, hashPassword(o2.getString("password")));
            JSONArray o3 = vmtable.getJSONArray(u);
            JSONArray stc = new JSONArray();
            VMData v = new VMData();
            int s = o3.length();
            v.names = new String[s];
            v.addrs = new String[s];
            for(int i = 0; i < s; i++){
                JSONObject o4 = o3.getJSONObject(i);
                v.names[i] = o4.getString("name");
                v.addrs[i] = o4.getString("addr");
                stc.put(v.names[i]);
            }
            v.sendToClient = stc.toString();
            userVMTable.put(u, v);
        }
        final WebSocketRelay instance = new WebSocketRelay(new InetSocketAddress("localhost", 42069));
        instance.start();
        instance.setConnectionLostTimeout(15);
    }
    
    static class VMData{
        public String sendToClient;
        public String[] names;
        public String[] addrs;
    }
    
    private static final MessageDigest digest;
    
    static{
        MessageDigest deeeee;
        try{
            deeeee = MessageDigest.getInstance("SHA-256");
        }catch(Throwable t){
            t.printStackTrace();
            deeeee = null;
        }
        digest = deeeee;
    }
    
    public static String hashPassword(String s){
        return byteArrayToHexString(digest.digest(("#$%#yeeLer#^#^$^GFD"+s+"YEE69%#$#@1").getBytes(StandardCharsets.UTF_8)));
    }
    
    public static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }
    
    static VMData getUserVMs(String username, String password) {
        String pass;
        if((pass = userPassHashTable.get(username)) != null && pass.equals(password)){
            return userVMTable.get(username);
        }return null;
    }
    
    public WebSocketRelay(InetSocketAddress address) {
            super(address);
    }
    
    @Override
    public void onOpen(WebSocket ws, ClientHandshake ch) {
        l("new connection from "+ws.getRemoteSocketAddress().toString());
        SocketWorker worker = new SocketWorker(ws);
        worker.setDaemon(true);
        worker.start();
        workerThreads.put(ws.getRemoteSocketAddress(), worker);
    }

    @Override
    public void onClose(WebSocket ws, int i, String string, boolean bln) {
        SocketWorker worker = workerThreads.get(ws.getRemoteSocketAddress());
        try {
            worker.serverToClientThread.server.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        worker.serverToClientThread.stop();
        worker.stop();
        l(ws.getRemoteSocketAddress().toString()+" disconnected");
    }

    @Override
    public void onMessage(WebSocket ws, String string) {
        workerThreads.get(ws.getRemoteSocketAddress()).onMessage(string);
    }
    
    @Override
    public void onMessage(WebSocket ws, ByteBuffer bb) {
        workerThreads.get(ws.getRemoteSocketAddress()).onMessage(bb);
    }

    @Override
    public void onError(WebSocket ws, Exception excptn) {
        
    }

    @Override
    public void onStart() {
        l("server started");
    }
    
    private static void l(String e){
        System.out.println("[darvm websocket relay] " + e);
    }
}
