package fr.telecomParistech.image.bitmap;

import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;


public class BitmapHeader {
	private static final Logger log = 
			Logger.getLogger(BitmapHeader.class.getName());
	// Predefined attributes
	public enum Attribute {

		// Bitmap File Header: 		{Offset, Size}
		SIGNATURE 					(0x00, 0x02),
		FILE_SIZE 					(0x02, 0x04),
		RESERVED_1 					(0x06, 0x02),
		RESERVED_2 					(0x08, 0x02),
		FILE_OFFSET_TO_PIXEL_ARRAY 	(0x0A, 0x04),

		// DIB Header
		// BitmapV5Header
		DIB_HEADER_SIZE 			(0x0E, 0x04),
		IMAGE_WIDTH 				(0x12, 0x04),
		IMAGE_HEIGHT 				(0x16, 0x04),
		PLANES 						(0x1a, 0x02),
		BITS_PER_PIXEL 				(0x1c, 0x02),
		COMPRESSION 				(0x1E, 0x04),
		IMAGE_SIZE 					(0x22, 0x04),
		X_PIXELS_PER_METER 			(0x26, 0x04),
		Y_PIXELS_PER_METER 			(0x2A, 0x04),
		COLORS_IN_COLOR_TABLE 		(0x2E, 0x04),
		IMPORTANT_COLOR_COUNT 		(0x32, 0x04),
		RED_CHANNEL_BITMASK			(0x36, 0x04),
		GREEN_CHANNEL_BITMASK		(0x3A, 0x04),
		BLUE_CHANNEL_BITMASK		(0x3E, 0x04),
		ALPHA_CHANNEL_BITMASK		(0x42, 0x04),
		COLOR_SPACE_TYPE			(0x46, 0x04),
		COLOR_SPACE_ENDPOINTS		(0x4A, 0x24),
		GAMMA_FOR_RED_CHANNEL		(0x6E, 0x04),
		GAMMA_FOR_GREEN_CHANNEL		(0x72, 0x04),
		GAMMA_FOR_BLUE_CHANNEL		(0x76, 0x04),
		INTENT 						(0x7A, 0x04),
		ICC_PROFILE_DATA 			(0x7E, 0x04),
		ICC_PROFILE_SIZE 			(0x82, 0x04),
		RESERVED 					(0x86, 0x04);


		private final int offset;
		private final int length;


		private Attribute(int offset, int length) {
			this.offset = offset;
			this.length = length;
		}

		public String getLength(Radix radix) {
			return ConvertUtility.getNumber(length,radix);
		}

		public int getLength() {
			return length;
		}

		public String getOffset(Radix radix) {
			return ConvertUtility.getNumber(offset, radix);
		}

		public int getOffset() {
			return offset;
		}
	}

	private Map<Integer, byte[]> attributes;
	private int headerLength;

	public BitmapHeader() {
		attributes = new TreeMap<Integer, byte[]>();
		headerLength = 0;
	}

	public void setAttribute(Attribute a, byte[] value) {
		// Save value, use it offset as key
		attributes.put(a.getOffset(), value);
		
		String strValue = "";
		for (byte b : value) {
			String hex = Integer.toHexString(b & 0xFF);
			if (hex.length() == 1) {
				hex = "0" + hex;
			}
			strValue += hex + " ";
		}
		log.info(Integer.toHexString(headerLength) 
				+ ": " + a + " 		:" + strValue);
		headerLength += value.length;
	}

	public byte[] getAttributeValue(Attribute a) {
		Integer key = a.getOffset();
		return attributes.get(key); 
	}

	public byte[] dump() {
		byte[] bytes = new byte[headerLength];

		byte[] sizeInByte = getAttributeValue(Attribute.FILE_SIZE);
		int lengAtt = ConvertUtility.byteArrayToInteger(
				sizeInByte, 
				ByteOrder.LITTLE_ENDIAN);

		log.info("Length Att: " + lengAtt);
		log.info("Data started at : "  + headerLength);
		int offset = 0;

		Set<Entry<Integer, byte[]>> entrySet = attributes.entrySet();
		Iterator<Entry<Integer, byte[]>> iterator = entrySet.iterator();
		while (iterator.hasNext()) {

			// Get the entry
			Entry<Integer, byte[]> entry = iterator.next();

			// Copy data
			byte[] data = entry.getValue();
			System.arraycopy(data, 0, bytes, offset, data.length);
			offset += data.length;
		}
		return bytes;
	}

}
