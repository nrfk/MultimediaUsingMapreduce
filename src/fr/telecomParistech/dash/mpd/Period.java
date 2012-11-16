package fr.telecomParistech.dash.mpd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class represents a period in a mdp's period list
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class Period implements Serializable{
	private static final long serialVersionUID = 7892900291066412819L;
	private List<AdaptationSet> adaptationSets;
	private Map<String, String> attributes;
	private int id;	
	
	/**
	 * Create a new period with an specific id
	 * @param id id of the period to create
	 */
	public Period(int id) {
		adaptationSets = new LinkedList<AdaptationSet>();
		attributes = new HashMap<String, String>();
		this.id = id;
	}

	/**
	 * Add an adaptation set to this period.
	 * @param adaptationSet the adaptationSet to be added.
	 */
	public void addAdaptationSet(AdaptationSet adaptationSet) {
		adaptationSets.add(adaptationSet);
	}
	
	/**
	 * Remove an adaptation set from this period.
	 * @param adaptationSet the adaptaionSet to be removed.
	 */
	public void removeAdaptationSet(AdaptationSet adaptationSet) {
		adaptationSets.remove(adaptationSet);
	}
	
	/**
	 * Get a specific adaptation set from it id (if any).
	 * @param id the adaptation set.
	 * @return
	 */
	public AdaptationSet getAdaptationSet(int id) {
		if (adaptationSets == null || adaptationSets.size() == 0) 
			return null;
		
		for (AdaptationSet a : adaptationSets) {
			if (a.getId() == id) return a; 
		}
		
		return null;
	}
	
	/**
	 * Get all adaptation sets of this period.
	 * @return a list containing all adaptation sets.
	 */
	public List<AdaptationSet> getAllAdaptationSet() {
		return adaptationSets;
	}
	
	/**
	 * Get id of this period.
	 * @return the period id.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Set id for this period.
	 * @param id the id for this period.
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * add an attribute to this period
	 * @param name attribute name
	 * @param value attribute value
	 */
	public void addAttribute(String name, String value) {
		attributes.put(name, value);
	}
	
	/**
	 * Get an attribute from this period
	 * @param name the attribute name
	 * @return attribute value (if any) or null.
	 */
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
