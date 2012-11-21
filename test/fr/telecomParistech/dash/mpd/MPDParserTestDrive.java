package fr.telecomParistech.dash.mpd;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import fr.telecomParistech.dash.mpd.InitSegment;
import fr.telecomParistech.dash.mpd.MPD;
import fr.telecomParistech.dash.mpd.MPDParser;
import fr.telecomParistech.dash.mpd.MediaSegment;
import fr.telecomParistech.dash.mpd.SegmentList;

public class MPDParserTestDrive {
	private static String filePath = "test/resources/test.mpd";
	private static MPD mpd;
	
	@BeforeClass
	public static void oneTimeSetUp() 
			throws IOException, SAXException, ParserConfigurationException {
		File file = new File(filePath);
		
		mpd = MPDParser.parseMPD(new FileInputStream(file));
	}
	
	@Test
	public void testParsedData() {
		SegmentList segmentList = mpd
				.getAllPeriod().get(0)
				.getAllAdaptationSet().get(0)
				.getAllRepresentation().get(0)
				.getSegmentList();
		
		InitSegment initSegment = segmentList.getInitSegment();
		assertEquals("SourceUrl init segment", 
				"count-video_dash.mp4", 
				initSegment.getSourceURL());
		
		int i = 0;
		for (MediaSegment segment : segmentList.getAllMediaSegment()) {
			i++;
			assertEquals("Segment Url #" + i, 
					"seg" + i + ".m4s", 
					segment.getMedia());
		}
	}

}
