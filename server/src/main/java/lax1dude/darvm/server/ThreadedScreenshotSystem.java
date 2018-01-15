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

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;

/**
 *
 * @author calder
 */
public class ThreadedScreenshotSystem {
    
    private static final int cores = Runtime.getRuntime().availableProcessors();
    
    private static final Rectangle[] rectanglePool = new Rectangle[cores];
    private static final BufferedImage[] outputPool = new BufferedImage[cores];
    
    private static final Object threadStartLock = new Object();
    private static final Object threadFinishLock = new Object();
    private static final Object threadCommitLock = new Object();
    
    public static void startThreads(){
        for(int i = 0; i < cores; i++){
            final int i2 = i;
            Thread threadler = new Thread(() -> {
                try{
                    
                    Robot robus = new Robot();
                    
                    while(true){
                        
                        log("waiting "+i2);
                        
                        synchronized(threadStartLock){ threadStartLock.wait(); }
                        
                        log("unlocked "+i2);
                        
                        BufferedImage out = robus.createScreenCapture(rectanglePool[i2]);
                        
                        log("took screenshot "+i2+" "+rectanglePool[i2].toString());
                        
                        synchronized(threadCommitLock){
                            
                            outputPool[i2] = out;
                            boolean flag = true;
                            
                            for(int i3 = 0; i3 < cores; i3++){
                                if(outputPool[i3] == null){
                                    flag = false;
                                    break;
                                }
                            }
                            
                            if(flag){
                                log("unlocking main");
                                synchronized(threadFinishLock){ threadFinishLock.notifyAll(); }
                            }
                            
                        }
                        
                    }
                }catch(Throwable t){
                    throw new RuntimeException(t);
                }
            });
            
            threadler.setDaemon(true);
            threadler.setName("darvm-screencapture-"+Integer.toString(i));
            threadler.start();
        }  
    }
    
    public static void log(String s){
        System.out.println(System.currentTimeMillis() + "  -  " + s);
    }
    
    public static BufferedImage captureThreadedScreenshot(int w, int h){
        int sh = h / cores;
        int outheight = sh * cores;
        
        for(int i = 0; i < cores; i++){
            rectanglePool[i] = new Rectangle(0, i * sh, w, sh);
            outputPool[i] = null;
        }
        
        log("unlocking threads");
        
        synchronized(threadStartLock){
            threadStartLock.notifyAll();
        }
        
        log("waiting for threads to finish");
        
        synchronized(threadFinishLock){
            try{
                threadFinishLock.wait();
            }catch(Throwable t){
                throw new RuntimeException(t);
            }
        }
        
        BufferedImage out = new BufferedImage(w, h, ColorSpace.TYPE_RGB);
        int[] sectionarray = new int[sh * w];
        
        for(int i = 0; i < cores; i++){
            Rectangle rectus = rectanglePool[i];
            outputPool[i].getRGB(0, 0, rectus.width, rectus.height, sectionarray, 0, 1); //(0, 0, rectus.width, rectus.height, (int[])null);
            out.setRGB(rectus.x, rectus.y, rectus.width, rectus.height, sectionarray, 0, 1);
        }
        
        return out;
    }
    
}
