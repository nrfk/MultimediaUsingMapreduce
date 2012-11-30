package fr.telecomParistech.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.tools.mapreduce.DatastoreMutationPool;

import fr.telecomParistech.dash.mpd.AdaptationSet;
import fr.telecomParistech.dash.mpd.InitSegment;
import fr.telecomParistech.dash.mpd.MPD;
import fr.telecomParistech.dash.mpd.MPDParser;
import fr.telecomParistech.dash.mpd.MediaSegment;
import fr.telecomParistech.dash.mpd.Period;
import fr.telecomParistech.dash.mpd.Representation;
import fr.telecomParistech.dash.mpd.SegmentList;
import fr.telecomParistech.parser.MP4Parser;

public class MPDParserServlet extends HttpServlet {
	private static final long serialVersionUID = 9114247753565601970L;
	private static final Logger LOGGER; 
	private static final FileService fileService; 
	private static final transient DatastoreMutationPool pool =
			DatastoreMutationPool.forManualFlushing();
	
	// Init
	static {
		LOGGER = Logger.getLogger(DashMpdParserServlet.class.getName());
		fileService = FileServiceFactory.getFileService();
	}
			
	/**
	 * Create mpdFile from raw data inside the request		
	 * @param request request which contains mpd's raw data 
	 * @return a reference to MPD object
	 */
	private MPD createMpdFile(HttpServletRequest request) {
		String senderUrl = request.getParameter("senderUrl");
		URL url = null;
		MPD mpd = null;
		try {
			url = new URL(senderUrl);
			InputStream inputStream = url.openStream();
			mpd = MPDParser.parseMPD(inputStream);
		} catch (MalformedURLException e) {
			request.setAttribute("status", "malformedUrl");
		} catch (IOException e) {
			e.printStackTrace();
		}
		LOGGER.info("mpd file created...");
		return mpd;
	}
	
	/**
	 * Get directory which contains mpd file from the fileUrl, for e.x, if 
	 * the fileUrl is http://link/to/file.txt, the return will be "to" folder
	 * @param fileUrl fileUrl to get directory
	 * @return the folder containing the file
	 */
	private String getDirectoryUrl(String fileUrl) {
		int delimIndex = fileUrl.lastIndexOf('/');
		String dirUrl = fileUrl.substring(0, delimIndex);
		return dirUrl;
	}
	
	@Override
	protected void doPost(HttpServletRequest request, 
			HttpServletResponse response)
			throws ServletException, IOException {
		
		// ----- Create MPD file ----------
		MPD mpd = createMpdFile(request);
		if (mpd == null) {
			response.sendRedirect("/process-dash.jsp?status=" + 
					request.getAttribute("status"));
			return;
		}
		LOGGER.finest(mpd.toString());
		
		// ------- Ok, now parse it ------------
		Entity entity = null;
		List<Period> periods = mpd.getAllPeriod();
		
		// Get file and dirUrl
		String fileUrl = request.getParameter("senderUrl");
		String dirUrl = getDirectoryUrl(fileUrl);
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
						entity.setProperty("imageFullPath", file.getFullPath());
						
						pool.put(entity);
					}
				}
			}
		}
		pool.flush();
		response.sendRedirect("/extract-image-processing.jsp");
	}

}
