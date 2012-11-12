package fr.telecomParistech.dash.mpd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Representation implements Serializable {
	private static final long serialVersionUID = -8188633044760053360L;
	private int id;
	private SegmentList segmentList;
	private Map<String, String> attributes;

	public Representation(int id, SegmentList segmentList) {
		this.id = id;
		attributes = new HashMap<String, String>();
		this.segmentList = segmentList;
	}

	public int getId() {
		return id;
	}

	public void addAttribute(String name, String value) {
		attributes.put(name, value);
	}

	public String getAttribute(String name) {
		return attributes.get(name);
	}

	public SegmentList getSegmentList() {
		return segmentList;
	}

	public void setSegmentList(SegmentList segmentList) {
		this.segmentList = segmentList;
	}

	public String toString() {

		String s = "";
		s += "Representation: \n";
		s += "\t id: " + getId() + "\n";

		Set<String> keys = attributes.keySet();
		Iterator<String> i = keys.iterator();
		while (i.hasNext()) {
			String key = i.next();
			String value = attributes.get(key);
			s += "\t " + key + ": " + value + "\n";
		}

		s += segmentList.toString();

		return s;
	}
}
