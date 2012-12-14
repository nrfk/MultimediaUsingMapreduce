package fr.telecomParistech.mapreduce;

import java.io.BufferedInputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.tools.mapreduce.KeyValue;
import com.google.appengine.tools.mapreduce.Mapper;

/**
 * DashMapper is used to concatenate init segment with media segment in order 
 * to create a playable video file
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class DashMapper extends Mapper<Entity, String, KeyValue<Long, String>>{
	//public class DashMapper extends Mapper<Entity, String, String>{
	private static final long serialVersionUID = 5935430091638190456L;
	private static final Logger LOGGER;
	// GEA services
	private static final FileService fileService;
	private static final BlobstoreService blobstoreService;
	
	// init 
	static {
		LOGGER = Logger.getLogger(DashMapper.class.getName());
		LOGGER.setLevel(Level.WARNING);
		fileService = FileServiceFactory.getFileService();
		blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
	}
	
	@Override
	public void map(Entity value) {
		LOGGER.info("Mapper: " + value.getKey().toString());
		String initSegUrl = (String) value.getProperty("initSegmentUrl"); 
		long initSegSize = (Long) value.getProperty("initSegmentSize");
		String mediaSegUrl = (String) value.getProperty("url"); 
		long id = (Long) value.getProperty("id");

		try {
			// Create Blob Store for each file
			AppEngineFile file = 
					fileService.createNewBlobFile("application/zip");
			// Create writer to write to it
			boolean lock = true;
			FileWriteChannel writeChannel = 
					fileService.openWriteChannel(file, lock);
			
			// OK, let's write to the file, use both buffer and ByteArrayOutPut
			// Stream to minimize the number of write operation
			byte[] buffer;
			ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
			
			// - Write the Init Segment	
			BlobKey blobKey = 
					fileService.getBlobKey(new AppEngineFile(initSegUrl));
			buffer = blobstoreService.fetchData(blobKey, 0, initSegSize);
			byteArrayOut.write(buffer, 0, buffer.length);
			
//			writeChannel.write(ByteBuffer.wrap(buffer));
			
			// - Then write Media Segment
			URL url = new URL(mediaSegUrl);
			buffer = new byte[4096];
			int n;
			BufferedInputStream bufInput = 
					new BufferedInputStream(url.openStream());
			while ((n = bufInput.read(buffer)) > 0) {
				byteArrayOut.write(buffer, 0, n);
//				writeChannel.write(ByteBuffer.wrap(buffer, 0, n));
			}
			
			buffer = byteArrayOut.toByteArray();
			byteArrayOut.close();
			writeChannel.write(ByteBuffer.wrap(buffer));
			writeChannel.closeFinally();
			
			
			
			String key = (String) value.getProperty("representationInfo");
			getContext().emit(
					key, KeyValue.of(new Long(id), file.getFullPath()));
//			getContext().emit(key, file.getFullPath());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

}
