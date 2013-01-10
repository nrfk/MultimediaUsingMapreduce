package fr.telecomParistech.mapreduce;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.configuration.XMLConfiguration;

import com.google.appengine.tools.mapreduce.KeyValue;
import com.google.appengine.tools.mapreduce.Reducer;
import com.google.appengine.tools.mapreduce.ReducerInput;

public class MpdModificatorReducer 
extends Reducer<String, KeyValue<String, String>, String> {
	private static final long serialVersionUID = -8920726986215557597L;

	// Configuration-related properties
	private static final Logger log;
	private static final String CONFIG_FILE="WEB-INF/mapreduce-config.xml";
	private static final XMLConfiguration mapreduceConfig;
	private static final TimeUnit timeUnit;

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
	@Override public void reduce(String key,
			ReducerInput<KeyValue<String, String>> values) {
		// id of the current reducer
		int id = counter.incrementAndGet();
		long startedTime = System.nanoTime();
		log.info("Reducer #" + id + " started at " + 
				startedTime + " (ABSOLUTE TIME)");

		// Read the MPD file
		URL url = null;
		StringBuilder stringBuilder = new StringBuilder();
		try {
			url = new URL(key);
			BufferedReader reader = 
					new BufferedReader(new InputStreamReader(url.openStream()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		String mpdFile = stringBuilder.toString();
		while (values.hasNext()) {
			KeyValue<String, String> value = values.next();
			String oldUrl = value.getKey();
			String newUrl = value.getValue();
			mpdFile = mpdFile.replace(oldUrl, newUrl);
		}


		System.out.println("******************************");
		System.out.println(mpdFile);
		getContext().emit(mpdFile);
		
		// Done Log the execution time
		long endTime = System.nanoTime();
		log.info("Reducer #" + id + " ended at: " + endTime + " (ABSOLUTE TIME)");
		long elapsedTime = endTime - startedTime;

		log.info("Reducer #" + id + " done in: " + 
				timeUnit.convert(elapsedTime, TimeUnit.NANOSECONDS) + 
				" ("+ timeUnit +")");
	}

}

























