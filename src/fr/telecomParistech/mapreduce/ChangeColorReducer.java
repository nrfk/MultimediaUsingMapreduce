package fr.telecomParistech.mapreduce;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.configuration.XMLConfiguration;

import com.google.appengine.tools.mapreduce.KeyValue;
import com.google.appengine.tools.mapreduce.Reducer;
import com.google.appengine.tools.mapreduce.ReducerInput;

/**
 * This is the Reducer function of the Map-Reduce extractor.
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class ChangeColorReducer 
extends Reducer<Integer, String, KeyValue<Integer, String>> {
	private static final long serialVersionUID = 3748003219458311578L;
	// Counter, help us to keep track of each reduce task.
	private static AtomicInteger counter = new AtomicInteger(0);

	// Configuration-related properties
	private static final Logger log;
	private static final String CONFIG_FILE="WEB-INF/mapreduce-config.xml";
	private static final XMLConfiguration mapreduceConfig;
	private static final TimeUnit timeUnit;
	// static initializer 
	static {
		log = Logger.getLogger(ImageExtractorMapper.class.getName());
		// First, set log level in order to display log info during this 
		// static initializer. It's also the default log level
		log.setLevel(Level.INFO);
		XMLConfiguration tmp = null; 
		try {
			tmp = new XMLConfiguration(CONFIG_FILE);
		} catch (Exception e) {
			log.severe("Couldn't read config file: " + CONFIG_FILE);
			System.exit(1);
		} finally {
			mapreduceConfig = tmp;
			if (mapreduceConfig != null) {
				String timeUnitStr = mapreduceConfig
						.getString("mapreduce.time-unit","SECONDS");
				timeUnit = TimeUnit.valueOf(timeUnitStr);

				String logLevel = 
						mapreduceConfig.getString("log.level-reducer", "INFO");
				log.setLevel(Level.parse(logLevel));
			} else {
				timeUnit = TimeUnit.SECONDS;
			}
		}
	}

	@Override
	public void reduce(Integer key, ReducerInput<String> values) {
		// id of the current reducer
		int id = counter.incrementAndGet();
		long startedTime = System.nanoTime();
		log.info("Reducer #" + id + " started at " + 
				startedTime + " (ABSULUTE TIME)");

		// Collect all the results and out put it.
		int i = 0;
		while (values.hasNext()) {
			i++;
			String url = values.next();
			getContext().emit(new KeyValue<Integer, String>(i, url));
		}

		// Done Log the execution time
		long endTime = System.nanoTime();
		log.info("Reducer #" + id + " ended at: " + endTime + " (ABSULUTE TIME)");
		long elapsedTime = endTime - startedTime;

		log.info("Reducer #" + id + " done in: " + 
				timeUnit.convert(elapsedTime, TimeUnit.NANOSECONDS) + 
				" ("+ timeUnit +")");
	}

}
