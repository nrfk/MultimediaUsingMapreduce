package fr.telecomParistech.mapreduce;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.tools.pipeline.Job2;
import com.google.appengine.tools.pipeline.Value;

import fr.telecomParistech.dash.mpd.AdaptationSet;
import fr.telecomParistech.dash.mpd.InitSegment;
import fr.telecomParistech.dash.mpd.MPD;
import fr.telecomParistech.dash.mpd.MPDParser;
import fr.telecomParistech.dash.mpd.MediaSegment;
import fr.telecomParistech.dash.mpd.Period;
import fr.telecomParistech.dash.mpd.Representation;
import fr.telecomParistech.dash.mpd.SegmentList;
import fr.telecomParistech.mp4parser.MP4Parser;

/**
 * This packet-private class (default modifier) is used to parse MPD file
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
/* packet-private */ class JobParsingMpd 
		extends Job2<List<Entity>, MPD, String> {
	private static final long serialVersionUID = 1070584585242415861L;
	private static final FileService fileService = 
			FileServiceFactory.getFileService();

	@Override
	public Value<List<Entity>> run(MPD mpd, String mpdLocation) {
		List<Entity> entityList = new ArrayList<Entity>();
		Entity entity = null;
		List<Period> periods = mpd.getAllPeriod();
		
		// Get file and dirUrl
		String dirUrl = getDirectoryUrl(mpdLocation);
		
		// For each Period
		for (Period period : periods) {
			List<AdaptationSet> adaptationSets = 
					period.getAllAdaptationSet();
			
			// For each AdaptationSet in Period
			for (AdaptationSet adaptationSet : adaptationSets) {
				List<Representation> representations = 
						adaptationSet.getAllRepresentation();
				
				// For each Representation in AdaptationSet
				for (Representation representation : representations) {
					SegmentList segmentList = 
							representation.getSegmentList();
					
					InitSegment initSegment = segmentList.getInitSegment();
					String initSegmentPath = 
							dirUrl + "/" + initSegment.getSourceURL();

					List<MediaSegment> mediaSegments = 
							segmentList.getAllMediaSegment();
					
					MP4Parser mp4Parser = new MP4Parser();
					
					 byte[] segmentData = null;
					try {
						URL initSegmentUrl = new URL(initSegmentPath);
						segmentData = 
								IOUtils.toByteArray(initSegmentUrl.openStream());
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
					String sps = mp4Parser.getSpsInHex(segmentData);
					String pps = mp4Parser.getPpsInHex(segmentData);
					int nalLengthSize = 
							mp4Parser.getNalLengthSize(segmentData);
					int videoTrackId = 
							mp4Parser.getVideoTrackBoxId(segmentData);
					
					String representationInfo = "";
					representationInfo += "id=" + 
								representation.getId() + ";";
					representationInfo += "width=" + 
								representation.getAttribute("width")+ ";";
					representationInfo += "height=" + 
								representation.getAttribute("height")+ ";";
					representationInfo += "bandwidth=" + 
								representation.getAttribute("bandwidth")+ ";";
					
					// For each media segment, create a new blob for storing 
					// image after.
					for (MediaSegment mediaSegment : mediaSegments) {
						Key key = KeyFactory.createKey(
								"MediaSegmentInfo",  mediaSegment.getId());
						entity = new Entity(key);
						
						entity.setProperty(
								"representationId", 
								representation.getId());
						entity.setProperty("sps", sps);
						entity.setProperty("pps", pps);
						entity.setProperty("nalLengthSize", nalLengthSize);
						entity.setProperty("videoTrackId", videoTrackId);
						entity.setProperty("representationInfo", 
								representationInfo);
						
						String relativeLocation = mediaSegment.getMedia();
						entity.setProperty("url", dirUrl + "/" + 
								relativeLocation);
						
						// Create a new Blob File, as a place holder for storing
						// image after.
						AppEngineFile file = null;
						while (file == null) {
							try {
								file = fileService
										.createNewBlobFile("image/bmp");
							} catch (IOException ignored) {
								// Exception will be ignored
							}
						}
						System.out.println(file.getFullPath());
						entity.setProperty("imageFullPath", file.getFullPath());
						entityList.add(entity);
					}
				}
			}
		}
		
		return immediate(entityList);
	}
	
	
	private String getDirectoryUrl(String fileUrl) {
		int delimIndex = fileUrl.lastIndexOf('/');
		String dirUrl = fileUrl.substring(0, delimIndex);
		return dirUrl;
	}
	
	private MPD createMpdFile(String mdpUrl) {
		URL url = null;
		MPD mpd = null;
		try {
			url = new URL(mdpUrl);
			InputStream inputStream = url.openStream();
			mpd = MPDParser.parseMPD(inputStream);
		} catch (MalformedURLException e) {
			// If url is malformed, just let mpd null
		} catch (IOException e) {
			e.printStackTrace();
		}
		return mpd;
	}
}
