package fr.telecomParistech.mapreduce;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import com.google.appengine.tools.pipeline.FutureValue;
import com.google.appengine.tools.pipeline.ImmediateValue;
import com.google.appengine.tools.pipeline.Job1;
import com.google.appengine.tools.pipeline.Value;

import fr.telecomParistech.dash.mpd.MPD;
import fr.telecomParistech.dash.mpd.MPDParser;

public class ImageExtractorPipeline extends Job1<MPD, String>{
	private static final long serialVersionUID = 1L;

	public Value<MPD> run(String mpdLocation) {
		Value<MPD> mpd = 
				futureCall(new DownloadingMpdJob(), immediate(mpdLocation));
		
//		System.out.println(mpd.toString());
//		System.out.println("*******************************");
		return mpd;
	}


	private class DownloadingMpdJob extends Job1<MPD, String>{
		private static final long serialVersionUID = 3232685144696876419L;

		public Value<MPD> run(String mpdLocation) {
			URL url = null;
			MPD mpd = null;
			
			try {
				url = new URL(mpdLocation);
				InputStream inputStream = url.openStream();
				mpd = MPDParser.parseMPD(inputStream);
			} catch (MalformedURLException e) {
				
			} catch (IOException e) {
				
			}
			return immediate(mpd);
		}


	}
	
}
