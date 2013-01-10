package fr.telecomParistech.mapreduce;

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

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.tools.mapreduce.KeyValue;
import com.google.appengine.tools.mapreduce.Mapper;

public class MpdModificatorMapper 
extends Mapper<Entity, String, KeyValue<String, String>> {
	private static final long serialVersionUID = 7197212312838140497L;

	// Configuration-related properties
	private static final Logger log;
	private static final String CONFIG_FILE="WEB-INF/mapreduce-config.xml";
	private static final XMLConfiguration mapreduceConfig;
	private static final TimeUnit timeUnit;
	// GEA services
	private static final FileService fileService = 
			FileServiceFactory.getFileService(); 

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

	@Override public void map(Entity value) {
		// Log the start time
		int id = counter.incrementAndGet();
		long startedTime = System.nanoTime();
		log.info("Mapper #" + id + " started at " +
				startedTime+ " (ABSULUTE TIME)");

		// Get some information which we obtained from parsing mpd fife in 
		// the previous step.
		// Segment url
		String segmentUrl = (String) value.getProperty("host"); // Host
		segmentUrl += (String) value.getProperty("url"); // and relative url

		// Download it
		long startDownTime = System.nanoTime();
		log.info("Mapper #" + id + " starts dowload segment data at: " + 
				startDownTime + (" (ABSOLUTE TIME)"));
		URL url = null;
		byte[] segmentData = null;
		try {
			url = new URL(segmentUrl);
			segmentData = IOUtils.toByteArray(url.openConnection());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			long endDownTime = System.nanoTime();
			log.info("Mapper #" + id + " finishes dowload segment data at: " + 
					endDownTime + (" (ABSOLUTE TIME)"));
			long downTime = endDownTime - startDownTime;
			log.info("Mapper #" + id + " downloading done in: " + 
					timeUnit.convert(downTime, TimeUnit.NANOSECONDS) + 
					" ("+ timeUnit +")");
		}

		try {
			// Save it to blob store
			// Create Blob Store for each file
			AppEngineFile file = 
					fileService.createNewBlobFile("application/zip");
			// Create writer to write to it
			boolean lock = true;
			FileWriteChannel writeChannel = 
					fileService.openWriteChannel(file, lock);
			writeChannel.write(ByteBuffer.wrap(segmentData));
			writeChannel.closeFinally();
			
			// Done
			String mpdPath = (String) value.getProperty("mpdPath");
			String oldPath = (String) value.getProperty("url");
			String newPath =  file.getFullPath();
			
			getContext().emit(
					mpdPath, 
					KeyValue.of(oldPath,newPath));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Log the execution time
			long endTime = System.nanoTime();
			log.info("Mapper #" + id + " ended at " + endTime + " (ABSOLUTE TIME)");
			long elapsedTime = endTime - startedTime;

			log.info("Mapper #" + id + " done in: " + 
					timeUnit.convert(elapsedTime, TimeUnit.NANOSECONDS) + 
					" ("+ timeUnit +")");
		}
	}

}




































