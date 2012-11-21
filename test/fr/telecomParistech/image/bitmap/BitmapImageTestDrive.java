package fr.telecomParistech.image.bitmap;

import static org.junit.Assert.*;

import org.junit.Test;

import fr.telecomParistech.image.bitmap.BitmapHeader.Attribute;

public class BitmapImageTestDrive {

	private byte[] bytes;
	private byte[] data;
	private BitmapHeader bitmapHeader = new BitmapHeader();
	private BitmapImage bitmapImage = new BitmapImage();

	{
		bitmapHeader = setUpBitmapHeader();
		data = setUpData();
		bitmapImage.setData(data);
		bitmapImage.setBitMapHeader(bitmapHeader);
	}

	@Test
	public void testCreateImage() {
		byte[] height = bitmapHeader.getAttributeValue(Attribute.IMAGE_HEIGHT);
		assertArrayEquals(
				"Image Height", 
				new byte[] {0x02,0x00,0x00,0x00}, 
				height);
	}

	private byte[] setUpData() {
		byte[] data = {
				(byte) 0x00	, (byte) 0x00 , (byte) 0xFF	, (byte) 0xFF, 
				(byte) 0xFF , (byte) 0xFF , (byte) 0x00 , (byte) 0x00,
				(byte) 0xFF , (byte) 0x00 , (byte) 0x00 , (byte) 0x00, 
				(byte) 0xFF , (byte) 0x00 , (byte) 0x00 , (byte) 0x00
		};
		return data;
	}
	
	private BitmapHeader setUpBitmapHeader() {
		bitmapHeader = new BitmapHeader();

		// Signature
		bytes = new byte[2];
		bytes[0] = 0x42;
		bytes[1] = 0x4D;
		bitmapHeader.setAttribute(Attribute.SIGNATURE, bytes);

		// File Size 
		bytes = new byte[4];
		bytes[0] = 0x46;
		bytes[1] = 0x00;
		bytes[2] = 0x00;
		bytes[3] = 0x00;
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
		
		return bitmapHeader;
	}
}
