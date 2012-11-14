package fr.telecomParistech.dash.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.ChunkOffset64BitBox;
import com.coremedia.iso.boxes.ChunkOffsetBox;
import com.coremedia.iso.boxes.MediaBox;
import com.coremedia.iso.boxes.MediaInformationBox;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.SampleSizeBox;
import com.coremedia.iso.boxes.SampleTableBox;
import com.coremedia.iso.boxes.SampleToChunkBox;
import com.coremedia.iso.boxes.SampleToChunkBox.Entry;
import com.coremedia.iso.boxes.StaticChunkOffsetBox;
import com.coremedia.iso.boxes.SyncSampleBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;

public class MP4Parser {
	private static final Logger log = Logger.getLogger(MP4Parser.class.getName());
	
	public void parse(String filePath) {
		FileInputStream fis = null;
		IsoFile isoFile = null;
	
		try {
			fis = new FileInputStream(filePath);
			FileChannel fc = fis.getChannel();
			
			isoFile = new IsoFile(fc);
			MovieBox movieBox = isoFile.getMovieBox();
			log.info(format(movieBox.toString()));
			log.info("-------------------------------------------------------");
			
			TrackBox trackBox = getVideoTrackBox(movieBox);			
			MediaBox mediaBox = trackBox.getMediaBox();
			MediaInformationBox mediaInformationBox = mediaBox.getMediaInformationBox();
			SampleTableBox sampleTableBox = mediaInformationBox.getSampleTableBox();
			SyncSampleBox syncSampleBox = sampleTableBox.getSyncSampleBox();
			
			long[] sampleNumber = syncSampleBox.getSampleNumber();
			for (int i = 0; i < sampleNumber.length; i++) {
				System.out.print(sampleNumber[i] + "   ");
			}
			
			// stsc
			SampleToChunkBox sampleToChunkBox = sampleTableBox.getSampleToChunkBox();
			List<Entry> chunkList = sampleToChunkBox.getEntries();
			log.info("ChunkList size: " + chunkList.size());
//			for (int i = 0; i < chunkList.size(); i++) {
//				Entry chunk = chunkList.get(i);
//				log.info("Sample Description Index: " + chunk.getSampleDescriptionIndex());
//				log.info("First Chunk: " + chunk.getFirstChunk());
//				log.info("Sample Per Chunk: " + chunk.getSamplesPerChunk());
//				log.info("-------------------------------------------");
//			}
			
			SampleSizeBox sampleSizeBox = sampleTableBox.getSampleSizeBox();
			log.info("Sample Count: " + sampleSizeBox.getSampleCount());
			
			ChunkOffsetBox chunkOffsetBox = sampleTableBox.getChunkOffsetBox();
			if (chunkOffsetBox instanceof StaticChunkOffsetBox) {
				StaticChunkOffsetBox staticChunkOffsetBox = (StaticChunkOffsetBox) chunkOffsetBox;
				long[] chunkOffsets = staticChunkOffsetBox.getChunkOffsets();
//				for (int i = 0; i < chunkOffsets.length; i++) {
//					log.info("chunk #" + i +", offset: " + chunkOffsets[i]);
//				}
			} 
			log.info(chunkOffsetBox.toString());
			isoFile.close();
			fis.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * Get VideoTrackBox form movieBox
	 * @param movieBox movieBox to get the track box
	 * @return
	 */
	private TrackBox getVideoTrackBox(MovieBox movieBox) {
		List<TrackBox> trackBoxList = movieBox.getBoxes(TrackBox.class);
		for (TrackBox trackBox : trackBoxList) {
			TrackHeaderBox trackHeaderBox = trackBox.getTrackHeaderBox();
			// Audio Tracks don't have width and weight
			if ((trackHeaderBox.getWidth() > 0) && (trackHeaderBox.getHeight() > 0)) {
				return trackBox;
			}
		}
		return null;
	}
	
	
	private byte[] getSampleData(long sampleIndex, SampleTableBox sampleTableBox, String filePath) {
		ChunkData chunkData = getChunkDataOf(sampleIndex, sampleTableBox);
		long offset = chunkData.getOffset();
		long currentSample = chunkData.getFirtSample();
		
		while (currentSample < sampleIndex) {
			offset += getSampleSize(currentSample, sampleTableBox);
			currentSample ++;
		}
		
		long size = getSampleSize(sampleIndex, sampleTableBox);
		byte[] mediaFile;
		try {
			mediaFile = FileUtils.readFileToByteArray(new File(filePath));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		byte[] image = new byte[(int) size];
		System.arraycopy(mediaFile, (int)offset , image, 0, (int) size);
		
		return image;
		
	}
	
	private long getSampleSize(long sampleIndex, SampleTableBox sampleTableBox) {
		long sampleQty = getSampleQuantity(sampleTableBox);
		if ((sampleIndex > sampleQty) || (sampleIndex < 0)) {
			return -1;
		}
		
		// Convert 1-based to 0-based:
		sampleIndex--;
		
		SampleSizeBox sampleSizeBox = sampleTableBox.getSampleSizeBox();
		return sampleSizeBox.getSampleSizeAtIndex((int)sampleIndex);
	}
	
	private ChunkData getChunkDataOf(long sampleIndex, SampleTableBox sampleTableBox) {
		long sampleQty = getSampleQuantity(sampleTableBox);
		if ((sampleIndex > sampleQty) || (sampleIndex < 0)) {
			return null;
		}
		
		// Get list of chunk group
		SampleToChunkBox sampleToChunkBox = sampleTableBox.getSampleToChunkBox();
		List<Entry> chunkGroupList = sampleToChunkBox.getEntries();
		
		// first and last sample in each chunk
		int currentChunk = 1; // 1-based system
		long firstSampleIndex = 1; // 1-based system
		
		ChunkData chunkData = new ChunkData();
		for (int i = 0; i < chunkGroupList.size() - 1; i++) {
			Entry chunkGroup = chunkGroupList.get(i);
			Entry nextChunkGroup = chunkGroupList.get(i+1);
			
			long chunkPerGroup = 
					nextChunkGroup.getFirstChunk() - chunkGroup.getFirstChunk();
			for (int j = 0; j < chunkPerGroup; j++) {
				long samplePerChunk = chunkGroup.getSamplesPerChunk();
				
				// firstIndex <= currentIndex < lastIndex 
				// We find out the sample
				if ((firstSampleIndex <= sampleIndex) && 
						(sampleIndex < (firstSampleIndex + samplePerChunk))) {
					chunkData.setIndex(currentChunk);
					chunkData.setFirtSample(firstSampleIndex);
					
					ChunkOffsetBox chunkOffsetBox = sampleTableBox.getChunkOffsetBox();
					// currentChunk - 1 : convert from 1-based to 0-based
					long chunkOffset = (chunkOffsetBox.getChunkOffsets())[currentChunk - 1]; 
					chunkData.setOffset(chunkOffset);
					
					return chunkData;
				}
				
				currentChunk ++;
				firstSampleIndex += samplePerChunk;
			}
		}
		
		// else, the it belongs to the last trunk
		chunkData.setIndex(currentChunk);
		chunkData.setFirtSample(firstSampleIndex);
		
		ChunkOffsetBox chunkOffsetBox = sampleTableBox.getChunkOffsetBox();
		long chunkOffset = (chunkOffsetBox.getChunkOffsets())[currentChunk - 1];
		chunkData.setOffset(chunkOffset);
		
		return chunkData;
	}
	
	private long getChunkQuantity(SampleTableBox sampleTableBox) {
		ChunkOffsetBox chunkOffsetBox = sampleTableBox.getChunkOffsetBox();
		long chunkQty = 0;
		if (chunkOffsetBox instanceof StaticChunkOffsetBox) {
			chunkQty = ((StaticChunkOffsetBox) chunkOffsetBox)
					.getChunkOffsets()
					.length;
		} else { // ChunkOffset64BitBox
			chunkQty = ((ChunkOffset64BitBox) chunkOffsetBox)
					.getChunkOffsets()
					.length;
		}
		return chunkQty;
	}
	
	private long getSampleQuantity(SampleTableBox sampleTableBox) {		
		SampleSizeBox sampleSizeBox = sampleTableBox.getSampleSizeBox();
		return sampleSizeBox.getSampleCount();
	}
	
	
	private String createSpace(int indent) {
		String output = "";
		for (int i = 0; i < indent; i++) {
			output += " ";
		}
		return output;
	}
	
	/**
	 * Format String, used for debug.
	 * @param input string to format
	 * @return
	 */
	private String format(String input) {
		if ("" == input || null == input) 
			return "";

		int indent = 0;
		char[] charList = input.toCharArray();
		String output = "";
		for (int i = 0; i < charList.length; i++) {
			if (i == charList.length - 1) {
				output += charList[i] + "\n";
				return output;
			}
			
			char c = charList[i];
			
			if (c == '[') {
				indent += 2;
				output += c + "\n";
				output += createSpace(indent);
			} else if ( c == ']' ) {
				indent -= 2;
				output += c;
			} else if (c == ';') {
				output += c + "\n";
				output += createSpace(indent);
			} else {
				output += c;
			}
		}
		return output;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MP4Parser mp4Parser = new MP4Parser();
		mp4Parser.parse("resources/sample_iPod.m4v");
		log.info("done");
	}
}
