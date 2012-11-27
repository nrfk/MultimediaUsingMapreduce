package fr.telecomParistech.mapreduce;

import com.google.appengine.tools.mapreduce.Reducer;
import com.google.appengine.tools.mapreduce.ReducerInput;

public class ImageExtractorReducer extends Reducer<Integer, String, String> {
	private static final long serialVersionUID = 3748003219458311578L;

	@Override
	public void reduce(Integer key, ReducerInput<String> values) {
		
	}


}
