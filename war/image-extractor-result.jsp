<%@page import="com.google.appengine.api.datastore.Key"%>
<%@page import="com.google.appengine.api.datastore.KeyFactory"%>
<%@page import="com.google.appengine.api.datastore.DatastoreServiceFactory"%>
<%@page import="com.google.appengine.api.datastore.DatastoreService"%>
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
	private static final DatastoreService datastoreService = 
			DatastoreServiceFactory.getDatastoreService();
	
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
</HEAD>
<BODY>
	<H2>Image Extractor Service</H2>
<%
	// Enable cache
	response.addHeader("Cache-Control", "public");
	String pipelineId = request.getParameter("pipelineId");
	// Get image list
	String imageInfoKeyStr = request.getParameter("imageInfoKey");
	Key imageInfoKey = KeyFactory.stringToKey(imageInfoKeyStr);
	Entity imageInfoEntity = datastoreService.get(imageInfoKey);
	
	// We save imageList whose type is List<String> in MPDParserServlet, 
	// so we can now safety cast Object --> List<String>
	@SuppressWarnings("unchecked")
	List<String> imageList = 
			(List<String>) imageInfoEntity.getProperty("imageList");
	
	//number of image, as we will display each image per media segment 
		// the number of image is equal to number of segment
		int nImages =  imageList.size();
	
	// Calculate rows of image 
	int imageRows = (int) Math.round(Math.ceil((float)nImages/IMAGES_PER_ROW));
	long refreshTime = mapreduceConfig.getLong("result.refresh-time",3000);
%>
	
	<p id="resultTag"> Result:<p>
	<p id="placeholder"></p>
	<!-- Save some info in hidden span, to later use, because JSP and 
	javascript doesn't mix well -->
	<span id="nImages" style="visibility:hidden"><%=nImages%></span>
	<span id=imageInfoKey style="visibility:hidden"><%=imageInfoKeyStr%></span>
	<span id="isImageCached" style="visibility:hidden">false</span>
	<span id="pipelineId" style="visibility:hidden"><%=pipelineId%></span>
	<span id="refreshTime" style="visibility:hidden"><%=refreshTime%></span>
	<script type="text/javascript" src="cookie.js"></script>
	<script type="text/javascript">
		
		var result;
		var i;
		var nImages = document.getElementById("nImages").innerHTML;
		var isImageCached = (document.getElementById("isImageCached").innerHTML == "true");
		if (!isImageCached) {
			for (i = 0; i < nImages; i++) {
				eraseCookie("image" + i);
			}	
			eraseCookie("loadedImage");
			document.getElementById("isImageCached").innerHTML = true;
		}
		
		
		
		if (window.XMLHttpRequest) {
			// code for IE7+, Firefox, Chrome, Opera, Safari
			xmlhttp=new XMLHttpRequest();
		} else {// code for IE6, IE5
			xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
		}
		
		
		xmlhttp.onreadystatechange=function() {
			if (xmlhttp.readyState==4 && xmlhttp.status==200) {
				var resultStr = xmlhttp.responseText;
				var result = eval("(" + resultStr + ")");
				
				var status = "" + result.status;
				var nImages = result.nImages;
				var loadedImage = readCookie("loadedImage");
				if (loadedImage == null) {
					loadedImage = 0;
				}
				
				if ((status.toUpperCase() != "COMPLETED_SUCCESSFULLY")
						&& (loadedImage != nImages)) {
					
					var images = result.images;
					for (name in images) {
						if (typeof images[name] !== 'function') {
							if (readCookie(name) == null) {
								//document.getElementById("image" + i).innerHTML = images[name];
								
					
								var img = document.createElement("img");
						        img.src = images[name];
						        img.width = 200;
						        img.height = 200;
						        img.alt = "Logo";
						        document.getElementById("placeholder").appendChild(img);
						        //document.body.appendChild(img);
								loadedImage = loadedImage + 1;
								createCookie("loadedImage", loadedImage, 1);
								createCookie(name, images[name], 1);
							}
						}
					}
					setTimeout(function() {
						// document.location.reload(false);
						
						var url = "/check-task?pipelineId=" + document.getElementById("pipelineId").innerHTML + 
						"&imageInfoKey=" + document.getElementById("imageInfoKey").innerHTML;
						/* xmlhttp.open("GET","/check-task",true); */
						xmlhttp.open("GET",url,true);
						xmlhttp.send();
						xmlDoc=xmlhttp.responseXML;
						
						
					},document.getElementById("refreshTime").innerHTML);
				}
			}
		}
		var url = "/check-task?pipelineId=" + document.getElementById("pipelineId").innerHTML + 
				"&imageInfoKey=" + document.getElementById("imageInfoKey").innerHTML;
		/* xmlhttp.open("GET","/check-task",true); */
		xmlhttp.open("GET",url,true);
		xmlhttp.send();
		xmlDoc=xmlhttp.responseXML;
    </script> 
    <%
		
		for (int i = 0; i < imageList.size(); i++) {
			out.print("<span id='image"+ i +"' style='visibility:hidden'>" + imageList.get(i) + "</span>");
		}
	
	%> 
	
</BODY>
</HTML>

























