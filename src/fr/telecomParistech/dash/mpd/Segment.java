package fr.telecomParistech.dash.mpd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A segment in the mpd file. There're two type of segment, InitSegment and 
 * MediaSegment
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public abstract class Segment implements Serializable {
	private static final long serialVersionUID = -3211862724639537468L;
	public static final Logger LOGGER = Logger.getLogger(Segment.class.getName());
	public static final String UNKNOWN_URL = "";
	// id of the segment
	private int id; 
	private Map<String, String> attributes;
	
	/**
	 * Create new segment with a specific id
	 * @param id id of the segment
	 */
	public Segment(int id) {
		this.id = id;
		attributes = new HashMap<String, String>();
	}
	
	/**
	 * Add new attribute to the segment.
	 * @param name name of the attribute.
	 * @param value value of the attribute.
	 */
	public void addAttribute(String name, String value) {
		attributes.put(name, value);
	}

	/**
	 * Get an attribute from its name
	 * @param name name of the attribute
	 * @return attribute value (if any) or null.
	 */
	public String getAttribute(String name) {
		return attributes.get(name);
	}
	
	/**
	 * Get segment id
	 * @return segment id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set segment id
	 * @param id segment id
	 */
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
