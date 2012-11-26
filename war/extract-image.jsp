<%@page import="com.google.appengine.api.datastore.Entity"%>
<%@page import="java.util.List"%>
<%@page import="fr.telecomParistech.dash.mpd.MPD"%>
<%@ page import="com.google.appengine.tools.pipeline.*" %>
<%@ page import="fr.telecomParistech.mapreduce.*" %>
<%@ page import="com.google.appengine.tools.pipeline.demo.LetterCountExample.LetterCounter" %>
<%@ page import="java.util.SortedMap" %>

<%!
    private static final String TEXT_PARAM_NAME = "mpdLocation";
    private static final String PIPELINE_ID_PARAM_NAME = "pipelineId";
    private static final String CLEANUP_PIPELINE_ID_PARAM_NAME = "cleanupId";
    private static final PipelineService service = 
    		PipelineServiceFactory.newPipelineService();

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
	String text = request.getParameter(TEXT_PARAM_NAME);
	if (text != null && 
			(text.equalsIgnoreCase("null") || text.equals(""))) {
		text = null;
	}
			
	String pipelineId = request.getParameter(PIPELINE_ID_PARAM_NAME);
	if (pipelineId != null && 
			(pipelineId.equalsIgnoreCase("null") || pipelineId.equals(""))) {
		pipelineId = null;
	}
	
	String cleanupId = request.getParameter(CLEANUP_PIPELINE_ID_PARAM_NAME);
	if (cleanupId != null && 
			(cleanupId.equalsIgnoreCase("null") || cleanupId.equals(""))) {
		cleanupId = null;
	}
	
	if (null != cleanupId) {
		service.deletePipelineRecords(cleanupId);
	}
	
    if (null != text) {
%>
<H4>Extraction Image from:</H4>
<em><%=text%>
</em>

<p>

<%
  if(null == pipelineId){
    pipelineId = service.startNewPipeline(new ImageExtractorPipeline(), text);
  }
  JobInfo jobInfo = service.getJobInfo(pipelineId);
  switch(jobInfo.getJobState()){
        case COMPLETED_SUCCESSFULLY:
%>
    Computation completed.

<p>
<%
	List<Entity> entityList = (List<Entity>) jobInfo.getOutput();
    for (Entity e : entityList) {
    	out.print("Entity: ");
    	out.println(e.toString());
    }
  
%>

<form method="post">
    <input name="<%=TEXT_PARAM_NAME%>" value="" type="hidden">
    <input name="<%=PIPELINE_ID_PARAM_NAME%>" value="" type="hidden">
    <input name="<%=CLEANUP_PIPELINE_ID_PARAM_NAME%>" value="<%=pipelineId%>" type="hidden">
    <input type="submit" value="Do it again">
</form>
<%
        break;
    case RUNNING:
%>
Calculation not yet completed.
<p>

<form method="post">
    <input name="<%=TEXT_PARAM_NAME%>" value="<%=text%>" type="hidden">
    <input name="<%=PIPELINE_ID_PARAM_NAME%>" value="<%=pipelineId%>" type="hidden">
    <input type="submit" value="Check Again">
</form>
<%
        break;
    case STOPPED_BY_ERROR:
%>
Calculation stopped. An error occurred.
<p>

<form method="post">
    <input name="<%=TEXT_PARAM_NAME%>" value="" type="hidden">
    <input name="<%=PIPELINE_ID_PARAM_NAME%>" value="" type="hidden">
    <input type="submit" value="Do it again">
</form>
<p>
    error info:

<p>
        <%=jobInfo.getError()%>
        <%
          break;
        case STOPPED_BY_REQUEST:
%>
    Calculation stopped by request;

<p>

<form method="post">
    <input name="<%=TEXT_PARAM_NAME%>" value="" type="hidden">
    <input name="<%=PIPELINE_ID_PARAM_NAME%>" value="" type="hidden">
    <input type="submit" value="Do it again">
</form>
<%
            break;
    }// end switch
}// end: if
else {
%>
Enter a MPD URL:
<form method="post">
    <textarea name="<%=TEXT_PARAM_NAME%>" cols=100></textarea>
    <br>
    <input type="submit" value="Get Image">
</form>
<%
    }

    if (null != pipelineId) {
%>
<p>
    <a href="/_ah/pipeline/status.html?root=<%=pipelineId%>" target="Pipeline Status">view status
        page</a>
        <%
}
%>


</BODY>
</HTML>