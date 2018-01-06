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
import java.util.zip.GZIPOutputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;

/**
 *
 * @author calder
 */
public class ScreenCapture {

    public static BufferedImage bufferA = null;
    public static BufferedImage bufferB = null;
    public static final ImageWriter JPEGWriter;
    public static final JPEGImageWriteParam JPEGWriterParams;
    public static final Rectangle screen = //new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                                           new Rectangle(0,0,800,600); //benchmarking
    
    static{
        JPEGWriter = ImageIO.getImageWritersByFormatName("jpg").next();
        JPEGWriterParams = new JPEGImageWriteParam(null);
        JPEGWriterParams.setOptimizeHuffmanTables(true);
        JPEGWriterParams.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
        JPEGWriterParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        JPEGWriterParams.setCompressionQuality(0.15f);
    }

    public static void captureScreen() {
        bufferA = DarVMServer.ROBOT.createScreenCapture(screen);
    }
    
    public static BufferedImage cloneBuffer(BufferedImage buffer){
        ColorModel cm = bufferA.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bufferA.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }
    
    public static byte[] getFullUpdate() throws IOException {
        byte[] out = bufferedImageToByteArray(bufferA);
        bufferB = cloneBuffer(bufferA); //bufferB represents client state
        return out;
    }
    
    public static byte[] getUpdatedPixels() throws IOException {
        int w = bufferA.getWidth();
        int h = bufferA.getHeight();
        WritableRaster rasterA = bufferA.getRaster();
        WritableRaster rasterB = bufferB.getRaster();
        int[] rasterpixelA = new int[3];
        int[] rasterpixelB = new int[3];
        BufferedImage out = new BufferedImage(w, h, ColorSpace.TYPE_RGB);
        WritableRaster rasterO = out.getRaster();
        int pixels = (w*h);
        byte[] maskout = new byte[(pixels/8) + ((pixels%8>0?1:0))];
        byte[] bitmasks = new byte[]{(byte)1,(byte)2,(byte)4,(byte)8,(byte)16,(byte)32,(byte)64,(byte)128,(byte)256};
        boolean screenchanged = false;
        for(int x = 0; x < w; x++){
            for(int y = 0; y < h; y++){
                rasterA.getPixel(x, y, rasterpixelA);
                rasterB.getPixel(x, y, rasterpixelB);
                if(rasterpixelA[0] != rasterpixelB[0] || rasterpixelA[1] != rasterpixelB[1] || rasterpixelA[2] != rasterpixelB[2]){
                    rasterO.setPixel(x, y, rasterpixelA);
                    rasterB.setPixel(x, y, rasterpixelA);
                    int pixelIndex = (x * h + y);
                    maskout[pixelIndex / 8] |= bitmasks[pixelIndex % 8];
                    screenchanged = true;
                }
            }
        }
        
        if(!screenchanged) return null;
        
        ByteArrayOutputStream yee  = new ByteArrayOutputStream();
        GZIPOutputStream bao = new GZIPOutputStream(yee);
        bao.write(EncodingUtils.intToBytes(maskout.length));
        bao.write(maskout);
        byte[] jpeg = bufferedImageToByteArray(out);
        bao.write(EncodingUtils.intToBytes(jpeg.length));
        bao.write(jpeg);
        bao.flush(); bao.close();
        return yee.toByteArray();
    }
    
    public static byte[] bufferedImageToByteArray(BufferedImage yee) throws IOException{
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        JPEGWriter.setOutput(ImageIO.createImageOutputStream(bao));
        JPEGWriter.write(null, new IIOImage(yee, null, null), JPEGWriterParams);
        bao.flush(); bao.close();
        return bao.toByteArray();
    }

}
