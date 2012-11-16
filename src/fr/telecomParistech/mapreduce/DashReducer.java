package fr.telecomParistech.mapreduce;

import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.tools.mapreduce.KeyValue;
import com.google.appengine.tools.mapreduce.Reducer;
import com.google.appengine.tools.mapreduce.ReducerInput;

/**
 * DashReducer
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class DashReducer extends Reducer<String, KeyValue<Long, String>, String>{
	private static final long serialVersionUID = 527411575120249415L;
	private static final FileService fileService = 
			FileServiceFactory.getFileService();

	//public class DashReducer extends Reducer<String, String, String>{

	@Override
	public void reduce(String key, ReducerInput<KeyValue<Long, String>> values){
		//	public void reduce(String key, ReducerInput<String> values) {

		try {
			String html = "<Representation>\n";
			html += "	<description>\n";
			html += "		" + key + "\n";
			html += "	</description>\n";
			// Collect and sort data
			SortedMap<Long, String> sortedMap = new TreeMap<Long, String>();
			while (values.hasNext()) {
				KeyValue<Long, String> keyvalue = values.next();
				Long index = keyvalue.getKey();
				String value = keyvalue.getValue();
				sortedMap.put(index, value);
			}

			// Create HTML from sorted data
			for (Entry<Long, String> entry : sortedMap.entrySet()) {
				html += "	<mediaSegment>\n";
				html += "		<index>" + entry.getKey() + "</index>\n";
				html += "		<location>" + entry.getValue() + "</location>\n";
				html += "	</mediaSegment>\n";
			}

			// Create a new Blob file with mime-type "text/plain"
			AppEngineFile file = fileService.createNewBlobFile("text/xml");

			// Open a channel to write to it
			boolean lock = true;
			FileWriteChannel writeChannel = 
					fileService.openWriteChannel(file, lock);

			// Different standard Java ways of writing to the channel
			// are possible. Here we use a PrintWriter:
			PrintWriter out = 
					new PrintWriter(Channels.newWriter(writeChannel, "UTF8"));

			

			html += "</Representation>\n";
//			System.out.println("path: " + file.getFullPath());
			
//			System.out.println(html);
			out.print(html);
			out.close();
			writeChannel.closeFinally();
			getContext().emit(html);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}



//try {
//	String html = "<HTML>\n<HEAD>\n" + "Segment list of: " + key + "\n</HEAD>\n<BODY>\n<PRE>\n";
//	html += "<table border>\n";
//
//	// Collect and sort data
//	SortedMap<Long, String> sortedMap = new TreeMap<Long, String>();
//	while (values.hasNext()) {
//		KeyValue<Long, String> keyvalue = values.next();
//		Long index = keyvalue.getKey();
//		String value = keyvalue.getValue();
//		sortedMap.put(index, value);
//	}
//
//	// Create HTML from sorted data
//	for (Entry<Long, String> entry : sortedMap.entrySet()) {
//		html += "<tr>\n";
//		html += "<td>" + entry.getKey() + "</td>\n";
//		html += "<td>" + entry.getValue() + "</td>\n";
//		html += "</tr>\n";
//	}
//
//	// Create a new Blob file with mime-type "text/plain"
//	AppEngineFile file = fileService.createNewBlobFile("text/html");
//
//	// Open a channel to write to it
//	boolean lock = true;
//	FileWriteChannel writeChannel = fileService.openWriteChannel(file, lock);
//
//	// Different standard Java ways of writing to the channel
//	// are possible. Here we use a PrintWriter:
//	PrintWriter out = new PrintWriter(Channels.newWriter(writeChannel, "UTF8"));
//
//	
//
//	html += "</table>\n";
//	html += "</PRE></BODY>\n</HTML>\n";
//	System.out.println("path: " + file.getFullPath());
//	
//	System.out.println(html);
//	out.print(html);
//	out.close();
//	writeChannel.closeFinally();
//	getContext().emit(file.getFullPath());
//} catch (Exception e) {
//	e.printStackTrace();
//}
