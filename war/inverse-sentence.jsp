<%@ page import="com.google.appengine.tools.pipeline.*" %>
<%@ page import="fr.telecomParistech.example.pipeline.*" %>
<%@ page import="fr.telecomParistech.example.pipeline.InverseSentenceExample.InverseSentence" %>

<%!
	private static final String TEXT_PARAM_NAME = "text";
	private static final String PIPELINE_ID_PARAM_NAME = "pipelineId";
	private static final String CLEANUP_PIPELINE_ID_PARAM_NAME = "cleanupId";
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
	<H2>Inverse sentence by spanwing a sub-job for inversing each word</H2>
	
<%
	String text = request.getParameter(TEXT_PARAM_NAME);
	if (text != null && (text.equalsIgnoreCase("null") || text.equals(""))) {
		text = null;
	}
			
	String pipelineId = request.getParameter(PIPELINE_ID_PARAM_NAME);
	if (pipelineId != null && (pipelineId.equalsIgnoreCase("null") || pipelineId.equals(""))) {
		pipelineId = null;
	}
	
	String cleanupId = request.getParameter(CLEANUP_PIPELINE_ID_PARAM_NAME);
	if (cleanupId != null && (cleanupId.equalsIgnoreCase("null") || cleanupId.equals(""))) {
		cleanupId = null;
	}
	
	PipelineService service = PipelineServiceFactory.newPipelineService();
	
	if (null != cleanupId) {
		service.deletePipelineRecords(cleanupId);
	}
	
	if (null != text) { // begin if
%>
	<H4>Computing letter counts...</H4>
	
	<em><%=text%></em>
	
	<p>
<%
		if (null == pipelineId) {
			pipelineId = service.startNewPipeline(new InverseSentence(), text);
		}

		JobInfo jobInfo = service.getJobInfo(pipelineId);
		switch (jobInfo.getJobState()) {
		case COMPLETED_SUCCESSFULLY:
%>
			Processing completed.
			
<%
			String inversedSentence = (String) jobInfo.getOutput();
			out.println("The inversed sentence is: ");
%>
	<p>
<%
			out.println(inversedSentence);
%>	

	<form method="post">
		<input name="<%=TEXT_PARAM_NAME%>" value="" type="hidden">
	    <input name="<%=PIPELINE_ID_PARAM_NAME%>" value="" type="hidden">
	    <input name="<%=CLEANUP_PIPELINE_ID_PARAM_NAME%>" value="<%=cleanupId%>" type="hidden">
	    <input type="submit" value="Do it again">
	</form>

<%
			break;
		case RUNNING:
%>

	Processing not yet completed.
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
	<form method="post">
	    <input name="<%=TEXT_PARAM_NAME%>" value="" type="hidden">
	    <input name="<%=PIPELINE_ID_PARAM_NAME%>" value="" type="hidden">
	    <input type="submit" value="Do it again">
	</form>
	<p>
	    error info:
	<p>
	<%= jobInfo.getError() %>	

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
		default: 
			break;
		} // end switch
%>
<%
	} else {  // end if, continue with else
%>
	Enter some text:
	<form method="post">
	    <textarea name="<%=TEXT_PARAM_NAME%>" cols=40 rows=6></textarea>
	    <br>
	    <input type="submit" value="Compute Letter Count">
	</form>
<%
    } // end if

    if (pipelineId != null) {
%>
	<p>
    <a href="/_ah/pipeline/status.html?root=<%=pipelineId%>" target="Pipeline Status">view status page</a>
<%
    }
%>
</BODY>
</HTML>