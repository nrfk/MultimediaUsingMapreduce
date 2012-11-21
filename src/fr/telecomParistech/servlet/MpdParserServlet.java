package fr.telecomParistech.servlet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;

import fr.telecomParistech.dash.mpd.AdaptationSet;
import fr.telecomParistech.dash.mpd.InitSegment;
import fr.telecomParistech.dash.mpd.MPD;
import fr.telecomParistech.dash.mpd.MPDParser;
import fr.telecomParistech.dash.mpd.MediaSegment;
import fr.telecomParistech.dash.mpd.Period;
import fr.telecomParistech.dash.mpd.Representation;
import fr.telecomParistech.dash.mpd.SegmentList;

/**
 * MpdParserServlet is used to parse .mpd file coming from client side
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class MpdParserServlet extends HttpServlet {
	private static final long serialVersionUID = 9114247753565601970L;
	private static final Logger log; 
	private static final DatastoreService dataStore; 
	private static final FileService fileService; 
	
	// Init
	static {
		dataStore = DatastoreServiceFactory.getDatastoreService();;
		log = Logger.getLogger(MpdParserServlet.class.getName());
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
		log.info("mpd file created...");
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
		log.finest(mpd.toString());
		
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
					String initSegmentUrl = 
							dirUrl + "/" + initSegment.getSourceURL();

					List<MediaSegment> mediaSegments = 
							segmentList.getAllMediaSegment();
					
					// Create a Blob Store for each init segment of 
					// a specific representation. Use Byte ArrayOutputStream
					// to avoid datastore to reduce the number of read operation
					// http://stackoverflow.com/questions/8052886
					// /reduce-datastore-read-operation
					ByteArrayOutputStream byteArrayOut = 
							new ByteArrayOutputStream();
					AppEngineFile file = 
							fileService.createNewBlobFile("video/mp4");
					boolean lock = true;
					FileWriteChannel writeChannel = 
							fileService.openWriteChannel(file, lock);

					URL url = new URL(initSegmentUrl);
					BufferedInputStream bufInput = 
							new BufferedInputStream(url.openStream());
					byte[] buffer = new byte[4096];
					int n;
					long size = 0;
					while ((n = bufInput.read(buffer)) > 0) {
						size += n;
						byteArrayOut.write(buffer, 0, n);
					}
					
					buffer = byteArrayOut.toByteArray();
					writeChannel.write(ByteBuffer.
							wrap(buffer, 0, buffer.length));
					writeChannel.closeFinally();
					// ---------------- Done create Blob Store -----------------

					// Create a new Entity to store all the information
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
						Key key = KeyFactory.createKey("MediaSegmentInfo", 
								mediaSegment.getId());
						entity = new Entity(key);
						
						entity.setProperty("id", mediaSegment.getId());
						entity.setProperty("initSegmentUrl",file.getFullPath());
						entity.setProperty("initSegmentSize", size);
						entity.setProperty("representationInfo", 
								representationInfo);
						
						String relativeLocation = mediaSegment.getMedia();
						entity.setProperty("url", dirUrl + "/" + 
								relativeLocation);
						
						dataStore.put(entity);
					}
				}
			}
		}
		response.sendRedirect("/process-dash-using-mapreduce");
	}
}
