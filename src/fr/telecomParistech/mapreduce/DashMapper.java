package fr.telecomParistech.mapreduce;

import java.io.BufferedInputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public class DashMapper extends Mapper<Entity, String, KeyValue<Long, String>>{
//public class DashMapper extends Mapper<Entity, String, String>{
	private static final long serialVersionUID = 5935430091638190456L;
	private static final Logger log = Logger.getLogger(DashMapper.class.getName());
	
	static {
		log.setLevel(Level.WARNING);
	}
	
	@Override
	public void map(Entity value) {
		log.info("Mapper: " + value.getKey().toString());
		String initSegUrl = (String) value.getProperty("initSegmentUrl"); 
		long initSegSize = (Long) value.getProperty("initSegmentSize");
		String mediaSegUrl = (String) value.getProperty("url"); 
		long id = (Long) value.getProperty("id");
	
		
		try {
			// Init some services
			FileService fileService = FileServiceFactory.getFileService();
			BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
			
			// Create Blob Store for each file
			AppEngineFile file = fileService.createNewBlobFile("application/zip");
			// Create writer to write to it
			boolean lock = true;
			FileWriteChannel writeChannel = 
					fileService.openWriteChannel(file, lock);
			
			// OK, let's write to the file
			byte[] byteChunk;
			
			// - Write the Init Segment	
			BlobKey blobKey = fileService.getBlobKey(new AppEngineFile(initSegUrl));
			byteChunk = blobstoreService.fetchData(blobKey, 0, initSegSize);
			writeChannel.write(ByteBuffer.wrap(byteChunk));
			
			// - Then write Media Segment
			URL url = new URL(mediaSegUrl);
			byteChunk = new byte[4096];
			int n;
			BufferedInputStream bufInput = new BufferedInputStream(url.openStream());
			while ((n = bufInput.read(byteChunk)) > 0) {
				writeChannel.write(ByteBuffer.wrap(byteChunk, 0, n));
			}
			writeChannel.closeFinally();
			
			
			
			String key = (String) value.getProperty("representationInfo");
			getContext().emit(key, KeyValue.of(new Long(id), file.getFullPath()));
//			getContext().emit(key, file.getFullPath());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

}
