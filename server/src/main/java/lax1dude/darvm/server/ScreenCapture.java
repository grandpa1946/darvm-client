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
import java.awt.Toolkit;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author calder
 */
public class ScreenCapture {

    public static BufferedImage bufferA = null;
    public static BufferedImage bufferB = null;
    public static final ImageWriter JPEGWriter;
    public static final JPEGImageWriteParam JPEGWriterParams;
    public static Rectangle screen = null;
    
    static{
        JPEGWriter = ImageIO.getImageWritersByFormatName("jpg").next();
        JPEGWriterParams = new JPEGImageWriteParam(null);
        JPEGWriterParams.setOptimizeHuffmanTables(true);
        JPEGWriterParams.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
        JPEGWriterParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        JPEGWriterParams.setCompressionQuality(0.15f);
    }

    public static byte[] getScreenSize() {
        screen = //new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                 new Rectangle(0,0,1024,768); //benchmarking
        byte[] sizedata = new byte[8];
        byte[] w = EncodingUtils.intToBytes(screen.width);
        byte[] h = EncodingUtils.intToBytes(screen.height);
        sizedata[0] = w[0];
        sizedata[1] = w[1];
        sizedata[2] = w[2];
        sizedata[3] = w[3];
        sizedata[4] = h[0];
        sizedata[5] = h[1];
        sizedata[6] = h[2];
        sizedata[7] = h[3];
        return sizedata;
    }
    
    public static void captureScreen() {
        //bufferA = ThreadedScreenshotSystem.captureThreadedScreenshot(screen.width, screen.height);
        bufferA = DarVMServer.robot.getBufferedImage(0,0,screen.width,screen.height);
    }
    
    public static BufferedImage cloneBuffer(BufferedImage buffer){
        ColorModel cm = bufferA.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bufferA.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }
    
    public static byte[] getFullUpdate() throws IOException {
        byte[] out = bufferedImageToByteArray(bufferA);
        bufferB = bufferA; //bufferB represents client state
        return out;
    }
    
    public static final int tolerance = 128;
    
    public static byte[] getUpdatedPixels() throws IOException {
        int w = bufferA.getWidth();
        int h = bufferA.getHeight();
        WritableRaster rasterA = bufferA.getRaster();
        WritableRaster rasterB = bufferB.getRaster();
        int[] rasterpixelA = new int[3];
        int[] rasterpixelB = new int[3];
        //BufferedImage out = new BufferedImage(w, h, ColorSpace.TYPE_RGB);
        //WritableRaster rasterO = out.getRaster();
        boolean screenchanged = false;
        rect[] changedSections = new rect[0];
        System.out.println("comparing");
        for(int x = 0; x < w; x++){
            for(int y = 0; y < h; y++){
                rasterA.getPixel(x, y, rasterpixelA);
                rasterB.getPixel(x, y, rasterpixelB);
                if(rasterpixelA[0] != rasterpixelB[0] || rasterpixelA[1] != rasterpixelB[1] || rasterpixelA[2] != rasterpixelB[2]){
                    boolean flag = false;
                    for(int i = 0; i < changedSections.length; i++) {
                        rect r = changedSections[i];
                        if(r.x1 - x > tolerance || r.y1 - y > tolerance || x - r.x2 > tolerance || y - r.y1 > tolerance){
                            continue;
                        }else{
                            r.x1 = Math.min(x, r.x1);
                            r.y1 = Math.min(y, r.y1);
                            r.x2 = Math.max(x, r.x2);
                            r.y2 = Math.max(y, r.y2);
                            flag = true;
                            break;
                        }
                    }
                    
                    if(!flag){
                        rect r = new rect();
                        r.x1 = x;
                        r.x2 = x;
                        r.y1 = y;
                        r.y2 = y;
                        changedSections = ArrayUtils.add(changedSections, r);
                    }
                    
                    screenchanged = true;
                }
            }
        }
        
        System.out.println("exporting");
        
        bufferB = bufferA;
        
        if(!screenchanged) return null;
        
        //for(int i = 0; i < changedSections.length; i++) {
        //    System.out.println("rect: "+changedSections[i].x1+" "+changedSections[i].y1+" "+changedSections[i].x2+" "+changedSections[i].y2);
        //}
        
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        bao.write(EncodingUtils.intToBytes(changedSections.length));
        for(int i = 0; i < changedSections.length; i++) {
            rect r = changedSections[i];
            bao.write(EncodingUtils.intToBytes(r.x1));
            bao.write(EncodingUtils.intToBytes(r.y1));
            byte[] data = bufferedImageToByteArray(bufferA.getSubimage(r.x1, r.y1, r.x2 - r.x1 + 1, r.y2 - r.y1 + 1));
            bao.write(EncodingUtils.intToBytes(data.length));
            bao.write(data);
        }
        bao.close();
        System.out.println("sending");
        return bao.toByteArray();
    }
    
    static class rect{
        int x1;
        int y1;
        int x2;
        int y2;
    }
    
    public static byte[] bufferedImageToByteArray(BufferedImage yee) throws IOException{
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        JPEGWriter.setOutput(ImageIO.createImageOutputStream(bao));
        JPEGWriter.write(null, new IIOImage(yee, null, null), JPEGWriterParams);
        bao.flush(); bao.close();
        return bao.toByteArray();
    }

}
