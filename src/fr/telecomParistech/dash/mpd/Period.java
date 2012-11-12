package fr.telecomParistech.dash.mpd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Period implements Serializable{
	private static final long serialVersionUID = 7892900291066412819L;
	private List<AdaptationSet> adaptationSets;
	private Map<String, String> attributes;
	private int id;	
	
	public Period(int id) {
		adaptationSets = new LinkedList<AdaptationSet>();
		attributes = new HashMap<String, String>();
		this.id = id;
	}

	public void addAdaptationSet(AdaptationSet adaptationSet) {
		adaptationSets.add(adaptationSet);
	}
	
	public void removeAdaptationSet(AdaptationSet adaptationSet) {
		adaptationSets.remove(adaptationSet);
	}
	
	public AdaptationSet getAdaptationSet(int id) {
		if (adaptationSets == null || adaptationSets.size() == 0) 
			return null;
		
		for (AdaptationSet a : adaptationSets) {
			if (a.getId() == id) return a; 
		}
		
		return null;
	}
	
	public List<AdaptationSet> getAllAdaptationSet() {
		return adaptationSets;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void addAttribute(String name, String value) {
		attributes.put(name, value);
	}
	
	public String getAttribute(String name) {
		return attributes.get(name);
	}
	
	public String toString() {
		
		String s = "";
		s += "Period:\n";
		s += "\t id: " + getId() + "\n";
		
		Set<String> keys = attributes.keySet();
		Iterator<String> i = keys.iterator();
		while (i.hasNext()) {
			String key = i.next();
			String value = attributes.get(key);
			s += "\t " + key + ": " + value + "\n";
		}
		
		for (AdaptationSet a : adaptationSets) {
			s += a.toString();
		}
		
		return s;
	}
}
