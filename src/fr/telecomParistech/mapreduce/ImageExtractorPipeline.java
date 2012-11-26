package fr.telecomParistech.mapreduce;

import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.pipeline.FutureValue;
import com.google.appengine.tools.pipeline.Job1;
import com.google.appengine.tools.pipeline.Value;

import fr.telecomParistech.dash.mpd.MPD;

public class ImageExtractorPipeline extends Job1<List<Entity>, String>{
	private static final long serialVersionUID = 1L;
	public Value<List<Entity>> run(String mpdLocation) {
		FutureValue<MPD> mpd = 
				futureCall(new JobDownloadingMpd(), immediate(mpdLocation));
		
		FutureValue<List<Entity>> entityList = 
				futureCall(new JobParsingMpd(), mpd, immediate(mpdLocation));
		
		futureCall(new JobSavingVideoData(), entityList);
		
		// Create Map Reduce task
		
		return entityList;
	}


	
}
