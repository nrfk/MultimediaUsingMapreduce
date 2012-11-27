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
		if (bytes.length > 4) {
			return -1;
		}
		
		ByteBuffer byteBuffer = ByteBuffer.allocate(INT_SIZE);
		byteBuffer.order(endian);
		
		// Convert so that 00 19 (big endian) becomes 00 00 00 19
		// and 00 19 (little endian) becomes 00 19 00 00
		
		int length = bytes.length;
		if (endian == ByteOrder.BIG_ENDIAN) {
			// Padding
			for (int i = length; i < INT_SIZE; i++) {
				byteBuffer.put( (byte) 0x00 );
			}
			// add real value
			byteBuffer.put(bytes);
		} else { // little endian
			byteBuffer.put(bytes);
			// Padding
			for (int i = length; i < INT_SIZE; i++) {
				byteBuffer.put( (byte) 0x00 );
			}
		}
		
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
	
	public static byte[] hexStringToByteArray(String hex) {
		hex = hex.replaceAll("\\s+", "");
		// align
		if (hex.length() % 2 == 1) {
			hex = "0" + hex;
		}
		
	    int len = hex.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
	                             + Character.digit(hex.charAt(i+1), 16));
	    }
	    return data;
	}
	
	
	public static void main(String[] args) {
		String hex = "cafe babe";
		byte[] bytes = ConvertUtility.hexStringToByteArray(hex);
	}
	
}





