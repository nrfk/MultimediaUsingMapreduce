<HTML>

<%!
    private static final String TEXT_PARAM_NAME = "text";
    private static final String PIPELINE_ID_PARAM_NAME = "pipelineId";
    private static final String CLEANUP_PIPELINE_ID_PARAM_NAME = "cleanupId";
%>
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
	<H2>Compute letter counts using Map Reduce</H2>
<%
	String text = request.getParameter(TEXT_PARAM_NAME);
	if (text == null || "".equals(text) || "null".equalsIgnoreCase(text)) {
		text = null;
	}

	if (null != text) { // Begin if 1
	
	
%>
	<H4>You've entered:</H4>
	
	<em><%=text%></em>
	
	<H4>Count character using Map Reduce ?</H4>
	<form method="post" action="/character-counter-servlet">
	    <input name="<%=TEXT_PARAM_NAME%>" value="<%=text%>" type="hidden">
	    <input type="submit" value="Yes, I want to count!"> 
	</form>
	
	<H4>No, I want to enter a new text:</H4>
	
	<form method="post">
	    <textarea name="<%=TEXT_PARAM_NAME%>" cols=40 rows=6></textarea>
	    <br>
	    <input type="submit" value="OK, use this text">
	</form>
<%
	} else { // else if 1
%>
	Enter some text:
	<form method="post">
	    <textarea name="<%=TEXT_PARAM_NAME%>" cols=40 rows=6></textarea>
	    <br>
	    <input type="submit" value="OK">
	</form>
<%
	} // end if 1
%>
</BODY>