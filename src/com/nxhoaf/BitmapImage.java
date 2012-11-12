package com.nxhoaf;

import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.nxhoaf.BitmapHeader.Attribute;
import com.sun.org.apache.bcel.internal.classfile.Field;


public class BitmapImage {
	private BitmapHeader bitmapHeader;
	private byte[] data;

	public BitmapImage() {
		this.bitmapHeader = new BitmapHeader();
		data = null;
	}
	
	public void setData(byte[] data) {
		this.data = data;
		
	}
	
	public void setBitMapHeader(BitmapHeader bitmapHeader) {
		this.bitmapHeader = bitmapHeader;
	}
	
	public byte[] createImage() {
		byte[] header = bitmapHeader.dump();
		int imageSize = header.length + data.length;
		byte[] image = new byte[imageSize];
		System.arraycopy(header, 0, image, 0, header.length);
		System.arraycopy(data, 0, image, header.length, data.length);
		return image;
	}
	
	public static void main(String[] args) {
		final int WIDTH = 1;
		final int HEIGHT = 1;
		
		byte[] bytes;
		byte[] data = {
				0x00		, 0x00	, (byte) 0xFF	, (byte) 0xFF	, (byte) 0xFF, (byte)0xFF	, 0x00, 0x00,
				(byte) 0xFF , 0x00	, 0x00 			, 0x00 			, (byte) 0xFF, 0x00 		, 0x00, 0x00
		};
		
		BitmapHeader bitmapHeader = new BitmapHeader();
		
		// Signature
		bytes = new byte[2];
		bytes[0] = 0x42;
		bytes[1] = 0x4D;
		bitmapHeader.setAttribute(Attribute.SIGNATURE, bytes);
		
		// File Size 
		bytes = new byte[4];
		bytes[0] = 0x46;
		bytes[1] = 0x00;
		bytes[3] = 0x00;
		bytes[4] = 0x00;
		bitmapHeader.setAttribute(
				Attribute.FILE_SIZE, 
				bytes);
		
		// Reserve 1 
		bitmapHeader.setAttribute(
				Attribute.RESERVED_1, 
				new byte[] {0x00,0x00});
		
		// Reserve 2 
		bitmapHeader.setAttribute(
				Attribute.RESERVED_2, 
				new byte[] {0x00,0x00});
		
		// Offset where the pixel array 
		bitmapHeader.setAttribute(
				Attribute.FILE_OFFSET_TO_PIXEL_ARRAY, 
				new byte[] {0x36,0x00,0x00,0x00});
		
		// ------------ DIB Header --------------------
		// Number of bytes in the DIB header (from this point)
		bitmapHeader.setAttribute(
				Attribute.DIB_HEADER_SIZE, 
				new byte[] {0x28,0x00,0x00,0x00});
		
		
		bitmapHeader.setAttribute(
				Attribute.IMAGE_WIDTH, 
				new byte[] {0x02,0x00,0x00,0x00});
		
		
		bitmapHeader.setAttribute(
				Attribute.IMAGE_HEIGHT, 
				new byte[] {0x02,0x00,0x00,0x00});
		
		// Number of color planes being used
		bitmapHeader.setAttribute(
				Attribute.PLANES, 
				new byte[] {0x01,0x00});
		
		bitmapHeader.setAttribute(
				Attribute.BITS_PER_PIXEL, 
				new byte[] {0x18,0x00});
		
		
		bitmapHeader.setAttribute(
				Attribute.IMAGE_HEIGHT, 
				new byte[] {0x02,0x00,0x00,0x00});
		
		// Compression
		bitmapHeader.setAttribute(
				Attribute.COMPRESSION, 
				new byte[] {0x00,0x00,0x00,0x00});
		
		bitmapHeader.setAttribute(
				Attribute.IMAGE_SIZE, 
				new byte[] {0x10,0x00,0x00,0x00});
		
		// Horizontal resolution of the image
		bitmapHeader.setAttribute(
				Attribute.X_PIXELS_PER_METER, 
				new byte[] {0x13,0x0B,0x00,0x00});
		
		// Vertical resolution of the image
		bitmapHeader.setAttribute(
				Attribute.Y_PIXELS_PER_METER, 
				new byte[] {0x13,0x0B,0x00,0x00});
		
		// 	Number of colors in the palette
		bitmapHeader.setAttribute(
				Attribute.COLORS_IN_COLOR_TABLE, 
				new byte[] {0x00,0x00,0x00,0x00});
		
		// 0 means all colors are important
		bitmapHeader.setAttribute(
				Attribute.IMPORTANT_COLOR_COUNT, 
				new byte[] {0x00,0x00,0x00,0x00});
		
		
		
		BitmapImage bitmapImage = new BitmapImage();
		bitmapImage.setData(data);
		bitmapImage.setBitMapHeader(bitmapHeader);
		
		byte[] rawImage = bitmapImage.createImage();
		InputStream imageStream = new ByteArrayInputStream(rawImage);
		
		FileInputStream fileInputStream=null;
		 
        File file = new File("image.bmp");
 
        byte[] bFile = new byte[(int) file.length()];
 
        try {

 
	    //convert array of bytes into file
//	    FileOutputStream fileOuputStream = 
//                  new FileOutputStream("image.bmp"); 
//	    fileOuputStream.write(rawImage);
//	    fileOuputStream.close();
 
	    System.out.println("Done");
        }catch(Exception e){
            e.printStackTrace();
        }
		
	}
}
