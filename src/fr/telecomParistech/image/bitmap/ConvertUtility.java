package fr.telecomParistech.image.bitmap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * ConvertUtility, used to convert between differents number representation
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class ConvertUtility {
	private static final int INT_SIZE = 4;
	
	/**
	 * convert integer number to an byte array
	 * @param number
	 * @param endian
	 * @return byte array of the number.
	 */
	public static byte[] integerToByteArray(int number, ByteOrder endian) {
		ByteBuffer bytes = ByteBuffer.allocate(INT_SIZE);
		bytes.order(endian);
		bytes.putInt(number);
		return bytes.array();
	}
	
	/**
	 * Convert an byte array to the equivalent integer.
	 * @param bytes
	 * @param endian
	 * @return the equivalent integer of this byte array.
	 */
	public static int byteArrayToInteger(byte[] bytes, ByteOrder endian) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(INT_SIZE);
		byteBuffer.order(endian);
		byteBuffer.put(bytes);
		byteBuffer.rewind();
		int result = byteBuffer.getInt();
		return result;
	}
	
	/**
	 * Convert an byte array to the equivalent integer. Big endian is used 
	 * as a default value
	 * @param bytes
	 * @return the equivalent integer of this byte array.
	 */
	public static int byteArrayToInteger(byte[] bytes) {
		return byteArrayToInteger(bytes, ByteOrder.BIG_ENDIAN);
	}
	
	/**
	 * Get number in a specific Radix
	 * @param number in Dec
	 * @param radix 
	 * @return number in radix-based.
	 */
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
	
	/**
	 * Convert an byte array to a hex String.
	 * @param value input byte array.
	 * @return equivalent hex string.
	 */
	public static String byteArrayToHext(byte[] value) {
		StringBuffer strBuffer = new StringBuffer();
		for (byte b : value) {
			String hex = Integer.toHexString(b & 0xFF);
			if (hex.length() == 1) {
				hex = "0" + hex;
			}
			strBuffer.append(hex + " ");
		}
		return strBuffer.toString();
	}

	// Test
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
