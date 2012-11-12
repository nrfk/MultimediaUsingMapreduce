package fr.telecomParistech.example.mapreduce;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.Mapper;

class CountMapper extends Mapper<Entity, String, Long> {
	private static final long serialVersionUID = 4973057382538885270L;

	private static final Logger log = Logger.getLogger(CountMapper.class.getName());

	public CountMapper() {
	}

	private void incrementCount(char c, int increment, Map<Character, Integer> charMap) {
		Integer countInteger = charMap.get(c);
		int count = (null == countInteger ? 0 : countInteger) + increment;
		charMap.put(c, count);
	}

	public SortedMap<Character, Integer> countLetters(String text) {
	    SortedMap<Character, Integer> charMap = new TreeMap<Character, Integer>();
	    for (char c : text.toCharArray()) {
	      incrementCount(c, 1, charMap);
	    }
	    return charMap;
	  }
	
	@Override 
	public void beginShard() {
		log.info("beginShard()");
	}

	@Override 
	public void beginSlice() {
		log.info("beginSlice()");
	}

	@Override public void map(Entity entity) {
		log.info("map, count character in word: " + entity.getProperty("word"));
		String word = (String) entity.getProperty("word");
		for (char c : word.toCharArray()) {
		      getContext().emit("" + c, (long) 1);
	    }
	}

	@Override 
	public void endShard() {
		log.info("endShard()");
	}

	@Override 
	public void endSlice() {
		log.info("endSlice()");
	}

}
