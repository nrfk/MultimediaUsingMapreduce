<%!
	private static final String SENDER_URL = "senderUrl";
	private static final String STATUS = "status";
	private static final String MALFORMED_URL = "malformedUrl";
	private static final String DEFAULT_LABEL = "Please choose:";
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
	<H2>Simple Image Generator</H2>
	<em > <%= label%> </em>
	<form action="image-generator-servlet" method="post">
				</br>
				Color:
				</br>
				<input type="radio" name="color" value="red">Red<br>
				<input type="radio" name="color" value="green" checked="checked">Green<br>
				<input type="radio" name="color" value="blue">Blue<br>
				<input type="radio" name="color" value="random">Random<br>
				</br>
				Size: 
				</br>
				<input type="radio" name="size" value="large">Large (640 x 480)<br>
				<input type="radio" name="size" value="medium" checked="checked">Medium (320 x 240)<br>
				<input type="radio" name="size" value="small">Small:  (160 x 120)<br>
				<input type="radio" name="size" value="tiny">Tiny:   (80 x 60)<br>
				</br>
       <input type="submit" value="Submit">
   </form>
	
</BODY>