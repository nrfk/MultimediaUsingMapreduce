package fr.telecomParistech.image.bitmap;

import static org.junit.Assert.*;

import java.nio.ByteOrder;

import org.junit.Ignore;
import org.junit.Test;

public class ConvertUtilityTestDrive {

	
	@Test
	public void testIntegerToByteArray() {
		int number = 0x11FF;
		
		// Big Endian
		byte[] bNumber = 
				ConvertUtility.integerToByteArray(number, ByteOrder.BIG_ENDIAN); 
		assertArrayEquals(
				"Big Endian", 
				new byte[] {
						(byte) 0x00,
						(byte) 0x00,
						(byte) 0x11,
						(byte) 0xFF
				}, 
				bNumber);
		
		
		// Little Endian
		bNumber = ConvertUtility
				.integerToByteArray(number, ByteOrder.LITTLE_ENDIAN); 
		assertArrayEquals(
				"Little Endian", 
				new byte[] {
						(byte) 0xFF,
						(byte) 0x11,
						(byte) 0x00,
						(byte) 0x00,
				}, 
				bNumber);
	}

	@Test
	public void testByteArrayToInteger() {
		byte[] bigEndian  = {(byte) 0x11, (byte) 0xFF};
		byte[] littleEndian  = {(byte) 0xFF, (byte) 0x11};
		
		assertEquals(
				"Big Endian", 
				4607, 
				ConvertUtility.byteArrayToInteger(
						bigEndian, ByteOrder.BIG_ENDIAN));
		
		assertEquals(
				"Little Endian", 
				4607, 
				ConvertUtility.byteArrayToInteger(
						littleEndian, ByteOrder.LITTLE_ENDIAN));
		
	}


	@Test
	public void testHextToByteArray() {
		String hex = "cafe babe";
		byte[] expected = new byte[] {
				(byte) Integer.parseInt("11001010", 2),
				(byte) Integer.parseInt("11111110", 2),
				(byte) Integer.parseInt("10111010", 2),
				(byte) Integer.parseInt("10111110", 2),
		};
		assertArrayEquals(
				"Hex To Byte Array", 
				expected,
				ConvertUtility.hexStringToByteArray(hex));
	}

}
