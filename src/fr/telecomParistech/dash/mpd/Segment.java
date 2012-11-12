package fr.telecomParistech.dash.mpd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class Segment implements Serializable {
	private static final long serialVersionUID = -3211862724639537468L;
	public static final String UNKNOWN_URL = "";
	// id of the segment
	private int id; 
	private Map<String, String> attributes;
	
	public Segment(int id) {
		this.id = id;
		attributes = new HashMap<String, String>();
	}
	
	public void addAttribute(String name, String value) {
		attributes.put(name, value);
	}
	
	public String getAttribute(String name) {
		System.out.println("attttttt: " + attributes.get("sourceURL"));
		return attributes.get(name);
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}	
	
	public String toString() {
		String s = "";
		s += "\t id: " + id + "\n";
		Set<String> keys = attributes.keySet();
		Iterator<String> i = keys.iterator();
		while (i.hasNext()) {
			String key = i.next();
			String value = attributes.get(key);
			s += "\t " + key + ": " + value + "\n";
		}
		return s;
	}
}
