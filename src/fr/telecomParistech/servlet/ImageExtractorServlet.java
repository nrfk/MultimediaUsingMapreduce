package fr.telecomParistech.servlet;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.XMLConfiguration;

import sun.rmi.server.Dispatcher;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.KeyValue;
import com.google.appengine.tools.mapreduce.MapReduceJob;
import com.google.appengine.tools.mapreduce.MapReduceSettings;
import com.google.appengine.tools.mapreduce.MapReduceSpecification;
import com.google.appengine.tools.mapreduce.Marshallers;
import com.google.appengine.tools.mapreduce.inputs.DatastoreInput;
import com.google.appengine.tools.mapreduce.outputs.InMemoryOutput;
import com.google.appengine.tools.pipeline.PipelineService;
import com.google.appengine.tools.pipeline.PipelineServiceFactory;

import fr.telecomParistech.mapreduce.ImageExtractorMapper;
import fr.telecomParistech.mapreduce.ImageExtractorReducer;

/**
 * This Servlet receives parsed information from MPDParserServlet and then, it 
 *  tries to extract image in each media segment (whose url retrieved from 
 *  parsing process) 
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class ImageExtractorServlet extends HttpServlet {
	private static final long serialVersionUID = -6201164928303453173L;
	private static final boolean USE_BACKENDS = false;
	private static final String SESSION_ID = "sessionId";
	
	private static final PipelineService service = 
			PipelineServiceFactory.newPipelineService();

	// Configuration-related properties
	private static final Logger log;
	private static final String CONFIG_FILE="WEB-INF/mapreduce-config.xml";
	private static final XMLConfiguration mapreduceConfig;
	private static final TimeUnit timeUnit;

	// static initializer 
	static {
		log = Logger.getAnonymousLogger();
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
						mapreduceConfig.getString("log.level-display", "INFO");
				log.setLevel(Level.parse(logLevel));
			} else {
				timeUnit = TimeUnit.SECONDS;
			}
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// The mapreduce function read entity as its input, and as we just want
		// to read all entities in this session by mapper function, but not 
		// entities used by previous mapper function, we create an session id
		// which is a unique time stamp to distinguish between these entities
		String sessionId = (String) req.getAttribute(SESSION_ID);
		
		// Number of mapper and reducer function
		int mapShardCount = mapreduceConfig.getInt("mapreduce.map-task",5);
		int reduceShardCount = 
				mapreduceConfig.getInt("mapreduce.reduce-task", 1);
		
		MapReduceSpecification<
						Entity, 
						Integer, 
						String, 
						KeyValue<Integer,String>, 
						List<List<KeyValue<Integer,String>>>> mrSpec = 
				MapReduceSpecification.of(
						"Image Extractor", new DatastoreInput(
								"MediaSegmentInfo" + sessionId,
								mapShardCount),
						new ImageExtractorMapper(), Marshallers
								.getIntegerMarshaller(), Marshallers
								.getStringMarshaller(),
						new ImageExtractorReducer(),
						new InMemoryOutput<KeyValue<Integer,String>>(reduceShardCount));
		
		MapReduceSettings settings = new MapReduceSettings()
				.setWorkerQueueName("mapreduce-workers")
				.setControllerQueueName("default");
		
		if (USE_BACKENDS) {
			settings.setBackend("worker");
		}
		
		String pipelineId = service.startNewPipeline(
						new MapReduceJob<
								Entity, 
								Integer, 
								String, 
								KeyValue<Integer,String>, 
								List<List<KeyValue<Integer,String>>>>(),
						mrSpec, settings);
		
		String redirectLink = "/image-extractor-result.jsp?";
		
		StringBuilder strBuilder = new StringBuilder();
		//The parser always sends us List<String> contains list of segment
		// So we can safety supress this casting warning
		@SuppressWarnings("unchecked")
		List<String> fullPathList = 
				(List<String>)req.getAttribute("fullPathList");
		strBuilder.append("fullPathList=");
		for (String fullPath : fullPathList) {
			strBuilder.append(fullPath + " ");
		}
		strBuilder.append("&segmentCounter=");
		int segmentCounter = (Integer) req.getAttribute("segmentCounter");
		strBuilder.append(segmentCounter);
		
		strBuilder.append("&pipelineId=" + pipelineId);
		
		redirectLink += strBuilder.toString();
		System.out.println(redirectLink);
		resp.sendRedirect(redirectLink);
	}
}



























