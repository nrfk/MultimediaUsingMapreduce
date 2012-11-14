package fr.telecomParistech.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.XMLConfiguration;

import com.google.appengine.demos.mapreduce.entitycount.CountEntityServlet;
import com.google.appengine.tools.mapreduce.MapReduceJob;
import com.google.appengine.tools.mapreduce.MapReduceResult;
import com.google.appengine.tools.mapreduce.MapReduceSettings;
import com.google.appengine.tools.mapreduce.MapReduceSpecification;
import com.google.appengine.tools.mapreduce.Marshallers;
import com.google.appengine.tools.mapreduce.inputs.DatastoreInput;
import com.google.appengine.tools.mapreduce.outputs.InMemoryOutput;
import com.google.appengine.tools.pipeline.JobInfo;
import com.google.appengine.tools.pipeline.PipelineService;
import com.google.appengine.tools.pipeline.PipelineServiceFactory;
import com.google.common.collect.ImmutableList;

import fr.telecomParistech.mapreduce.DashMapper;
import fr.telecomParistech.mapreduce.DashReducer;

/**
 * Servlet for doing the map-reduce processing
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class DashMapReduceServlet extends HttpServlet {
	private static final long serialVersionUID = 5366478568593641461L;	
	private static final Logger log = Logger.getLogger(CountEntityServlet.class.getName());
	static {
		log.setLevel(Level.INFO);
	}
	
	//private static final boolean USE_BACKENDS = true;
	private static final boolean USE_BACKENDS = false;
	private final PipelineService pipelineService = PipelineServiceFactory.newPipelineService();

	/**
	 * Get UrlBase of the request
	 * @param req request to getUrl
	 * @return UrlBase
	 * @throws MalformedURLException
	 */
	private String getUrlBase(HttpServletRequest req) throws MalformedURLException {
		URL requestUrl = new URL(req.getRequestURL().toString());
		String portString = requestUrl.getPort() == -1 ? "" : ":" + requestUrl.getPort();
		return requestUrl.getProtocol() + "://" + requestUrl.getHost() + portString + "/";
	}

	/**
	 * Get url of pipeline status of a pipeline process
	 * @param urlBase Urlbase of the request
	 * @param pipelineId pipeline id
	 * @return the url of pipeline status
	 */
	private String getPipelineStatusUrl(String urlBase, String pipelineId) {
		return urlBase + "_ah/pipeline/status.html?root=" + pipelineId;
	}

	/**
	 * Redirect to pipeline status
	 * @param req current request of the Servlet
	 * @param resp current response of the Servlet
	 * @param pipelineId pipelineId to redirect to
	 * @throws IOException
	 */
	private void redirectToPipelineStatus(HttpServletRequest req, HttpServletResponse resp,
			String pipelineId) throws IOException {
		String destinationUrl = getPipelineStatusUrl(getUrlBase(req), pipelineId);
		log.info("Redirecting to " + destinationUrl);
		resp.sendRedirect(destinationUrl);
	}

	private MapReduceSettings getSettings() {
		MapReduceSettings settings = new MapReduceSettings()
		.setWorkerQueueName("mapreduce-workers")
		.setControllerQueueName("default");
		if (USE_BACKENDS) {
			settings.setBackend("worker");
		}
		return settings;
	}
	
	private String startDashProcessingJob(int mapShardCount, int reduceShardCount) {
		
		String pipelineId = 
				MapReduceJob.start(
					MapReduceSpecification.of(
							"Dash Processing using Map Reduce",
							new DatastoreInput("MediaSegmentInfo", mapShardCount), // I
							new DashMapper(), // I, K, V
							Marshallers.getStringMarshaller(), // K
							Marshallers. // V
									getKeyValueMarshaller(
											Marshallers.getLongMarshaller(), 
											Marshallers.getStringMarshaller()),
						  	new DashReducer(), // K, V, O
						  	new InMemoryOutput<String>(reduceShardCount)), // O, R
				  	getSettings()); 
		return pipelineId;
//		new InMemoryOutput<KeyValue<Long, String>>(reduceShardCount)),
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp){
		try {
			XMLConfiguration config = new XMLConfiguration("WEB-INF/dash-mapreduce-config.xml");
			String pipelineId = 
					startDashProcessingJob(config.getInt("mapreduce.map-task",3), 
										   config.getInt("mapreduce.reduce-task",1));
			JobInfo jobInfo = pipelineService.getJobInfo(pipelineId);
			
			
			redirectToPipelineStatus(req, resp, pipelineId);
			
			
//			Object result = jobInfo.getOutput();
//			MapReduceResult<ImmutableList<ImmutableList<String>>> result =  
//					(MapReduceResult<ImmutableList<ImmutableList<String>>>) jobInfo.getOutput();
//			
//			String xml = result
//					.getOutputResult()
//					.get(0)
//					.get(0);
//			
//			PrintWriter pw = resp.getWriter();
//			pw.write(xml);
//			pw.write("************************");
//			pw.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}


//redirectToPipelineStatus(req, resp, pipelineId);

//new MapReduceJob<I, K, V, O, R>()
//PipelineService service = PipelineServiceFactory.newPipelineService();
//
//
//MapReduceSpecification< Entity, 
//						String, 
//						KeyValue<Long, String>, 
//						String, 
//						List<List<String>>> 
//		mrSpec = 
//			MapReduceSpecification.of(
//					"Dash Processing using Map Reduce",
//					new DatastoreInput("MediaSegmentInfo", 3), // I
//					new DashMapper(), // I, K, V
//					Marshallers.getStringMarshaller(), // K
//					Marshallers. // V
//							getKeyValueMarshaller(
//									Marshallers.getLongMarshaller(), 
//									Marshallers.getStringMarshaller()),
//				  	new DashReducer(), // K, V, O
//				  	new InMemoryOutput<String>(1));		
//
//
//
//String pipelineId = 
//		service.startNewPipeline(
//				new MapreduceConcatenateJob(), 
//				mrSpec, // O, R, 
//			  	getSettings() );




//PrintWriter pw = new PrintWriter(resp.getOutputStream());
//  pw.println("" + ShuffleServiceFactory.getShuffleService()
//      .getStatus(req.getParameter("shuffleJobId")));
//  pw.close();