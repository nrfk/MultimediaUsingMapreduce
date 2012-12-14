<%@page import="javax.persistence.criteria.CriteriaBuilder.In"%>
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

<%!
	private static final String TEXT_PARAM_NAME = "mpdLocation";
	// Atrributes set by ParserServlet
	private static final String SEGMENT_COUNTER = "segmentCounter";
	private static final String SEGMENT_PATHS = "segmentPaths";
	private static final String SESSION_ID = "sessionId";
	
	// Attributes set by MapReduceServlet
	private static final String PIPELINE_ID_PARAM_NAME = "pipelineId";
	private static final String CLEANUP_PIPELINE_ID_PARAM_NAME = "cleanupId";
	
	// Attribute in config file
	private static final int IMAGES_PER_ROW;
	private static final int IMAGES_WIDTH;
	private static final int IMAGES_HEIGHT;
	
	// Some Google Service
	private static final PipelineService service = PipelineServiceFactory
			.newPipelineService();
	
	// Configuration-related properties
	private static final Logger log;
	private static final String CONFIG_FILE="WEB-INF/mapreduce-config.xml";
	private static final XMLConfiguration mapreduceConfig;
	private static final TimeUnit timeUnit;
	private static final boolean USE_BACKENDS = false;
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
				
				String logLevel = 
						mapreduceConfig.getString("log.level-display", "INFO");
				log.setLevel(Level.parse(logLevel));
			} else {
				IMAGES_PER_ROW = 4; 
				IMAGES_WIDTH = 150;
				IMAGES_HEIGHT = 150;
				timeUnit = TimeUnit.SECONDS;
			}
		}
	}
	
	%>
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
	//number of image, as we will display each image per media segment 
	// the number of image is equal to number of segment
	int nImages =  Integer.parseInt(request.getParameter(SEGMENT_COUNTER));
	String pipelineId = request.getParameter("pipelineId");
	String fullPathList = request.getParameter("fullPathList");
	String[] segmentPaths = fullPathList.split(" ");
	
	// Calculate rows of image 
	int imageRows = (int) Math.round(Math.ceil((float)nImages/IMAGES_PER_ROW));
%>
	<p> Result for calculation #<span id="pipelineId"><%=pipelineId%></span> is:<p>
<%	
	// Create table of result
	out.println("<table border='1'>");
	for (int i = 0; i < imageRows; i++) {
		out.print("<tr>");
		for (int j = 0; j < IMAGES_PER_ROW; j++) {
			int index = i*IMAGES_PER_ROW + j;
			if (nImages > 0) { // Placeholder
				out.print("<td ><image height='" + IMAGES_HEIGHT + " ' width='"+IMAGES_WIDTH+"' src='/blobstore-reader-servlet?blobPath=" + segmentPaths[index]  + "' alt='image loading...'></td>");
				nImages--;
			} else { // Padding
				out.print("<td height='" + IMAGES_HEIGHT + " ' width='"+IMAGES_WIDTH+"' > This is an align cell  </td>");
			}
		}
		out.print("</tr>");
	}
	out.print("</table>");
%>
	<script type="text/javascript" src="cookie.js"></script> 
	<script type="text/javascript">
	
		var result;
		if (window.XMLHttpRequest) {
			// code for IE7+, Firefox, Chrome, Opera, Safari
			xmlhttp=new XMLHttpRequest();
		} else {// code for IE6, IE5
			xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
		}
		xmlhttp.onreadystatechange=function() {
			if (xmlhttp.readyState==4 && xmlhttp.status==200) {
				result = xmlhttp.responseText;
				result = result.toUpperCase();
				if (result != "COMPLETED_SUCCESSFULLY") {
					setTimeout(function() {
						document.location.reload(true);
					},5000);
				}
			}
		}
		var url = "/check-task?pipelineId=" + document.getElementById("pipelineId").innerHTML;
		<%-- xmlhttp.open("GET","/check-task?pipelineId=" + ,true); --%>
		/* xmlhttp.open("GET","/check-task",true); */
		xmlhttp.open("GET",url,true);
		xmlhttp.send();
		xmlDoc=xmlhttp.responseXML;
    </script> 
</BODY>
</HTML>

























