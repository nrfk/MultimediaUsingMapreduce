package fr.telecomParistech.mapreduce;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.tools.pipeline.Job1;
import com.google.appengine.tools.pipeline.Value;

import fr.telecomParistech.dash.mpd.AdaptationSet;
import fr.telecomParistech.dash.mpd.InitSegment;
import fr.telecomParistech.dash.mpd.MPD;
import fr.telecomParistech.dash.mpd.MediaSegment;
import fr.telecomParistech.dash.mpd.Period;
import fr.telecomParistech.dash.mpd.Representation;
import fr.telecomParistech.dash.mpd.SegmentList;
import fr.telecomParistech.dash.util.MPDParser;

public class ParseMpdFileJob extends Job1<String, String>  {
	private static final long serialVersionUID = -8220916617355833630L;
	private static final DatastoreService dataStore = 
			DatastoreServiceFactory.getDatastoreService();
	
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
	
	@Override
	public Value<String> run(String mpdUrl) {
		MPD mpd = createMpdFile(mpdUrl);
		if (mpd == null) {
			return immediate("MalformedURLException");
		}
		
		Entity entity = null;

		List<Period> periods = mpd.getAllPeriod();
		
		
		// Get file and dirUrl
		String dirUrl = getDirectoryUrl(mpdUrl);
		// For each Period
		for (Period period : periods) {
			List<AdaptationSet> adaptationSets = period.getAllAdaptationSet();

			// For each AdaptationSet in Period
			for (AdaptationSet adaptationSet : adaptationSets) {
				List<Representation> representations = 
						adaptationSet.getAllRepresentation();

				// For each Representation in AdaptationSet
				for (Representation representation : representations) {
					SegmentList segmentList = representation.getSegmentList();
					
					InitSegment initSegment = segmentList.getInitSegment();
					String initSegmentUrl = 
							dirUrl + "/" + initSegment.getSourceURL();

					List<MediaSegment> mediaSegments = 
							segmentList.getAllMediaSegment();
					
					// Create a Blob Store for each init segment of 
					// a specific representation
					FileService fileService = 
							FileServiceFactory.getFileService();
					try {
						AppEngineFile file = 
								fileService.createNewBlobFile("video/mp4");
						boolean lock = true;
						FileWriteChannel writeChannel = 
								fileService.openWriteChannel(file, lock);

						URL url = new URL(initSegmentUrl);
						BufferedInputStream bufInput = 
								new BufferedInputStream(url.openStream());
						byte[] byteChunk = new byte[4096];
						int n;
						long size = 0;
						while ((n = bufInput.read(byteChunk)) > 0) {
							size += n;
							writeChannel.write(ByteBuffer.wrap(byteChunk, 0, n));
						}
						writeChannel.closeFinally();
						
						String representationInfo = "";
						representationInfo += "id=" + 
									representation.getId() + ";";
						representationInfo += "width=" + 
									representation.getAttribute("width")+ ";";
						representationInfo += "height=" + 
									representation.getAttribute("height")+ ";";
						representationInfo += "bandwidth=" + 
									representation.getAttribute("bandwidth")+ ";";
						
						for (MediaSegment mediaSegment : mediaSegments) {
							Key key = KeyFactory.createKey(
									"MediaSegmentInfo",  mediaSegment.getId());
							entity = new Entity(key);
							
							entity.setProperty("id", mediaSegment.getId());
							entity.setProperty("initSegmentUrl", 
									file.getFullPath());
							entity.setProperty("initSegmentSize", size);
							entity.setProperty("representationInfo", 
									representationInfo);
							
							String relativeLocation = mediaSegment.getMedia();
							entity.setProperty("url", dirUrl + "/" + 
									relativeLocation);
							
							dataStore.put(entity);
						}
					} catch (IOException e) {
						return immediate("IOException");
					}
				}
			}
		}
		return immediate("Done");
	}
	
//	public static class DowloadMpdSubJob extends Job1<MPD, String> {
//		private static final long serialVersionUID = -4689080506927094004L;
//
//		@Override
//		public Value<MPD> run(String mpdUrl) {
//			URL url = null;
//			MPD mpd = null;
//			
//			try {
//				url = new URL(mpdUrl);
//				InputStream inputStream = url.openStream();
//				mpd = MPDParser.parseMPD(inputStream);
//			} catch (MalformedURLException e) {
//				
//			} catch (IOException e) {
//				
//			}
//			return immediate(mpd);
//		}
//	}
//	
//	public static class parseMpdSubJob extends Job1<String, MPD> {
//		private static final long serialVersionUID = 6333575162271946044L;
//
//		@Override
//		public Value<String> run(MPD param1) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//	}
	
	
}
