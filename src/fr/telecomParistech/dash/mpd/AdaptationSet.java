package fr.telecomParistech.dash.mpd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdaptationSet implements Serializable {
	private static final long serialVersionUID = -3232244540886035116L;
	public static final String AUDIO = "audio";
	public static final String VIDEO = "video";
	public static final String UNKNOWN_TYPE = "unknownType";
	public static final int UNKNOWN_ID = -1;
	
	private Map<String, String> attributes;
	private int id;
	private List<Representation> representations;
	
	public AdaptationSet(int id) {
		attributes = new HashMap<String, String>();
		this.id = id;
		representations = new LinkedList<Representation>();
	}
	
	public void addRepresentation(Representation representation) {
		representations.add(representation);
	}

	public void removeRepresentation(Representation representation) {
		representations.remove(representation);
	}
	
	public List<Representation> getAllRepresentation() {
		return representations;
	}
	
	public Representation getRepresentation(int id) {
		if (representations == null || representations.size() == 0) 
			return null;
		
		for (Representation r : representations) {
			if (r.getId() == id) return r; 
		}
		
		return null;
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
		s += "AdaptationSet\n";
		s += "\t id: " + getId() + "\n";
		
		Set<String> keys = attributes.keySet();
		Iterator<String> i = keys.iterator();
		while (i.hasNext()) {
			String key = i.next();
			String value = attributes.get(key);
			s += "\t " + key + ": " + value + "\n";
		}
		
		for (Representation r : representations) {
			s += r.toString();
		}
		
		return s;
	}
	
}
