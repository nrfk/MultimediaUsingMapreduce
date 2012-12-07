<%@page import="java.util.concurrent.TimeUnit"%>
<%@page import="java.util.logging.Level"%>
<%@page import="org.apache.commons.configuration.XMLConfiguration"%>
<%@page import="java.util.logging.Logger"%>
<%@page import="com.sun.org.apache.bcel.internal.generic.IINC"%>
<%@page import="java.util.Iterator"%>
<%@page import="fr.telecomParistech.mapreduce.ImageExtractorMapper"%>
<%@page import="fr.telecomParistech.mapreduce.ImageExtractorReducer"%>
<%@page import="com.google.appengine.tools.mapreduce.outputs.InMemoryOutput"%>
<%@page import="com.google.appengine.tools.mapreduce.inputs.DatastoreInput"%>
<%@page import="com.google.appengine.tools.mapreduce.MapReduceJob"%>
<%@page import="com.google.appengine.tools.mapreduce.MapReduceResult"%>
<%@page import="com.google.appengine.api.datastore.Entity"%>
<%@page import="java.util.List"%>
<%@page import="fr.telecomParistech.dash.mpd.MPD"%>
<%@page import="com.google.appengine.tools.pipeline.*"%>
<%@page import="fr.telecomParistech.example.mapreduce.*"%>
<%@page import="com.google.appengine.tools.pipeline.demo.LetterCountExample.LetterCounter"%>
<%@page import="java.util.SortedMap"%>
<%@page import="com.google.appengine.tools.mapreduce.*"%>

<%!private static final String TEXT_PARAM_NAME = "mpdLocation";
	private static final String PIPELINE_ID_PARAM_NAME = "pipelineId";
	private static final String CLEANUP_PIPELINE_ID_PARAM_NAME = "cleanupId";
	private static final String SEGMENT_COUNTER = "segmentCounter";
	private static final String SEGMENT_FULL_PATHS = "segmentFullPaths";
	private static final String SESSION_ID = "sessionId";
	
	private static final int IMAGES_PER_ROW;
	private static final int IMAGES_WIDTH;
	private static final int IMAGES_HEIGHT;
	
	private static final PipelineService service = PipelineServiceFactory
			.newPipelineService();
	
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
				IMAGES_PER_ROW = 
						mapreduceConfig.getInt("result.images-per-row",4);
				IMAGES_WIDTH = 
						mapreduceConfig.getInt("result.image-width",150);
				IMAGES_HEIGHT = 
						mapreduceConfig.getInt("result.image-height",150);
				
				String timeUnitStr = mapreduceConfig
						.getString("mapreduce.time-unit","SECONDS");
				timeUnit = TimeUnit.valueOf(timeUnitStr);
				
				String logLevel = mapreduceConfig.getString("log.level-display", "INFO");
				log.setLevel(Level.parse(logLevel));
			} else {
				IMAGES_PER_ROW = 4; 
				IMAGES_WIDTH = 150;
				IMAGES_HEIGHT = 150;
				timeUnit = TimeUnit.SECONDS;
			}
		}
	}
	
	private static final boolean USE_BACKENDS = false;%>
<HTML>
<HEAD>
<link rel="stylesheet" type="text/css" href="someStyle.css">
<style type="text/css">
.period {
	font-style: italic;
	margin-bottom: 1em;
	font-size: 0.8em;
}

h4.withperiod {
	margin-bottom: 0em;
}
</style>
<script src="result-checker.js"></script>
</HEAD>
<BODY>

	<H2>Image Extractor Service</H2>

	<%
		String text = request.getParameter(TEXT_PARAM_NAME);
		if (text != null
				&& (text.equalsIgnoreCase("null") || text.equals(""))) {
			text = null;
		}

		String pipelineId = request.getParameter(PIPELINE_ID_PARAM_NAME);
		if (pipelineId != null
				&& (pipelineId.equalsIgnoreCase("null") || pipelineId
						.equals(""))) {
			pipelineId = null;
		}

		String cleanupId = request
				.getParameter(CLEANUP_PIPELINE_ID_PARAM_NAME);
		if (cleanupId != null
				&& (cleanupId.equalsIgnoreCase("null") || cleanupId
						.equals(""))) {
			cleanupId = null;
		}

		int segmentCouter = 
				Integer.parseInt(request.getParameter(SEGMENT_COUNTER));
		int imageRows = (int) Math.round(Math.ceil((float)segmentCouter/IMAGES_PER_ROW));
		
		String segmentFullPaths = request.getParameter("segmentFullPaths"); 
		if (segmentFullPaths != null
				&& (segmentFullPaths.equalsIgnoreCase("null") || segmentFullPaths.equals(""))) {
			segmentFullPaths = null;
		}
		
		if (null != cleanupId) {
			service.deletePipelineRecords(cleanupId);
		}
		
	%>

	<p>

		<%
		String sessionId = request.getParameter(SESSION_ID);
		if (sessionId != null
				&& (sessionId.equalsIgnoreCase("null") || sessionId.equals(""))) {
			sessionId = "";
		}
		
		if (null == pipelineId) {
			int mapShardCount = mapreduceConfig.getInt("mapreduce.map-task",5);
			int reduceShardCount = 
					mapreduceConfig.getInt("mapreduce.reduce-task", 1);
			MapReduceSpecification<Entity, Integer, String, KeyValue<Integer,String>, List<List<KeyValue<Integer,String>>>> mrSpec = MapReduceSpecification
					.of("Image Extractor", new DatastoreInput(
							"MediaSegmentInfo" + sessionId, mapShardCount),
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
			pipelineId = service
					.startNewPipeline(
							new MapReduceJob<Entity, Integer, String, KeyValue<Integer,String>, List<List<KeyValue<Integer,String>>>>(),
							mrSpec, settings);
		}

		JobInfo jobInfo = service.getJobInfo(pipelineId);
		switch (jobInfo.getJobState()) {
		case WAITING_TO_RETRY:
			// Do nothing
			break;

		case COMPLETED_SUCCESSFULLY:
		%>
			<H4>Computation completed.</H4>
		
			
			
	<p>
		<table border="1">
			
		
		<%
			MapReduceResult<List<List<KeyValue<Integer,String>>>> mrResult = 
				(MapReduceResult<List<List<KeyValue<Integer,String>>>>) jobInfo.getOutput();
			List<KeyValue<Integer,String>> outputList = mrResult.getOutputResult().get(0);
			
			// Get iterator
			Iterator<KeyValue<Integer,String>> iterator = outputList.iterator();
			// We'll create 'imageRows' row and 'IMAGE_PER_ROW' columns'
			imageRows = (int) Math.round(Math.ceil(((float)outputList.size() / IMAGES_PER_ROW)));
			
			// OK, now fill it
			for (int i = 0; i < imageRows; i++) {
				out.print("<tr>");
				for (int j = 0; j < IMAGES_PER_ROW; j++) {
					
					KeyValue<Integer,String> keyvalue = null;
					if (iterator.hasNext()) {
						keyvalue = iterator.next();
					}
					
					// Padding cell at the last row.
					if (keyvalue == null) {
						// Align padding
						out.print("<td height='200' width='200'> . </td>");
						continue;
					}
					
					/* alt='Extracted Image' height='200' width='200' */
					// Data cell
					
					if (keyvalue.getValue().startsWith("http://")) { // If it's an image
						out.print("<td height='" + IMAGES_HEIGHT + " ' width='"+IMAGES_WIDTH+"'><img src=" + keyvalue.getValue() + " > </td>");	
					} else { // if it's an exception
						out.print("<td height='" + IMAGES_HEIGHT + " ' width='"+IMAGES_WIDTH+"'> Exception while parsing image:" + keyvalue.getValue() + "</td>");
					}
				}
				out.print("</tr>");
			}
		%>
		</table>
	<form method="post" >
		<input name="<%=TEXT_PARAM_NAME%>" value="" type="hidden"> 
		<input name="<%=PIPELINE_ID_PARAM_NAME%>" value="" type="hidden"> 
		<input name="<%=CLEANUP_PIPELINE_ID_PARAM_NAME%>" value="<%=pipelineId%>" type="hidden"> 
		<input type="submit" value="Do it again" >
	</form>
	
	
	<%
			break;
		case RUNNING:
			// Parse segmentFullPaths to get item
			String[] items = segmentFullPaths.split(" ");
			out.print("<table border='1'>");
			int nImages = Integer.parseInt(request.getParameter("segmentCounter"));
			for (int i = 0; i < imageRows; i++) {
				out.print("<tr>");
				for (int j = 0; j < IMAGES_PER_ROW; j++) {
					if (nImages > 0) { // Placeholder
						out.print("<td height='" + IMAGES_HEIGHT + " ' width='"+IMAGES_WIDTH+"'>" + items[i*IMAGES_PER_ROW + j]  + "</td>");	
					} else { // Padding
						out.print("<td height='" + IMAGES_HEIGHT + " ' width='"+IMAGES_WIDTH+"'> .  </td>");
					}
					nImages--;
				}
				out.print("</tr>");
			}
			out.print("</table>");
	%>
	Calculation not yet completed.
	<p>
	
	<img src="loading.gif" alt="Now loading" height="200" width="200">
	
	<form method="post" id="polling-form">
		<input name="<%=TEXT_PARAM_NAME%>" value="<%=text%>" type="hidden">
		<input name="<%=SEGMENT_FULL_PATHS%>" value="<%=segmentFullPaths%>" type="hidden">
		<input name="<%=PIPELINE_ID_PARAM_NAME%>" value="<%=pipelineId%>"type="hidden"> 
		<input type="submit" value="Check Again" style="visibility:hidden;">
	</form>
	<script type="text/javascript">
       setTimeout(function() {
			document.getElementById("polling-form").submit();
		},5000);
    </script> 
	<%
			break;
		case STOPPED_BY_ERROR:
	%>
	Calculation stopped. An error occurred.
	<p>
	<form method="post">
		<input name="<%=TEXT_PARAM_NAME%>" value="" type="hidden"> <input
			name="<%=PIPELINE_ID_PARAM_NAME%>" value="" type="hidden"> <input
			type="submit" value="Do it again">
	</form>
	<p>error info:
	<p>
		<%=jobInfo.getError()%>
		<%
			break;
		case STOPPED_BY_REQUEST:
		%>
		Calculation stopped by request;
	<p>
	<form method="post">
		<input name="<%=TEXT_PARAM_NAME%>" value="" type="hidden"> <input
			name="<%=PIPELINE_ID_PARAM_NAME%>" value="" type="hidden"> <input
			type="submit" value="Do it again">
	</form>
	<%
			break;
		}// end switch

		if (null != pipelineId) {
	%>
	<p>
		<a href="/_ah/pipeline/status.html?root=<%=pipelineId%>"
			target="Pipeline Status">view status page</a>
		<%
		}
		%>
	
</BODY>
</HTML>