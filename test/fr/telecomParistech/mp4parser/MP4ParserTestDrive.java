package fr.telecomParistech.mp4parser;

import static org.junit.Assert.*;

 import java.util.List;

import org.junit.Test;

public class MP4ParserTestDrive {
	private MP4Parser mp4Parser = new MP4Parser();
	private String filePath = "test/resources/sample_iPod.m4v";
	
	@Test
	public void testGetSampleQuantity() {
		long sampleQuantity = mp4Parser.getSampleQuantity(filePath);
		assertEquals("Sample quantity", 855, sampleQuantity);
	}
	
	@Test
	public void testGetSpsInHex() {
		String expectedSps = 
				"27   42   E0   0D   A9	" +
				"18   28   3F   60   0D	" +
				"41   80   41   AD   B7	" +
				"A0   2F   01   E9   7B	" +
				"DF   01";
		
		String resultSps = mp4Parser.getSpsInHex(filePath);
		
		// Remove all duplicate spaces for faciliting test
		expectedSps = expectedSps.replaceAll("\\s", "");
		
		resultSps = resultSps.toUpperCase();
		resultSps = resultSps.replaceAll("\\s", "");
		
		assertEquals("SPS in hex", expectedSps, resultSps);
	}
	
	@Test
	public void testGetPpsInHex() {
		String expectedSps = "28   CE   09   88";
		
		String resultSps = mp4Parser.getPpsInHex(filePath);
		
		// Remove all duplicate spaces for faciliting test
		expectedSps = expectedSps.replaceAll("\\s", "");
		
		resultSps = resultSps.toUpperCase();
		resultSps = resultSps.replaceAll("\\s", "");
		
		assertEquals("PPS in hex", expectedSps, resultSps);
	}
	
	@Test
	public void testGetISamples() {
		List<Long> isampleList = mp4Parser.getISamples(filePath);
		long[] iSamples = new long[isampleList.size()];
		for (int i = 0; i < isampleList.size(); i++) {
			iSamples[i] = isampleList.get(i);
		}
		
		long[] expexted = {
				1, 90, 225, 375, 525, 675, 752
		};
		
		assertArrayEquals("ISample List",expexted, iSamples);
	}
	
}
