package fr.telecomParistech.dash.mpd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SegmentList implements Serializable {
	private static final long serialVersionUID = 1572394965635579178L;
	private InitSegment initSegment;
	private List<MediaSegment> mediaSegments;
	private Map<String, String> attributes;
	
	public SegmentList(InitSegment initSegment) {
		this.initSegment = initSegment;
		mediaSegments = new LinkedList<MediaSegment>();
		attributes = new HashMap<String, String>();
	}

	public SegmentList() {
		this(null);
	}

	public InitSegment getInitSegment() {
		return initSegment;
	}

	public void setInitSegment(InitSegment initSegment) {
		this.initSegment = initSegment;
	}

	public void addMediaSegment(MediaSegment mediaSegment) {
		mediaSegments.add(mediaSegment);
	}

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
	
	public List<MediaSegment> getAllMediaSegment() {
		return mediaSegments;
	}

	public void removeMediaSegment(MediaSegment mediaSegment) {
		if (mediaSegments == null) {
			return;
		} else {
			mediaSegments.remove(mediaSegment);
		}
	}

	public void addAttribute(String name, String value) {
		attributes.put(name, value);
	}

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
