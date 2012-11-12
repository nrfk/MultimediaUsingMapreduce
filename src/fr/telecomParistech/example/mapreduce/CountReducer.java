package fr.telecomParistech.example.mapreduce;

import com.google.appengine.tools.mapreduce.KeyValue;
import com.google.appengine.tools.mapreduce.Reducer;
import com.google.appengine.tools.mapreduce.ReducerInput;

import java.util.logging.Logger;

class CountReducer extends Reducer<String, Long, KeyValue<String, Long>> {
	private static final long serialVersionUID = 1316637485625852869L;

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(CountReducer.class.getName());

	public CountReducer() {
	}


	@Override 
	public void beginShard() {
	}

	@Override 
	public void beginSlice() {
	}

	@Override
	public void reduce(String key, ReducerInput<Long> values) {
		// TODO Auto-generated method stub
		long total = 0;
		while (values.hasNext()) {
			total += values.next();
		}
		
		getContext().emit(KeyValue.of(key, total));
		
	}

	@Override 
	public void endShard() {
	}

	@Override 
	public void endSlice() {
	}





}
