package fr.telecomParistech.dash.mpd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class describes Representation tag in mpd file 
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class Representation implements Serializable {
	private static final long serialVersionUID = -8188633044760053360L;
	private int id;
	private SegmentList segmentList;
	private Map<String, String> attributes;

	/**
	 * Create a new Representation object
	 * @param id id of this object
	 * @param segmentList segment list that this object contains
	 */
	public Representation(int id, SegmentList segmentList) {
		this.id = id;
		attributes = new HashMap<String, String>();
		this.segmentList = segmentList;
	}

	/**
	 * Get id of this representation
	 * @return the current id
	 */
	public int getId() {
		return id;
	}

	/**
	 * add new attribute to Representation object
	 * @param name name of the attribute
	 * @param value value of the attribute
	 */
	public void addAttribute(String name, String value) {
		attributes.put(name, value);
	}

	/**
	 * Get an attribute value from the Representation object
	 * @param name name of the attribute
	 * @return value of the attribute (if any) or null.
	 */
	public String getAttribute(String name) {
		return attributes.get(name);
	}

	/**
	 * Get segment list (segment info) of this representation.
	 * @return segment list.
	 */
	public SegmentList getSegmentList() {
		return segmentList;
	}

	/**
	 * Set a new segment list for this Representation.
	 * @param segmentList the new Segment list to set.
	 */
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
