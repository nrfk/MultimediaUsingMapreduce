package fr.telecomParistech.mp4parser;

/**
 * Chunk Data, used to keep the chunk information returned by 
 * parsing mp4 file
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class ChunkInfo {
	private long index;
	private long offset;
	private long firstSample;
	
	/**
	 * Default constructor
	 */
	public ChunkInfo() {}
	
	/**
	 * Get Index of this chunk
	 * @return index of the chunk
	 */
	public long getIndex() {
		return index;
	}
	
	/**
	 * Set index for this chunk
	 * @param index index to set
	 */
	public void setIndex(long index) {
		this.index = index;
	}
	
	/**
	 * Get offset (from the start of file) of this chunk
	 * @return offset
	 */
	public long getOffset() {
		return offset;
	}
	
	/**
	 * Set offset for this chunk
	 * @param offset offset to set
	 */
	public void setOffset(long offset) {
		this.offset = offset;
	}
	
	/**
	 * Get first sample in the chunk
	 * @return the first sample
	 */
	public long getFirtSample() {
		return firstSample;
	}
	public void setFirtSample(long firtSample) {
		this.firstSample = firtSample;
	}

	public String toString() {
		String str = "";
		str += "[" + ChunkInfo.class.getName() + "]";
		str += "   index: " + index;
		str += "   offset: " + offset;
		str += "  firstSample: " + firstSample;
		return str;
	}
}
