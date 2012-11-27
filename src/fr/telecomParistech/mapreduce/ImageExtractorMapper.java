package fr.telecomParistech.mapreduce;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.tools.mapreduce.Mapper;
import com.twilight.h264.util.BufferedInputStream;

import fr.telecomParistech.image.bitmap.ConvertUtility;
import fr.telecomParistech.mp4parser.MP4Parser;

public class ImageExtractorMapper extends Mapper<Entity, Integer, String>{
	private static final long serialVersionUID = -1726878920669357399L;

	private static final Logger LOGGER;
	// GEA services
	private static final FileService fileService;
	private static final BlobstoreService blobstoreService;

	// init 
	static {
		LOGGER = Logger.getLogger(ImageExtractorMapper.class.getName());
		LOGGER.setLevel(Level.FINE);
		fileService = FileServiceFactory.getFileService();
		blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
	}

	public void map(Entity value) {
		LOGGER.info("Mapper: " + value.getKey().toString());
		long id = (Long) value.getProperty("id");
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
		
		// Now pass it to H264 parser
		
		LOGGER.info("******************* #" + id);
		
//		LOGGER.info(ConvertUtility.byteArrayToHext(h264Raw));
		
		LOGGER.info("SPS: " + sps);
		LOGGER.info("PPS: " + pps);
		LOGGER.info("Nal Size: " + nalLengthSize);
		
		
	}

}
