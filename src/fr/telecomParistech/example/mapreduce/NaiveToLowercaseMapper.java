package fr.telecomParistech.example.mapreduce;

import java.util.Date;
import java.util.logging.Logger;

import org.apache.commons.io.output.NullWriter;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.Mapper;

@SuppressWarnings("serial")
public class NaiveToLowercaseMapper extends Mapper<Entity, NullWriter, NullWriter> {
	private static final Logger log = Logger.getLogger(NaiveToLowercaseMapper.class.getName());
	private DatastoreService datastore;
	
	@Override
	public void beginShard() {
		// TODO Auto-generated method stub
		this.datastore = DatastoreServiceFactory.getDatastoreService();
	}
	
	@Override
	public void map(Entity value) {
		// TODO Auto-generated method stub
		log.info("Mapping: " + value.getKey().toString());
		if (value.hasProperty("comment")) {
			String comment = (String) value.getProperty("comment");
			comment = comment.toLowerCase();
			value.setProperty("comment", comment);
			value.setProperty("updateAt", new Date());
			
			datastore.put(value);
		}
	}

	

}
