package fr.telecomParistech.dash.mpd;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Utility class, used to parse mpd file
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class MPDParser {
	private static final Logger LOGGER = 
			Logger.getLogger(MPDParser.class.getName());
	
	/**
	 * Create document from an input stream.
	 * @param fromInputStream input stream from which we create document.
	 * @return created document.
	 */
	private static Document createDocument(InputStream fromInputStream) {
		try {
			DocumentBuilderFactory dbFactory = 
					DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fromInputStream);
			doc.getDocumentElement().normalize();
			return doc;
		} catch (Exception e) {
			LOGGER.severe("Cannot create document");
			e.printStackTrace();
			System.err.println("Cannot create document");
			System.exit(1);
			
		}
		return null;
		
	}
	
	/**
	 * Create Document from mpd local path.
	 * @param fromMpdLocalPath mpd local path. 
	 * @return the created document.
	 */
	private static Document createDocument(String fromMpdLocalPath) {
		try {
			File xmlFile = new File(fromMpdLocalPath);
			DocumentBuilderFactory dbFactory = 
					DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
			return doc;
		} catch (Exception e) {
			LOGGER.severe("Cannot create document");
			e.printStackTrace();
			System.err.println("Cannot create document");
			System.exit(1);
			
		}
		return null;
	}
	
	/**
	 * Parse a Document to create MDP file
	 * @param doc doccument to parse
	 * @return mpd file
	 */
	private static MPD parseMPD(Document doc) {
		try {
			Element eMpd = doc.getDocumentElement();
			// Get list of Period
			NodeList nlPeriod = doc.getElementsByTagName("Period");
			LOGGER.info("---------@ Period tag -----------");
			
			// Pre-init variable for later use
			InitSegment initSegment = null;
			MediaSegment mediaSegment = null;
			SegmentList segmentList = null;
			Representation representation = null;
			AdaptationSet adaptationSet = null;
			Period period = null;
			MPD mpd = new MPD();
			
			// For each Period.
			for (int i = 0; i < nlPeriod.getLength(); i++) {
				Element ePeriod = (Element) nlPeriod.item(i);
				if (ePeriod == null) continue;
				
				// Get list of AdaptationSet
				NodeList nlAdaptationSet = 
						ePeriod.getElementsByTagName("AdaptationSet");
				
				LOGGER.info("---------@ AdaptationSet tag-----------");
				
				// For each AdaptationSet.
				for (int j = 0; j < nlAdaptationSet.getLength(); j++) {
					Element eAdaptationSet = (Element) nlAdaptationSet.item(j);
					if (eAdaptationSet == null) continue;
					
					// Get List of Representation
					NodeList nlRepresentation = eAdaptationSet
							.getElementsByTagName("Representation");
					LOGGER.info("---------Representation List-----------");
					
					// For each Representation
					for (int k = 0; k < nlRepresentation.getLength(); k++) {
						Element eRepresentation = 
								(Element) nlRepresentation.item(k);
						if(eRepresentation == null) {
							continue;
						}
						
						// Get SegmentList
						Element eSegmentList = (Element) eRepresentation
								.getElementsByTagName("SegmentList").item(0);
						// A representation must have a segment list
						if (eSegmentList == null) {
							continue; 
						}
						
						// In Segment list:
						// a. Get InitSegment
						Element eInitSegment = (Element) eSegmentList
								.getElementsByTagName("Initialization").item(0);
						// A segment list must have an init segment
						if (eInitSegment == null) continue; 
						initSegment = new InitSegment(
								eInitSegment.getAttribute("sourceURL"));
						
						// b. Get list of SegmentURL
						NodeList nlSegmentUrl = 
								eSegmentList.getElementsByTagName("SegmentURL");
						// And some media segments
						if (nlSegmentUrl == null) {
							continue; 
						}
						
						// Create new SegmentList
						segmentList = new SegmentList();
						for (int m = 0; m < nlSegmentUrl.getLength(); m ++) {
							// c. Get SegmentURL
							Element eSegmentUrl = (Element)nlSegmentUrl.item(m);
							if (eSegmentUrl == null) continue;
							
							String media = eSegmentUrl.getAttribute("media");
							// We'll create a new MediaSegment whose id started
							// from 1 instead of 0.
							mediaSegment = new MediaSegment(m + 1, media);
							segmentList.addMediaSegment(mediaSegment);
						}
						
						segmentList.setInitSegment(initSegment);
						segmentList.addAttribute("duration", 
								eSegmentList.getAttribute("duration"));
						
						// Create new representation
						representation = new Representation(k, segmentList);
						representation.addAttribute("mimeType", 
								eRepresentation.getAttribute("mimeType"));
						
						representation.addAttribute("codecs", 
								eRepresentation.getAttribute("codecs"));
						
						representation.addAttribute("width", 
								eRepresentation.getAttribute("width"));
						
						representation.addAttribute("height", 
								eRepresentation.getAttribute("height"));
						
						representation.addAttribute("bandwidth", 
								eRepresentation.getAttribute("bandwidth"));
					}
					
					// Create new AdaptationSet
					adaptationSet = new AdaptationSet(j);
					adaptationSet.addRepresentation(representation);
					
					Element eContentComponent = (Element) eAdaptationSet
							.getElementsByTagName("ContentComponent").item(0);
					adaptationSet.addAttribute("contentType", 
							eContentComponent.getAttribute("contentType"));
					
				}
				
				period = new Period(i);
				period.addAdaptationSet(adaptationSet);
				period.addAttribute("start", ePeriod.getAttribute("start"));
				period.addAttribute("duration", 
						ePeriod.getAttribute("duration"));

			}
			
			mpd.addPeriod(period);
			mpd.addAttribute("type", eMpd.getAttribute("type"));

//			log.info(mpd.toString());
			
			return mpd;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static MPD parseMPD(String fromMpdPath) {
		Document doc = createDocument(fromMpdPath);
		MPD mpdFile = parseMPD(doc);
		return mpdFile;
	}
	
	public static MPD parseMPD(InputStream fromInputStream) {
		Document doc = createDocument(fromInputStream);
		MPD mpdFile = parseMPD(doc);
		return mpdFile;
	}

	private static String getEnclosingDirectoryOf(String filePath) {
		int delimIndex = filePath.lastIndexOf('/');
		String dirUrl = filePath.substring(0, delimIndex);
		return dirUrl;
	}
	
}
