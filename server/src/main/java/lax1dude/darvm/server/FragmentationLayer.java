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
import java.io.OutputStream;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author calder
 */
public class FragmentationLayer {
    
    public static synchronized byte[] recievePacket(InputStream in) throws IOException{
        byte[] length = new byte[4];
        in.read(length);
        byte[] packet = new byte[EncodingUtils.bytesToInt(length)];
        in.read(packet);
        return packet;
    }
    
    public static synchronized void sendPacket(OutputStream out, byte[] packet) throws IOException{
        out.write(EncodingUtils.intToBytes(packet.length));
        out.flush();
        int packetsToSend = packet.length / 8192 + 1;
        for(int i = 0; i < packetsToSend; i++){
            int index = i*8192;
            out.write(ArrayUtils.subarray(packet, index, Math.min(index + 8192, packet.length)));
            out.flush();
        }
    }
}
