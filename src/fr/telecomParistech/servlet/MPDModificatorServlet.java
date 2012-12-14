package fr.telecomParistech.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;

import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;

import fr.telecomParistech.dash.mpd.AdaptationSet;
import fr.telecomParistech.dash.mpd.MPD;
import fr.telecomParistech.dash.mpd.MPDParser;
import fr.telecomParistech.dash.mpd.MediaSegment;
import fr.telecomParistech.dash.mpd.Period;
import fr.telecomParistech.dash.mpd.Representation;
import fr.telecomParistech.dash.mpd.SegmentList;


public class MPDModificatorServlet extends HttpServlet {
	private static final long serialVersionUID = 2235198614628917491L;
	private static final String BLOBSTORE_READER_SERVICE = 
			"/blobstore-reader-servlet";

	// Configuration-related properties
	private static final Logger log;
	private static final String CONFIG_FILE="WEB-INF/mapreduce-config.xml";
	private static final XMLConfiguration mapreduceConfig;
	// GEA services
	private static final FileService fileService = 
			FileServiceFactory.getFileService(); 

	// Counter, help us to keep track of each map task.
	// static initializer 
	static {
		log = Logger.getLogger(MPDModificatorServlet.class.getName());

		// First, set log level in order to display log info during this 
		// static initializer. It's also the default log level
		log.setLevel(Level.INFO);
		XMLConfiguration tmpConfig = null; 
		try {
			tmpConfig = new XMLConfiguration(CONFIG_FILE);
		} catch (Exception e) {
			log.severe("Couldn't read config file: " + CONFIG_FILE);
			System.exit(1);
		} finally {
			mapreduceConfig = tmpConfig;
			if (mapreduceConfig != null) {
				String logLevel = 
						mapreduceConfig.getString("log.level-mapper", "INFO");
				log.setLevel(Level.parse(logLevel));

			} 
		}
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

	@Override protected void doPost(HttpServletRequest request, 
			HttpServletResponse resp)
					throws ServletException, IOException {

		// Log the start time
		long startedTime = System.nanoTime();
		log.info("MPDParser started at " + startedTime + " (ABSOLUTE TIME)");

		// ----- Create MPD text-representation and MPD object ----------

		String senderUrl = request.getParameter("senderUrl");
		if (senderUrl == null) {
			throw new IllegalStateException("sender url is null");
		}
		URL url = new URL(senderUrl);
		// MPD text
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader reader = 
				new BufferedReader(new InputStreamReader(url.openStream()));
		String line = null;
		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line + "\n");
		}
		reader.close();

		String mpdTextFormat = stringBuilder.toString();
		// MPD object
		MPD mpd = MPDParser.parseMPD(url);

		// Ok, now parse mpd object
		// ------- Ok, now parse it ------------
		List<Period> periods = mpd.getAllPeriod();

		// Get file and dirUrl
		String fileUrl = request.getParameter("senderUrl");
		String dirUrl = getDirectoryUrl(fileUrl);

		// Store all segment full path
		List<String> fullPathList = new ArrayList<String>();
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

					List<MediaSegment> mediaSegments =
							segmentList.getAllMediaSegment();

					// For each media segment, create a new blob for storing
					// image after.

					for (MediaSegment mediaSegment : mediaSegments) {

						// Create a new Blob File, as a place holder for storing
						// image after.
						AppEngineFile file = null;
						while (file == null) {
							try {
								file = fileService
										.createNewBlobFile("image/bmp");
								// Add this path to segmentFullPaths
								fullPathList.add(file.getFullPath());
							} catch (IOException ignored) {
								// Exception will be ignored
							}
						}

						String segmentUrlStr = dirUrl + "/" + mediaSegment.getMedia();
						URL segmentUrl = new URL(segmentUrlStr);
						byte[] segmentData = 
								IOUtils.toByteArray(segmentUrl.openStream());

						// Create writer to write to it
						boolean lock = true;
						FileWriteChannel writeChannel = 
								fileService.openWriteChannel(file, lock);
						writeChannel.write(ByteBuffer.wrap(segmentData));
						writeChannel.close();

						String newSegmentUrl = 
								BLOBSTORE_READER_SERVICE + "?" + 
										"blobPath=" + file.getFullPath();
						
						log.info("upload finished: " + file.getFullPath());
						log.info("old url: " + mediaSegment.getMedia());
						log.info("new url: " + newSegmentUrl);
						
						mpdTextFormat.replace(mediaSegment.getMedia(), 
								newSegmentUrl);

						//							entity.setProperty("imageFullPath", file.getFullPath());
						//							segmentCounter++;
						//							pool.put(entity);
					}
				}
			}
		}
		
		PrintWriter pw = resp.getWriter();
		pw.write(mpdTextFormat);
		pw.close();
		long endTime = System.nanoTime();
		log.info("Mpd modification done at:  "  + 
				endTime + (" (ABSOLUTE TIME)"));
	}
}































