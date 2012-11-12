package com.nxhoaf;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ConvertUtility {
	private static final int BITS_PER_BYTE = 8;
	private static final int INT_SIZE = 4;
	
	public static byte[] integerToByteArray(int number, ByteOrder endian) {
		ByteBuffer bytes = ByteBuffer.allocate(INT_SIZE);
		bytes.order(endian);
		bytes.putInt(number);
		return bytes.array();
	}
	
	public static int byteArrayToInteger(byte[] bytes, ByteOrder endian) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(INT_SIZE);
		byteBuffer.order(endian);
		byteBuffer.put(bytes);
		byteBuffer.rewind();
		int result = byteBuffer.getInt();
		return result;
	}
	
	public static String getNumber(int number, Radix radix) { 
		switch (radix) {
			case BINARY:
				return Integer.toBinaryString(number);
			case OCTAL:
				return Integer.toOctalString(number);
			case HEX:
				return Integer.toHexString(number);
			default: 
				return String.valueOf(number);
		}
	}
	
	public static void main(String[] args) {
		int number = 0x0f1a6674;
		byte[] bytes = integerToByteArray(number, ByteOrder.BIG_ENDIAN);
		
		for (int i = 0; i < bytes.length; i++) {
			System.out.print(getNumber(bytes[i], Radix.HEX) + "   ");
		}
		
		System.out.println("\n");
		
		bytes = integerToByteArray(number, ByteOrder.LITTLE_ENDIAN);
		
		for (int i = 0; i < bytes.length; i++) {
			System.out.print(getNumber(bytes[i], Radix.HEX) + "   ");
		}
		
		System.out.println("\n");
		
		
		byte[] bigEndian = {0x00, 0x0f, 0x42, 0x40};
		System.out.println(byteArrayToInteger(bigEndian, ByteOrder.BIG_ENDIAN));
		
		byte[] littleEndian = {0x40, 0x42, 0x0f, 0x00};
		System.out.println(byteArrayToInteger(littleEndian, ByteOrder.LITTLE_ENDIAN));
	}
}
