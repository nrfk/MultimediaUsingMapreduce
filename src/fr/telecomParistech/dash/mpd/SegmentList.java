package fr.telecomParistech.dash.mpd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SegmentList (also known as segment info) contains all Segment info of a
 * specific representation.
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class SegmentList implements Serializable {
	private static final long serialVersionUID = 1572394965635579178L;
	private InitSegment initSegment;
	private List<MediaSegment> mediaSegments;
	private Map<String, String> attributes;
	
	/**
	 * Create a new SegmentList object 
	 * @param initSegment init segment in this SegmentList object
	 */
	public SegmentList(InitSegment initSegment) {
		this.initSegment = initSegment;
		mediaSegments = new LinkedList<MediaSegment>();
		attributes = new HashMap<String, String>();
	}

	/**
	 * Create a new segment list object
	 */
	public SegmentList() {
		this(null);
	}

	/**
	 * Get InitSegment of this SegmentList object
	 * @return the initSegment
	 */
	public InitSegment getInitSegment() {
		return initSegment;
	}

	/**
	 * Set Init Segment for this SegmentList object.
	 * @param initSegment the initSegment to set. 
	 */
	public void setInitSegment(InitSegment initSegment) {
		this.initSegment = initSegment;
	}

	/**
	 * Add a new MediaSegment to the segment list object.
	 * @param mediaSegment the mediaSegment to add.
	 */
	public void addMediaSegment(MediaSegment mediaSegment) {
		mediaSegments.add(mediaSegment);
	}

	/**
	 * Get a media segment from its id.
	 * @param id id of the media segment.
	 * @return mediaSegment indicated by the id (if any) or null.
	 */
	public MediaSegment getMediaSegment(int id) {
		// Not a media segment
		if (id <= 0)
			return null;

		// List empty
		if (mediaSegments == null || mediaSegments.size() == 0)
			return null;

		for (MediaSegment m : mediaSegments) {
			if (m.getId() == id)
				return m; // found!!! return...
		}

		// Not found
		return null;
	}
	
	/**
	 * Get all media segments of the list.
	 * @return all the media segment.
	 */
	public List<MediaSegment> getAllMediaSegment() {
		return mediaSegments;
	}

	/**
	 * Remove an media segment from the list
	 * @param mediaSegment the media segment to remove
	 */
	public void removeMediaSegment(MediaSegment mediaSegment) {
		if (mediaSegments == null) {
			return;
		} else {
			mediaSegments.remove(mediaSegment);
		}
	}

	/**
	 * Add a new attribute to this segment list.
	 * @param name name of the attribute.
	 * @param value value of the attribute.
	 */
	public void addAttribute(String name, String value) {
		attributes.put(name, value);
	}

	/**
	 * Get an attribute from this segment list.
	 * @param name name of the attribute.
	 * @return value of the attribute (if any) or null.
	 */
	public String getAttribute(String name) {
		return attributes.get(name);
	}

	public String toString() {

		String s = "";
		s += "SegmentList:\n";
		s += initSegment.toString();

		Set<String> keys = attributes.keySet();
		Iterator<String> i = keys.iterator();
		while (i.hasNext()) {
			String key = i.next();
			String value = attributes.get(key);
			s += "\t " + key + ": " + value + "\n";
		}

		for (MediaSegment m : mediaSegments) {
			s += m.toString();
		}

		return s;
	}

}
