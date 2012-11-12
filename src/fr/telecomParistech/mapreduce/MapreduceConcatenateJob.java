package fr.telecomParistech.mapreduce;


import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.KeyValue;
import com.google.appengine.tools.mapreduce.MapReduceJob;
import com.google.appengine.tools.mapreduce.MapReduceResult;
import com.google.appengine.tools.mapreduce.MapReduceSettings;
import com.google.appengine.tools.mapreduce.MapReduceSpecification;
import com.google.appengine.tools.pipeline.Value;

public class MapreduceConcatenateJob extends 
		MapReduceJob<Entity, 
					 String, 
					 KeyValue<Long,String>, 
					 String, 
					 List<List<String>>> {
	private static final long serialVersionUID = 5997153602540614532L;

	@Override
	public Value<MapReduceResult<List<List<String>>>> run(
			MapReduceSpecification<
					Entity, 
					String, 
					KeyValue<Long, String>, 
					String, 
					List<List<String>>> mrSpec,
			MapReduceSettings settings) {
		// TODO Auto-generated method stub
		return super.run(mrSpec, settings);
	}	
}
