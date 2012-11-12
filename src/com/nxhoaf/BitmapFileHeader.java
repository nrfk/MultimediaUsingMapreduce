package com.nxhoaf;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;


public class BitmapFileHeader {
	public enum Attribute {
		SIGNATURE 					(0x00, 0x02),
		FILE_SIZE 					(0x02, 0x04),
		RESERVED_1 					(0x06, 0x02),
		RESERVED_2 					(0x08, 0x02),
		FILE_OFFSET_TO_PIXEL_ARRAY 	(0x0A, 0x04);
	
		private final int offset;
		private final int length;
		
		
		private Attribute(int offset, int length) {
			this.offset = offset;
			this.length = length;
		}
		
		public String getLength(Radix radix) {
			return Radix.getNumber(length,radix);
		}
		
		public int getLength() {
			return length;
		}
		
		public String getOffset(Radix radix) {
			return Radix.getNumber(offset, radix);
		}
		
		public int getOffset() {
			return offset;
		}
		
	}
	
	private Map<Integer, ByteBuffer> attributes;
	private int headerLength;
	
	public BitmapFileHeader() {
		attributes = new TreeMap<Integer, ByteBuffer>();
		headerLength = 0;
	}
	
	public void setAttribute(Attribute a, ByteBuffer value) {
		// Save value, use it offset as key
		attributes.put(a.getOffset(), value);
		headerLength += value.array().length;
	}
	
	public ByteBuffer getAttribute(Attribute a) {
		Integer key = a.getOffset();
		return attributes.get(key); 
	}
	
	
	public byte[] dump() {
		byte[] bytes = new byte[headerLength];
		int offset = 0;
		
		Set<Entry<Integer, ByteBuffer>> entrySet = attributes.entrySet();
		Iterator<Entry<Integer, ByteBuffer>> iterator = entrySet.iterator();
		while (iterator.hasNext()) {
			
			// Get the entry
			Entry<Integer, ByteBuffer> entry = iterator.next();
			
			// Copy data
			byte[] data = entry.getValue().array();
			System.arraycopy(data, 0, bytes, offset, data.length);
			offset += data.length;
		}
		return bytes;
	}
	
	
	
//	private String intToHex(int number) {
//		return Integer.toHexString(number);
//	}
//	
//	private int hexToInt(String number) {
//		return Integer.parseInt(number, 16);
//	}
	
	
	public static void main(String[] args) {
		BitmapFileHeader bitmapFileHeader = new BitmapFileHeader();
		Attribute a = BitmapFileHeader.Attribute.FILE_OFFSET_TO_PIXEL_ARRAY;
		System.out.println(a + "  " + a.getLength(Radix.HEX) + "   " + a.getOffset(Radix.HEX));
	}

}
