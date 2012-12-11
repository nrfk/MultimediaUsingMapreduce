package fr.telecomParistech.mapreduce;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.IOUtils;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.tools.mapreduce.Mapper;

import fr.telecomParistech.image.bitmap.ConvertUtility;
import fr.telecomParistech.parser.H264Parser;
import fr.telecomParistech.parser.MP4Parser;

/**
 * This is the Mapper function of the Map-Reduce extractor.
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class ImageExtractorMapper extends Mapper<Entity, Integer, String>{
	private static final long serialVersionUID = -1726878920669357399L;

	// Configuration-related properties
	private static final Logger log;
	private static final String CONFIG_FILE="WEB-INF/mapreduce-config.xml";
	private static final XMLConfiguration mapreduceConfig;
	private static final TimeUnit timeUnit;
	// GEA services
	private static final FileService fileService = 
			FileServiceFactory.getFileService(); 
	private static final ImagesService imagesService = 
			ImagesServiceFactory.getImagesService();

	// Counter, help us to keep track of each map task.
	private static AtomicInteger counter = new AtomicInteger(0);
	// static initializer 
	static {
		log = Logger.getLogger(ImageExtractorMapper.class.getName());

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
						mapreduceConfig.getString("log.level-mapper", "INFO");
				log.setLevel(Level.parse(logLevel));

			} else {
				timeUnit = TimeUnit.SECONDS;
			}
		}
	}

	public void map(Entity value) {
		// Log the start time
		int id = counter.incrementAndGet();
		long startedTime = System.nanoTime();
		log.info("Mapper #" + id + " started at " +
				startedTime+ " (ABSULUTE TIME)");

		// Get some information which we obtained from parsing mpd fife in 
		// the previous step.
		long representationId = (Long) value.getProperty("representationId");
		// sps
		String sps = (String) value.getProperty("sps");
		// pps
		String pps = (String) value.getProperty("pps");
		String segmentUrl = (String) value.getProperty("url");
		MP4Parser mp4Parser = new MP4Parser();
		URL url = null;
		byte[] segmentData = null;

		long startDownTime = System.nanoTime();
		log.info("Mapper #" + id + " starts dowload segment data at: " + 
				startDownTime + (" (ABSULUTE TIME)"));
		try {
			url = new URL(segmentUrl);
			segmentData = IOUtils.toByteArray(url.openStream());

		} catch (MalformedURLException e) {
			e.printStackTrace();
			log.severe("MalformedURLException, url: " + url.toString());
			System.exit(1);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			long endDownTime = System.nanoTime();
			log.info("Mapper #" + id + " finishes dowload segment data at: " + 
					endDownTime + (" (ABSULUTE TIME)"));
			long downTime = endDownTime - startDownTime;
			log.info("Mapper #" + id + " downloading done in: " + 
					timeUnit.convert(downTime, TimeUnit.NANOSECONDS) + 
					" ("+ timeUnit +")");
		}

		// nalHeader
		byte[] nalHeader = {
				0x00,
				0x00,
				0x00, 
				0x01}; 
		byte[] spsData = ConvertUtility.hexStringToByteArray(sps);
		byte[] ppsData = ConvertUtility.hexStringToByteArray(pps);
		long nalLengthSize = (Long) value.getProperty("nalLengthSize");
		long videoTrackId = (Long) value.getProperty("videoTrackId");

		// Get the h264 raw data
		byte[] h264Raw = mp4Parser.createFirstImageFromDashSegment(
				nalHeader, 
				spsData, 
				ppsData, 
				segmentData, 
				(int) nalLengthSize, 
				(int) videoTrackId);

		// cannot get h264 data, return.
		if (h264Raw == null) {
			// Log the execution time
			long endTime = System.nanoTime();
			log.info("Mapper #" + id + " ended at " + 
					endTime + " (ABSULUTE TIME)");
			long elapsedTime = endTime - startedTime;

			// Convert from nano second to mini second
			log.info("Mapper #" + id + " (downloading + processing) done in: " + 
					timeUnit.convert(elapsedTime, TimeUnit.NANOSECONDS) + 
					" ("+ timeUnit +")");
			return;
		}

		// Now pass it to H264 parser
		H264Parser h264Parser = new H264Parser();
		byte[] iFrame = null;
		boolean isExceptionOccured = false;
		try {
			iFrame = h264Parser.parseH264Raw(h264Raw);
		} catch (ArrayIndexOutOfBoundsException e) {
			log.info("ArrayIndexOutOfBoundsException");
			// If we have an exception, pass it as the result
			isExceptionOccured = true;
		}

		// Save the image to file
		FileWriteChannel writeChannel = null;
		AppEngineFile file = null;
		try {
			String imageFullPath = (String) value.getProperty("imageFullPath");
			file = new AppEngineFile(imageFullPath);
			boolean lock = true;

			// If there're exceptions while parsing h264 raw data, display 
			// an altenative image.
			if (isExceptionOccured) {
				File errorImageFile = new File("WEB-INF/error.png");
				FileInputStream fis = new FileInputStream(errorImageFile);
				iFrame = IOUtils.toByteArray(fis);
			}
			
			writeChannel = fileService.openWriteChannel(file, lock);
			writeChannel.write(ByteBuffer.wrap(iFrame, 0, iFrame.length));



		} catch (Exception exception) {
			log.severe("Error at GAE server: ");
			exception.printStackTrace();
			System.exit(1);
		} finally {
			if (writeChannel != null) {
				try {
					writeChannel.closeFinally();
				} catch (Exception e) {
					log.severe("error while closing file channel: ");
					e.printStackTrace();
					System.exit(1);
				}
			}
		}

		// Ok, get its url in order to return to the client later.
		BlobKey blobKey = fileService.getBlobKey(file);
		ServingUrlOptions servingUrlOptions =
				ServingUrlOptions.Builder.withBlobKey(blobKey);
		String iFrameUrl = imagesService.getServingUrl(servingUrlOptions);
		log.info("Emit: " + value.getKey().toString());
		getContext().emit((int) representationId, iFrameUrl);

		// Log the execution time
		long endTime = System.nanoTime();
		log.info("Mapper #" + id + " ended at " + endTime + " (ABSULUTE TIME)");
		long elapsedTime = endTime - startedTime;

		log.info("Mapper #" + id + " done in: " + 
				timeUnit.convert(elapsedTime, TimeUnit.NANOSECONDS) + 
				" ("+ timeUnit +")");
	}
}




















