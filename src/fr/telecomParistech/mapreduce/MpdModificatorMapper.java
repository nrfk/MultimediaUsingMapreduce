package fr.telecomParistech.mapreduce;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.KeyValue;
import com.google.appengine.tools.mapreduce.Mapper;

public class MpdModificatorMapper 
		extends Mapper<Entity, Integer, KeyValue<String, String>> {
	private static final long serialVersionUID = 7197212312838140497L;

	@Override public void map(Entity value) {
		
	}

}
