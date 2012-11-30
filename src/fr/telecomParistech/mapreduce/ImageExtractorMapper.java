package fr.telecomParistech.mapreduce;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
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

	private static final Logger LOGGER;
	// GEA services
	private static final FileService fileService;
	private static final ImagesService imagesService = 
			ImagesServiceFactory.getImagesService();

	// init 
	static {
		LOGGER = Logger.getLogger(ImageExtractorMapper.class.getName());
		LOGGER.setLevel(Level.FINE);
		fileService = FileServiceFactory.getFileService();
	}

	public void map(Entity value) {
		LOGGER.info("Mapper: " + value.getKey().toString());
		long representationId = (Long) value.getProperty("representationId");
		String sps = (String) value.getProperty("sps");
		String pps = (String) value.getProperty("pps");

		String segmentUrl = (String) value.getProperty("url");

		MP4Parser mp4Parser = new MP4Parser();
		URL url = null;
		byte[] segmentData = null;
		try {
			url = new URL(segmentUrl);
			segmentData = IOUtils.toByteArray(url.openStream());

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		byte[] nalHeader = {
				0x00,
				0x00,
				0x00, 
				0x01}; 
		byte[] spsData = ConvertUtility.hexStringToByteArray(sps);
		byte[] ppsData = ConvertUtility.hexStringToByteArray(pps);
		long nalLengthSize = (Long) value.getProperty("nalLengthSize");
		long videoTrackId = (Long) value.getProperty("videoTrackId");

		// Get the result
		byte[] h264Raw = mp4Parser.createH264rawDataFromDashSegment(
				nalHeader, 
				spsData, 
				ppsData, 
				segmentData, 
				(int) nalLengthSize, 
				(int) videoTrackId);

		if (h264Raw == null) {
			return;
		}

		// Now pass it to H264 parser
		H264Parser h264Parser = new H264Parser();
		byte[] iFrame = h264Parser.parseH264Raw(h264Raw);
		if (iFrame == null) {
			return;
		}

		// save the image to file
		FileWriteChannel writeChannel = null;
		AppEngineFile file = null;
		try {
			String imageFullPath = (String) value.getProperty("imageFullPath");
			file = new AppEngineFile(imageFullPath);
			boolean lock = true;
			writeChannel = fileService.openWriteChannel(file, lock);
			writeChannel.write(ByteBuffer.wrap(iFrame, 0, iFrame.length));

		} catch (Exception ignored) {

		} finally {
			if (writeChannel != null) {
				try {
					writeChannel.closeFinally();
				} catch (Exception ignored) {}
			}
		}

		BlobKey blobKey = fileService.getBlobKey(file);
		ServingUrlOptions servingUrlOptions =
				ServingUrlOptions.Builder
				.withBlobKey(blobKey);


		String iFrameUrl = imagesService.getServingUrl(servingUrlOptions);


		// Ok, pass it to the reduce function*
		System.out.println("mapper *******************");
		System.out.println("representationId: " + representationId);
		getContext().emit((int) representationId, iFrameUrl);
	}

}




















