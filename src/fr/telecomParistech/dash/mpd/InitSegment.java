package fr.telecomParistech.dash.mpd;

import java.io.Serializable;

public class InitSegment extends Segment implements Serializable{
	private static final long serialVersionUID = -4308417887537332941L;
	// Placeholder, for future use.
	private String sourceURL;
	public InitSegment(String sourceURL) {
		super(0);
		this.sourceURL = sourceURL;
	}
	
	public InitSegment() {
		this("");
	}
	
	public String getSourceURL() {
		return sourceURL;
	}
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
