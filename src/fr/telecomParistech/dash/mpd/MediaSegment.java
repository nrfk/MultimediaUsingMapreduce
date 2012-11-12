package fr.telecomParistech.dash.mpd;

import java.io.Serializable;

public class MediaSegment extends Segment implements Serializable{
	private static final long serialVersionUID = -6090940567448819805L;
	private String media;
	public MediaSegment(int id, String media) {
		super(id);
		this.media = media;
	}
	
	public MediaSegment() {
		this(-1,"");
	}
	
	public String getMedia() {
		return media;
	}
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
