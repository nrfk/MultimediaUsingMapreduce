package fr.telecomParistech.mapreduce;

import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.KeyValue;
import com.google.appengine.tools.mapreduce.MapReduceJob;
import com.google.appengine.tools.mapreduce.MapReduceSettings;
import com.google.appengine.tools.mapreduce.MapReduceSpecification;
import com.google.appengine.tools.mapreduce.Marshallers;
import com.google.appengine.tools.mapreduce.inputs.DatastoreInput;
import com.google.appengine.tools.mapreduce.outputs.InMemoryOutput;
import com.google.appengine.tools.pipeline.FutureValue;
import com.google.appengine.tools.pipeline.Job1;
import com.google.appengine.tools.pipeline.Value;

import fr.telecomParistech.dash.mpd.MPD;

/**
 * ImageExtractorPipeline consists of three sub-jobs:
 *  + JobDownloadingMpd for dowloading MPD file
 *  + JobParsingMpd for parsing MPD file
 *  + JobSavingVideoData for saving information obtained via parsing MPD
 *  + ImageExtractorMapReduce
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class ImageExtractorPipeline extends Job1<String, String>{
	private static final long serialVersionUID = 1L;
	private static final boolean USE_BACKENDS = false;
	
	public Value<String> run(String mpdLocation) {
		FutureValue<MPD> mpd = 
				futureCall(new JobDownloadingMpd(), immediate(mpdLocation));
		
		FutureValue<List<Entity>> entityList = 
				futureCall(new JobParsingMpd(), mpd, immediate(mpdLocation));
		
		futureCall(new JobSavingVideoData(), entityList);
		
		// Create Map Reduce task
		int mapShardCount = 10;
		int reduceShardCount = 1;
		
		MapReduceSpecification<	Entity, // I
								Integer, // K
								String, // V
								KeyValue<Integer, String>, // O
								List<List<KeyValue<Integer, String>>>> //R
				mrSpec = MapReduceSpecification.of(
						"Image Extractor", 
						new DatastoreInput("MediaSegmentInfo", mapShardCount), 
						new ImageExtractorMapper(), 
						Marshallers.getIntegerMarshaller(), 
						Marshallers.getStringMarshaller(), 
						new ImageExtractorReducer(), 
						new InMemoryOutput<KeyValue<Integer, String>>(reduceShardCount));
		
		MapReduceSettings settings = getSettings();
		
		String pipelineId = MapReduceJob.start(mrSpec, settings);
		
		return immediate(pipelineId);
	}
	/**
	 * Get defaut MapReduceSettings, used by the startDashProcessingJob() 
	 * function
	 * @return MapReduceSettings
	 */
	private MapReduceSettings getSettings() {
		MapReduceSettings settings = new MapReduceSettings()
		.setWorkerQueueName("mapreduce-workers")
		.setControllerQueueName("default");
		if (USE_BACKENDS) {
			settings.setBackend("worker");
		}
		return settings;
	}
}
