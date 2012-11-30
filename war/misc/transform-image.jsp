<%!
	private static final String SENDER_URL = "senderUrl";
	private static final String STATUS = "status";
	private static final String MALFORMED_URL = "malformedUrl";
	private static final String DEFAULT_LABEL = "Please enter the url of the image:";
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
<%
	String label = request.getParameter(STATUS);
	if (label == null) {
		label = DEFAULT_LABEL;
	}
	if ( label.equals(MALFORMED_URL)) {
		label = "Cannot parse url, please enter a valid url: ";
	} else {
		label = DEFAULT_LABEL;
	}
%>
<BODY>
	<H2>Process Dash</H2>
	<em > <%= label%> </em>
	<form action="transform-image-servlet" method="post">
       <input type="text" name="<%= SENDER_URL %>" size="50">
       </br>
				Choose one type of transformation
       </br>
				<input type="radio" name="transform" value="vertical">Make Vertical Flip<br>
				<input type="radio" name="transform" value="horizontal" checked="checked">Make Horizontal Flip<br>
				<input type="radio" name="transform" value="rotate">Rotate 90 degree<br>
				</br>
       <input type="submit" value="Submit">
   </form>
	
</BODY>