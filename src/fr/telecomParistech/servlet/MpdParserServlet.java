package fr.telecomParistech.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.XMLConfiguration;
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

/**
 * This class receives requests which contain mpd file. It then parse the file 
 * and forward parsed data to another servlet in order to do some post
 * processing
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class MPDParserServlet extends HttpServlet {
	private static final long serialVersionUID = 9114247753565601970L;
	// Use FileService to work with file in GAE
	private static final FileService fileService = 
			FileServiceFactory.getFileService();

	// use DatastoreMutationPool to persist entity, blob in batch, thus, 
	// decrease the number of read/write operation
	private static final transient DatastoreMutationPool pool =
			DatastoreMutationPool.forManualFlushing();

	// Configuration-related properties
	private static final Logger log;
	private static final String CONFIG_FILE="WEB-INF/mapreduce-config.xml";
	private static final XMLConfiguration mapreduceConfig;
	private static final TimeUnit timeUnit;
	// static initializer 
	static {
		log = Logger.getLogger(DashMpdParserServlet.class.getName());
		
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
				String timeUnitStr = mapreduceConfig
						.getString("mapreduce.time-unit","SECONDS");
				timeUnit = TimeUnit.valueOf(timeUnitStr);

				String logLevel = 
						mapreduceConfig.getString("log.level-parser", "INFO");
				log.setLevel(Level.parse(logLevel));

			} else {
				timeUnit = TimeUnit.SECONDS;
			}
		}
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
		
		long startDownTime = System.nanoTime();
		log.info("MPDParser starts dowload mpd file: "+ senderUrl +"at: " + 
				startDownTime + (" (ABSULUTE TIME)"));
		try {
			url = new URL(senderUrl);
			InputStream inputStream = url.openStream();
			mpd = MPDParser.parseMPD(inputStream);
		} catch (MalformedURLException e) {
			request.setAttribute("status", "malformedUrl");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			long endDownTime = System.nanoTime();
			log.info("MPDParser end dowload mpd file: "+ senderUrl +"at: "  + 
					endDownTime + (" (ABSULUTE TIME)"));
			long downTime = endDownTime - startDownTime;
			log.info("MPDParser downloads file: "+ senderUrl +"in: "  + 
					timeUnit.convert(downTime, TimeUnit.NANOSECONDS) + 
					" ("+ timeUnit +")");
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
		// Started time, il'll also be used later as a label for all of entities
		// in this season. It helps the mapper function to process only entities
		// generated in this session but not previous ones.
		long startedTime = System.nanoTime();
		log.info("MPDParser started at " + startedTime + " (ABSULUTE TIME)");
		int segmentCounter = 0;

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
						log.severe("MalformedURLException, url: " 
								+ initSegmentPath);
						e.printStackTrace();
						System.exit(1);
					} catch (IOException e) {
						log.severe("IOException occurs during parsing...");
						e.printStackTrace();
						System.exit(1);
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
								// use startTime as an id for this session
								"MediaSegmentInfo" + startedTime, 
								mediaSegment.getId());
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
								// Add this path to segmentFullPaths
								fullPathList.add(file.getFullPath());
							} catch (IOException ignored) {
								// Exception will be ignored
							}
						}
						entity.setProperty("imageFullPath", file.getFullPath());
						segmentCounter++;
						pool.put(entity);
					}
				}
			}
		}
		pool.flush();

		// Log the execution time
		long endTime = System.nanoTime();
		log.info("MPDParser ended at " + endTime + " (ABSULUTE TIME)");
		long elapsedTime = endTime - startedTime;
		// Convert from nano second to mini second
		log.info("MPDParser done in: " + 
				timeUnit.convert(elapsedTime, TimeUnit.NANOSECONDS) + 
				" ("+ timeUnit +")");
		
		
		// Dispatcher request to another servlet
		
		// Save full path list and number of media segment for later use.
		request.setAttribute("fullPathList", fullPathList);
		request.setAttribute("segmentCounter", segmentCounter);
		
		// The mapreduce function read entity as its input, and as we just want
		// to read all entities in this session by mapper function, but not 
		// entities used by previous mapper function, we create an session id
		// which is a unique time stamp to distinguish between these entities
		request.setAttribute("sessionId", "" + startedTime ); // String form
		
		String dispatchedLink = "/extract-image";
		log.info("redirect to: " + dispatchedLink);
		RequestDispatcher dispatcher = 
				request.getRequestDispatcher(dispatchedLink);
		
		dispatcher.forward(request, response);
		
	}

}