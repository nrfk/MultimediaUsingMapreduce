<%!
	private static final String MPD_FILE_NAME = "text";
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
	<H2>Process Dash</H2>
	Please enter a blob url: 
	<form action="read-blob-servlet" method="post" >
       <textarea name="blobPath" cols=40 rows=6></textarea>
       <input type="submit" value="Submit">
    </form>
	
</BODY>