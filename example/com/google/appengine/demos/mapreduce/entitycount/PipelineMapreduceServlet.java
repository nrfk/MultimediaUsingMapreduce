package com.google.appengine.demos.mapreduce.entitycount;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.KeyValue;
import com.google.appengine.tools.mapreduce.MapReduceJob;
import com.google.appengine.tools.mapreduce.MapReduceSettings;
import com.google.appengine.tools.mapreduce.MapReduceSpecification;
import com.google.appengine.tools.mapreduce.Marshallers;
import com.google.appengine.tools.mapreduce.impl.Util;
import com.google.appengine.tools.mapreduce.inputs.DatastoreInput;
import com.google.appengine.tools.mapreduce.outputs.InMemoryOutput;
import com.google.appengine.tools.pipeline.Job0;
import com.google.appengine.tools.pipeline.PipelineService;
import com.google.appengine.tools.pipeline.PipelineServiceFactory;
import com.google.appengine.tools.pipeline.Value;

import fr.telecomParistech.mapreduce.ImageExtractorMapper;
import fr.telecomParistech.mapreduce.ImageExtractorReducer;


public class PipelineMapreduceServlet extends HttpServlet {
	private static final long serialVersionUID = 1624900710626309961L;
	private static final Logger log = Logger.getLogger(EntityCounterServlet.class.getName());
	private static final PipelineService pipelineService = PipelineServiceFactory.newPipelineService();
	private static final boolean USE_BACKENDS = false;

	private static MapReduceSettings getSettings() {
		MapReduceSettings settings = new MapReduceSettings()
		.setWorkerQueueName("mapreduce-workers")
		.setControllerQueueName("default");
		if (USE_BACKENDS) {
			settings.setBackend("worker");
		}
		return settings;
	}
	
	private static class CountEntityJob extends Job0<Void> {

		@Override
		public Value<Void> run() {
			int mapShardCount = 3;
			int reduceShardCount = 1;
			
			
			MapReduceSpecification<  
							 Entity, // I  
							 String, // K
							 Long,  // V 
							 KeyValue<String, Long>, // O 
							 List<List<KeyValue<String, Long>>>> // R 
					mrSpec = MapReduceSpecification.of(
							"MapReduceTest stats",
							new DatastoreInput("MapReduceTest", mapShardCount), 
							new CountMapper(), 
							Marshallers.getStringMarshaller(),
							Marshallers.getLongMarshaller(),
							new CountReducer(), 
							new InMemoryOutput<KeyValue<String, Long>>(reduceShardCount)); 
			MapReduceSettings settings = getSettings();
				
			// Work with  startNewPipeline(Job2<?,T1,T2>, T1, T2, JobSetting...)
			pipelineService.startNewPipeline(new MapReduceJob(), mrSpec, settings);
			
			// Dont work with futureCall(Job2.....
//			futureCall(new MapReduceJob(), mrSpec, settings);
//			futureCall(new MapReduceJob(), mrSpec, settings, Util.jobSettings(settings));
			
			return null;
		}
		
	}
}
