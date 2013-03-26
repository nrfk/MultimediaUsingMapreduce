package fr.telecomParistech.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
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
import com.google.appengine.api.files.FileWriteChannel;
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
 * processing.
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class MPDParserColorServlet extends HttpServlet {
	private enum ProcessType {
		MODIFY_MPD,
		EXTRACT_IMAGE, 
		CHANGE_COLOR
	}

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
	private MPD createMpdFile(URL mpdLocation) {
		if (mpdLocation == null) {
			throw new NullPointerException("mpdLocation is null");
		}
		MPD mpd = null;

		long startDownTime = System.nanoTime();
		log.info("MPDParser creation started at: " + 
				startDownTime + (" (ABSOLUTE TIME)"));
		try {
			InputStream inputStream = mpdLocation.openStream();
			mpd = MPDParser.parseMPD(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			long endDownTime = System.nanoTime();
			log.info("MPDParser, mpd creation done at:  "  + 
					endDownTime + (" (ABSOLUTE TIME)"));
			long downTime = endDownTime - startDownTime;
			log.info("MPDParser, mpd creation time: "  + 
					timeUnit.convert(downTime, TimeUnit.NANOSECONDS) + 
					" ("+ timeUnit +")");
		}
		log.info("mpd file created...");
		return mpd;
	}

	/**
	 * Save MPD File to Blobstore for later use. 
	 * @param mpdLocation location of mpd file
	 * @return full path to the blobstore file containing this mpd file 
	 * @throws IOException
	 */
	private String saveMpdFileToBlobstore(URL mpdLocation) throws IOException {
		byte[] data = IOUtils.toByteArray(mpdLocation.openConnection());
		
		// Ok, now save to blob store
		// Create Blob Store for each file
		AppEngineFile file = 
				fileService.createNewBlobFile("application/octet-stream");
		// Create writer to write to it
		boolean lock = true;
		FileWriteChannel writeChannel = 
				fileService.openWriteChannel(file, lock);
		writeChannel.write(ByteBuffer.wrap(data));
		writeChannel.closeFinally();

		return file.getFullPath();
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
		log.info("MPDParser started at " + startedTime + " (ABSOLUTE TIME)");
		int segmentCounter = 0;

		// ----- Create MPD file ----------
		String senderUrl = request.getParameter("senderUrl");
		URL url = new URL(senderUrl);
		MPD mpd = createMpdFile(url);
		if (mpd == null) {
			throw new NullPointerException("Cannot create Mpd file.");
		}
		
		String processTypeStr = request.getParameter("processType");
		if (processTypeStr == null || processTypeStr == "") {
			processTypeStr = "EXTRACT_IMAGE"; // Default value
		}
		ProcessType processType = ProcessType.valueOf(processTypeStr);
		// Save MPD if required
//		String mpdPath = null;
//		if (processType == ProcessType.MODIFY_MPD) {
//			mpdPath = saveMpdFileToBlobstore(url);
//		}
		
		// ------- Ok, now parse it ------------
		Entity entity = null;

		// Store a list of image's url
		List<String> imageList = new ArrayList<String>();
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
						entity.setProperty("host", dirUrl);
						entity.setProperty("url", relativeLocation);
						
						switch (processType) {
						case EXTRACT_IMAGE:
							entity.setProperty("processType", "EXTRACT_IMAGE");
							break;

						case MODIFY_MPD:
							entity.setProperty("processType", "MODIFY_MPD");
							break;
						
						case CHANGE_COLOR:
							entity.setProperty("processType", "CHANGE_COLOR");
							break;

						default:
							throw new UnsupportedOperationException(processType + 
									" is unsupported by the underlying platform");
						}
						
						if (processType == ProcessType.MODIFY_MPD) {
							entity.setProperty("mpdPath", senderUrl);
						}
						
						// Create a new Blob File, as a place holder for storing
						// image after.
						AppEngineFile file = null;
						while (file == null) {
							try {
								file = fileService
										.createNewBlobFile("image/bmp");
								// Add this path to segmentFullPaths
								fullPathList.add(file.getFullPath());
								imageList.add(file.getFullPath());
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
		
		// Save all image info into an entity for later use
		Key imageInfoKey = KeyFactory.createKey(
				// use startTime as an id for this session
				"ImageInfo" + startedTime, 
				senderUrl);
		Entity imageInfoEntity = new Entity(imageInfoKey);
		imageInfoEntity.setProperty("imageList", imageList);
		pool.put(imageInfoEntity);
		
		pool.flush();
		
		
		// Log the execution time
		long endTime = System.nanoTime();
		log.info("MPDParser ended at " + endTime + " (ABSOLUTE TIME)");
		long elapsedTime = endTime - startedTime;
		// Convert from nano second to mini second
		log.info("MPDParser done in: " + 
				timeUnit.convert(elapsedTime, TimeUnit.NANOSECONDS) + 
				" ("+ timeUnit +")");


		// Dispatcher request to another servlet
		// Save full path list and number of media segment for later use.
		request.setAttribute("fullPathList", fullPathList);
		request.setAttribute("segmentCounter", segmentCounter);
		request.setAttribute("imageInfoKey", 
				KeyFactory.keyToString(imageInfoKey));
		
		
		// The mapreduce function read entity as its input, and as we just want
		// to read all entities in this session by mapper function, but not 
		// entities used in previous sessions, we create an session id
		// which is a unique time stamp to distinguish between these entities
		request.setAttribute("sessionId", "" + startedTime ); // String form

		String dispatchedLink = null;
		
		switch (processType) {
		case EXTRACT_IMAGE:
			dispatchedLink = "/change-color-servlet";
			break;

		case MODIFY_MPD:
			// ----- Save MPD file for later use ----------
			saveMpdFileToBlobstore(url);
			dispatchedLink = "/mpd-modificator-mapreduce-servlet";
			break;
		
		case CHANGE_COLOR:
			dispatchedLink = "/change-color-servlet";
			break;

		default:
			throw new UnsupportedOperationException(processType + 
					" is unsupported by the underlying platform");
		}

		
		log.info("redirect to: " + dispatchedLink);
		RequestDispatcher dispatcher = 
				request.getRequestDispatcher(dispatchedLink);

		dispatcher.forward(request, response);

	}

}