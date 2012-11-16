package fr.telecomParistech.dash.mpd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AdaptationSet in MPD file.
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class AdaptationSet implements Serializable {
	private static final long serialVersionUID = -3232244540886035116L;
	public static final String AUDIO = "audio";
	public static final String VIDEO = "video";
	public static final String UNKNOWN_TYPE = "unknownType";
	public static final int UNKNOWN_ID = -1;
	
	private Map<String, String> attributes;
	private int id;
	private List<Representation> representations;
	
	/**
	 * Create new AdaptationSet with a specific id.
	 * @param id id to create.
	 */
	public AdaptationSet(int id) {
		attributes = new HashMap<String, String>();
		this.id = id;
		representations = new LinkedList<Representation>();
	}
	
	/**
	 * Add new Representation to this AdaptationSet.
	 * @param representation representation to add.
	 */
	public void addRepresentation(Representation representation) {
		representations.add(representation);
	}

	/**
	 * Remove a representation from this AdaptationSet
	 * @param representation
	 */
	public void removeRepresentation(Representation representation) {
		representations.remove(representation);
	}
	
	/**
	 * Get all representations in this AdaptationSet
	 * @return list of representations.
	 */
	public List<Representation> getAllRepresentation() {
		return representations;
	}

	/**
	 * Get a specific representation from this AdaptationSet.
	 * @param id id of the representation.
	 * @return
	 */
	public Representation getRepresentation(int id) {
		if (representations == null || representations.size() == 0) 
			return null;
		
		for (Representation r : representations) {
			if (r.getId() == id) return r; 
		}
		
		return null;
	}
	
	/**
	 * Get id of this AdaptationSet
	 * @return id
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set id for this AdaptationSet
	 * @param id to set.
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Add new Attribute to this AdaptationSet
	 * @param name name of the attribute.
	 * @param value value of the attribute.
	 */
	public void addAttribute(String name, String value) {
		attributes.put(name, value);
	}
	
	/**
	 * Get a specific attribute from this attribute.
	 * @param name name of the attribute.
	 * @return mediaSegment indicated by the id (if any) or null.
	 */
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
