package fr.telecomParistech.dash.mpd;

import java.io.Serializable;

/**
 * A InitSegment in MPD file
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class InitSegment extends Segment implements Serializable{
	private static final long serialVersionUID = -4308417887537332941L;
	private String sourceURL;
	
	/**
	 * Create new InitSegment.
	 * @param sourceURL sourceUrl attribute of this segment.
	 */
	public InitSegment(String sourceURL) {
		super(0);
		this.sourceURL = sourceURL;
	}
	
	public InitSegment() {
		this("");
	}
	
	/**
	 * Get sourceUrl of this segment.
	 * @return the sourceUrl.
	 */
	public String getSourceURL() {
		return sourceURL;
	}
	
	/**
	 * Set sourceUrl for this segment
	 * @param sourceURL sourceUrl to set.
	 */
	public void setSourceURL(String sourceURL) {
		this.sourceURL = sourceURL;
	}
	
	public String toString() {
		String s = "";
		s += "InitSegment:\n";
		s += super.toString();
		s += "\t sourceURL: " + getSourceURL() + "\n";
		return s;
	}
}
