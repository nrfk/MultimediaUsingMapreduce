package fr.telecomParistech.dash.mpd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Media Presentation Description, this class  contains all the information read
 * from a .mpd file.
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class MPD implements Serializable {
	private static final long serialVersionUID = -3031163060671750940L;

	public static final int UNKNOWN_ID = -1;
	
	private Map<String, String> attributes;
	private List<Period> periods;
	
	/**
	 * Create new media presentation object
	 */
	public MPD() {
		attributes = new HashMap<String, String>();
		this.periods = new LinkedList<Period>();
	}
	
	/**
	 * Add a new period object to this mpd
	 * @param period the object to add
	 */
	public void addPeriod(Period period) {
		periods.add(period);
	}
	
	/**
	 * Remove a period object from this mpd
	 * @param period the object to remove
	 */
	public void removePeriod(Period period) {
		periods.remove(period);
	}
	
	/**
	 * Get a period from MPD file
	 * @param id a period's id to get
	 * @return the period whose id is specific in the id param
	 */
	public Period getPeriod(int id) {
		if (periods == null || periods.size() == 0) 
			return null;
		
		for (Period p : periods) {
			if (p.getId() == id) return p;
		}
		return null;
	}
	
	/**
	 * Get all period in this mpd file
	 * @return List of period
	 */
	public List<Period> getAllPeriod() {
		return periods;
	}
	
	/**
	 * Add attribute to mpd file
	 * @param name name of the attribute
	 * @param value value of the attribute
	 */
	public void addAttribute(String name, String value) {
		attributes.put(name, value);
	}
	
	/**
	 * Get an attribute
	 * @param name name of the attribute
	 * @return value of the attribute (if any) or null.
	 */
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
