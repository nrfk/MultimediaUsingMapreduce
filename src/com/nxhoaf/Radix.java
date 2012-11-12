package com.nxhoaf;

public enum Radix {
	BINARY,
	HEX,
	OCTAL;
	
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
}
