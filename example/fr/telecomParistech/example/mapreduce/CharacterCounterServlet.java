package fr.telecomParistech.example.mapreduce;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.KeyValue;
import com.google.appengine.tools.mapreduce.MapReduceJob;
import com.google.appengine.tools.mapreduce.MapReduceSettings;
import com.google.appengine.tools.mapreduce.MapReduceSpecification;
import com.google.appengine.tools.mapreduce.Marshallers;
import com.google.appengine.tools.mapreduce.inputs.DatastoreInput;
import com.google.appengine.tools.mapreduce.outputs.InMemoryOutput;

@SuppressWarnings("serial")
public class CharacterCounterServlet extends HttpServlet {
	private static final Logger log = 
			Logger.getLogger(CharacterCounterServlet.class.getName());

	//private static final boolean USE_BACKENDS = true;
	private static final boolean USE_BACKENDS = false;


	private String getUrlBase(HttpServletRequest req) 
			throws MalformedURLException {
		URL requestUrl = new URL(req.getRequestURL().toString());
		String portString = 
				requestUrl.getPort() == -1 ? "" : ":" + requestUrl.getPort();
		return requestUrl.getProtocol() + 
				"://" + requestUrl.getHost() + portString + "/";
	}

	private String getPipelineStatusUrl(String urlBase, String pipelineId) {
		return urlBase + "_ah/pipeline/status.html?root=" + pipelineId;
	}

	private void redirectToPipelineStatus(HttpServletRequest req, 
			HttpServletResponse resp, String pipelineId) throws IOException {
		String destinationUrl = 
				getPipelineStatusUrl(getUrlBase(req), pipelineId);
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

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		PrintWriter printWriter = resp.getWriter();
		printWriter.println(
				"<H1> Hello, I'm character counter servlet :) </H1>");
		printWriter.close();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		String text = req.getParameter("text");
		if (null == text || "".equals(text) || "null".equalsIgnoreCase(text) ) {
			return;
		}

		DatastoreService dataStore = 
				DatastoreServiceFactory.getDatastoreService();
		String[] words = text.split("[^a-zA-Z]");
		for (String word : words) {
			Entity e = new Entity("Word");
			e.setProperty("word", word);
			dataStore.put(e); // @TODO: Need to use MutualDataStoreService
		}

		redirectToPipelineStatus(req, resp, startCountJob(3,1));

	}

	private String startCountJob(int mapShardCount, int reduceShardCount) {
		return MapReduceJob.start(
				MapReduceSpecification.of(
						"Count Character using Map Reduce",
						new DatastoreInput("Word", mapShardCount),
						new CountMapper(),
						Marshallers.getStringMarshaller(),
						Marshallers.getLongMarshaller(),
						new CountReducer(),
						new InMemoryOutput<KeyValue<String, Long>>(
								reduceShardCount)),
						getSettings());
	}

}
