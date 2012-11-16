package fr.telecomParistech.dash.mpd;

import java.io.Serializable;

/**
 * A MediaSegment in MPD file
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class MediaSegment extends Segment implements Serializable{
	private static final long serialVersionUID = -6090940567448819805L;
	private String media;
	
	/**
	 * Create new MediaSegment.
	 * @param id id of the MediaSegment.
	 * @param media media (url) of this MediaSegment.
	 */
	public MediaSegment(int id, String media) {
		super(id);
		this.media = media;
	}
	
	/**
	 * Create new MediaSegment.
	 */
	public MediaSegment() {
		this(-1,"");
	}
	
	/**
	 * Get the media (url)
	 * @return media
	 */
	public String getMedia() {
		return media;
	}
	
	/**
	 * Set media for this MediaSegment
	 * @param media
	 */
	public void setMedia(String media) {
		this.media = media;
	}
	
public String toString() {
		
		String s = "";
		s += "MediaSegment:\n";
		s += super.toString();
		s += "\t media: " + getMedia() + "\n";
		
		return s;
	}
}
