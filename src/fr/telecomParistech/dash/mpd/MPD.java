package fr.telecomParistech.dash.mpd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MPD implements Serializable {
	private static final long serialVersionUID = -3031163060671750940L;

	public static final int UNKNOWN_ID = -1;
	
	private Map<String, String> attributes;
	private List<Period> periods;
	
	public MPD() {
		attributes = new HashMap<String, String>();
		this.periods = new LinkedList<Period>();
	}
	
	public void addPeriod(Period period) {
		periods.add(period);
	}
	
	public void removePeriod(Period period) {
		periods.remove(period);
	}
	
	public Period getPeriod(int id) {
		if (periods == null || periods.size() == 0) 
			return null;
		
		for (Period p : periods) {
			if (p.getId() == id) return p;
		}
		return null;
	}
	
	public List<Period> getAllPeriod() {
		return periods;
	}
	
	public void addAttribute(String name, String value) {
		attributes.put(name, value);
	}
	
	public String getAttribute(String name) {
		return attributes.get(name);
	}
	
	public String toString() {
		
		String s = "";
		s += "Media Presentation Description: \n";
		
		Set<String> keys = attributes.keySet();
		Iterator<String> i = keys.iterator();
		while (i.hasNext()) {
			String key = i.next();
			String value = attributes.get(key);
			s += "\t " + key + ": " + value + "\n";
		}
		
		for (Period p : periods) {
			s += p.toString();
		}
		
		
		
		return s;
	}
}
