package fr.telecomParistech.dash.util;

public class ChunkData {
	private long index;
	private long offset;
	private long firstSample;
	
	public long getIndex() {
		return index;
	}
	public void setIndex(long index) {
		this.index = index;
	}
	public long getOffset() {
		return offset;
	}
	public void setOffset(long offset) {
		this.offset = offset;
	}
	public long getFirtSample() {
		return firstSample;
	}
	public void setFirtSample(long firtSample) {
		this.firstSample = firtSample;
	}

	public String toString() {
		String str = "";
		str += "[" + ChunkData.class.getName() + "]";
		str += "   index: " + index;
		str += "   offset: " + offset;
		str += "  firstSample: " + firstSample;
		return str;
	}
}
