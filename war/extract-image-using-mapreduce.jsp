<%@page
	import="com.google.appengine.tools.mapreduce.outputs.InMemoryOutput"%>
<%@page
	import="com.google.appengine.tools.mapreduce.inputs.DatastoreInput"%>
<%@page import="com.google.appengine.tools.mapreduce.MapReduceJob"%>
<%@page import="com.google.appengine.tools.mapreduce.MapReduceResult"%>
<%@page import="com.google.appengine.api.datastore.Entity"%>
<%@page import="java.util.List"%>
<%@page import="fr.telecomParistech.dash.mpd.MPD"%>
<%@page import="com.google.appengine.tools.pipeline.*"%>
<%@page import="fr.telecomParistech.mapreduce.*"%>
<%@page
	import="com.google.appengine.tools.pipeline.demo.LetterCountExample.LetterCounter"%>
<%@page import="java.util.SortedMap"%>
<%@page import="com.google.appengine.tools.mapreduce.*"%>


<%!private static final String TEXT_PARAM_NAME = "mpdLocation";
	private static final String PIPELINE_ID_PARAM_NAME = "pipelineId";
	private static final String CLEANUP_PIPELINE_ID_PARAM_NAME = "cleanupId";
	private static final PipelineService service = PipelineServiceFactory
			.newPipelineService();
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

		if (null != cleanupId) {
			service.deletePipelineRecords(cleanupId);
		}
	%>
	<H4>Extraction Image from:</H4>

	<p>

		<%
		if (null == pipelineId) {
			int mapShardCount = 4;
			int reduceShardCount = 1;
			MapReduceSpecification<Entity, Integer, String, String, List<List<String>>> mrSpec = MapReduceSpecification
					.of("Image Extractor", new DatastoreInput(
							"MediaSegmentInfo", mapShardCount),
							new ImageExtractorMapper(), Marshallers
									.getIntegerMarshaller(), Marshallers
									.getStringMarshaller(),
							new ImageExtractorReducer(),
							new InMemoryOutput<String>(reduceShardCount));

			MapReduceSettings settings = new MapReduceSettings()
					.setWorkerQueueName("mapreduce-workers")
					.setControllerQueueName("default");
			if (USE_BACKENDS) {
				settings.setBackend("worker");
			}
			pipelineId = service
					.startNewPipeline(
							new MapReduceJob<Entity, Integer, String, String, List<List<String>>>(),
							mrSpec, settings);
		}

		JobInfo jobInfo = service.getJobInfo(pipelineId);
		switch (jobInfo.getJobState()) {
		case WAITING_TO_RETRY:
			// Do nothing
			break;

		case COMPLETED_SUCCESSFULLY:
		%>
			Computation completed.
	<p>
		<%
			MapReduceResult<List<List<String>>> mrResult = (MapReduceResult<List<List<String>>>) jobInfo
						.getOutput();
				String result = mrResult.getOutputResult().get(0).get(0);
				out.print(result);
		%>
	
	<form method="post">
		<input name="<%=TEXT_PARAM_NAME%>" value="" type="hidden"> <input
			name="<%=PIPELINE_ID_PARAM_NAME%>" value="" type="hidden"> <input
			name="<%=CLEANUP_PIPELINE_ID_PARAM_NAME%>" value="<%=pipelineId%>"
			type="hidden"> <input type="submit" value="Do it again">
	</form>
	<%
			break;
		case RUNNING:
	%>
	Calculation not yet completed.
	<p>
	<form method="post">
		<input name="<%=TEXT_PARAM_NAME%>" value="<%=text%>" type="hidden">
		<input name="<%=PIPELINE_ID_PARAM_NAME%>" value="<%=pipelineId%>"
			type="hidden"> <input type="submit" value="Check Again">
	</form>
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