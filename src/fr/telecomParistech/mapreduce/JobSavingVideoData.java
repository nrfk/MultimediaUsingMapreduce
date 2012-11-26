package fr.telecomParistech.mapreduce;

import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.DatastoreMutationPool;
import com.google.appengine.tools.pipeline.Job1;
import com.google.appengine.tools.pipeline.Value;

/* packet-private */ class JobSavingVideoData 
		extends Job1<Void, List<Entity>> {
	private static final long serialVersionUID = -4270133947832717398L;
	private static final transient DatastoreMutationPool pool =
			DatastoreMutationPool.forManualFlushing();
	@Override
	public Value<Void> run(List<Entity> entityList) {
		for (Entity entity : entityList) {
			System.out.println(entity.toString());
			pool.put(entity);
		}
		pool.flush();
		return null;
	}
	
}
