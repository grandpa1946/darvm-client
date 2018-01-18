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

/**
 *
 * @author calder
 */
public class Benchmarking {
    
    
    public static void main(String[] args) throws Throwable {
        
        byte[] yigg = EncodingUtils.intToBytes(765);
        for(int i = 0; i < yigg.length; i++){
            System.out.println((int)yigg[i]);
        }
        System.out.println(EncodingUtils.bytesToInt(EncodingUtils.intToBytes(765)));
        
        ScreenCapture.getScreenSize();
        ScreenCapture.captureScreen();
        ScreenCapture.getFullUpdate();
        
        while(true){
            long time = System.currentTimeMillis();
            int frames = 0;
            while(System.currentTimeMillis() - time < 1000L){
                ScreenCapture.captureScreen();
                //byte[] data = ScreenCapture.getUpdatedPixels();
                //if(data != null) System.out.println("     " + Integer.toString(data.length));
                frames += 1;
            }
            System.out.println(frames);
        }
        
    }
    
}
