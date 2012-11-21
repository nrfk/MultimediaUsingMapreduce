package fr.telecomParistech.mp4parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.ChunkOffsetBox;
import com.coremedia.iso.boxes.HandlerBox;
import com.coremedia.iso.boxes.MediaBox;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.SampleDescriptionBox;
import com.coremedia.iso.boxes.SampleSizeBox;
import com.coremedia.iso.boxes.SampleTableBox;
import com.coremedia.iso.boxes.SampleToChunkBox;
import com.coremedia.iso.boxes.SampleToChunkBox.Entry;
import com.coremedia.iso.boxes.SyncSampleBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.h264.AvcConfigurationBox;
import com.coremedia.iso.boxes.sampleentry.SampleEntry;

import fr.telecomParistech.image.bitmap.ConvertUtility;

/**
 * A class used to parse MP4 file.
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class MP4Parser {
	private static final Logger LOGGER = 
			Logger.getLogger(MP4Parser.class.getName());

	/**
	 * Get all ISamples of a specific video
	 * @param filePath filePath to the file
	 * @return List of all Isamples in the video file
	 */
	public List<Long> getISamples(String filePath) {
		// All I-Samples located in moov -> trak -> mdia -> minf -> stbl ->
		// stss. Not included stss, all of them are mandatory.
		IsoFile isoFile = getIsoFile(filePath);
		MovieBox movieBox = isoFile.getMovieBox();
		TrackBox trackBox = getVideoTrackBox(movieBox);
		SyncSampleBox syncSampleBox =  trackBox
				.getMediaBox()
				.getMediaInformationBox()
				.getSampleTableBox() 	//stbl
				.getSyncSampleBox();	//stss

		// As syncSampleBox is optional, if we don't find it out, we
		// return null
		if (syncSampleBox == null) {
			return null;
		}

		// Ok, get it.
		List<Long> iSamples = new ArrayList<Long>();
		for (long iSample : syncSampleBox.getSampleNumber()) {
			iSamples.add(iSample);
			System.out.print("   " + iSample);
		}
		return iSamples;
	}

	/**
	 * Get IsoFile from video's filePath
	 * @param filePath video's filePath
	 * @return isoFile
	 */
	public IsoFile getIsoFile(String filePath) {
		FileInputStream fileInputStream = null;
		IsoFile isoFile = null;
		try {
			fileInputStream = new FileInputStream(filePath);
			isoFile = new IsoFile(fileInputStream.getChannel());
		} catch (FileNotFoundException fe) {
			LOGGER.info("File not found: " + filePath);
			fe.printStackTrace();
		} catch (IOException ioe) {
			LOGGER.info("IO exception");
			ioe.printStackTrace();
		} finally {
			try {
				if (isoFile != null) {
					isoFile.close();
				}
				if (fileInputStream != null) {
					fileInputStream.close();
				}
			} catch (IOException e) {
				LOGGER.info("Error while closing stream");
				e.printStackTrace();
			}
		}

		return isoFile;
	}

	/**
	 * Get VideoTrackBox form movieBox
	 * @param movieBox movieBox to get the track box
	 * @return TrackBox
	 */
	private TrackBox getVideoTrackBox(MovieBox movieBox) {
		List<TrackBox> trackBoxList = movieBox.getBoxes(TrackBox.class);
		for (TrackBox trackBox : trackBoxList) {
			MediaBox mediaBox = trackBox.getMediaBox();
			HandlerBox handlerBox = mediaBox.getHandlerBox();
			String type = handlerBox.getHandlerType();
			if (type.equalsIgnoreCase("VIDE")) {
				return trackBox;
			}
		}
		return null;
	}

	/**
	 * Get sampleData of a sample checkBox
	 * @param sampleIndex
	 * @param sampleTableBox
	 * @param filePath
	 * @return ByteBuffer of sampleData (if any) or null
	 */
	public ByteBuffer getSampleData(long sampleIndex, String filePath) {
		// Get information of chunk which contains this sampleIndex
		ChunkInfo chunkInfo = getChunkDataOf(sampleIndex, filePath);
		// Offset of this chunk from the beginning of file
		long offset = chunkInfo.getOffset();
		
		// currentSample used to iterate from first sample in this chunk to 
		// the "sampleIndex"
		long currentSample = chunkInfo.getFirtSample();

		while (currentSample < sampleIndex) {
			offset += getSampleSize(currentSample, filePath);
			currentSample ++;
		}

		// Ok, find out the offset of sample index, get its size
		long size = getSampleSize(sampleIndex, filePath);
		
		// Now, copy all sample data from video data
		// Load video data
		byte[] videoData;
		try {
			videoData = FileUtils.readFileToByteArray(new File(filePath));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		// Read sample data from video data
		byte[] sampleData = new byte[(int) size];
		
		// If file is too large, throw an Runtime Exception, because casting 
		// will not work here. Note that it's a RUN TIME exception
		if (videoData.length > Integer.MAX_VALUE) {
			throw new FileTooLargeException(videoData.length);
		} else { // else, read sample data
			System.arraycopy(videoData, (int)offset , sampleData, 0, (int)size);
		}
		
		return ByteBuffer.wrap(sampleData);

	}

	/**
	 * Get PPS of the video in byte Array format
	 * @param filePath file path to video
	 * @return PPS byte array
	 */
	public ByteBuffer getPpsInByte(String filePath) {
		AvcConfigurationBox avcConfigurationBox = 
				getAvcConfigurationBox(filePath);
		if (avcConfigurationBox == null) {
			return null;
		}
		
		byte[] pps = avcConfigurationBox.getPictureParameterSets().get(0);
		if (pps == null) {
			return null;
		}
		
		return ByteBuffer.wrap(pps);
	}
	
	/**
	 * Get PPS of the video in Hex String format
	 * @param filePath file path to video
	 * @return Hex String format
	 */
	public String getPpsInHex(String filePath) {
		byte[] pps = getPpsInByte(filePath).array();
		return ConvertUtility.byteArrayToHext(pps);
	}
	
	
	/**
	 * Get SPS of the video in byte Array format
	 * @param filePath file path to video
	 * @return PPS byte array
	 */
	public ByteBuffer getSpsInByte(String filePath) {
		AvcConfigurationBox avcConfigurationBox = 
				getAvcConfigurationBox(filePath);
		if (avcConfigurationBox == null) {
			return null;
		}
		
		byte[] sps = avcConfigurationBox.getSequenceParameterSets().get(0);
		if (sps == null) {
			return null;
		}
		
		return ByteBuffer.wrap(sps);
	}
	
	public ByteBuffer createH264rawData(String filePath) {
		// Get sample quantity and nalUnitSize
		long sampleQuantity = getSampleQuantity(filePath);
		int nalLengthSize = getNalLengthSize(filePath);
		if (sampleQuantity == -1 || nalLengthSize == -1) {
			return null;
		}
		
		// Nal header
		byte[] nalHeader = {
				0x00,
				0x00,
				0x00, 
				0x01}; 
		
		byte[] buffer;
		ByteArrayOutputStream byteArrayOutputStream = null;
		try {
			byteArrayOutputStream = new ByteArrayOutputStream();
			
			// Write SPS 
			buffer = getSpsInByte(filePath).array();
			byteArrayOutputStream.write(nalHeader);
			byteArrayOutputStream.write(buffer);
			
			// Write PPS 
			buffer = getPpsInByte(filePath).array();
			byteArrayOutputStream.write(nalHeader);
			byteArrayOutputStream.write(buffer);
			
			// Write NAL
			// 1-based system
			for (long i = 1; i <= sampleQuantity; i++) {
				buffer = getSampleData(i, filePath).array();
				
				// NOte that each sample may have many NAL
				// Write NalRaw data of this sample 
				byte[] h264Raw = getH264DataOf(buffer, nalLengthSize);
				byteArrayOutputStream.write(h264Raw);
			}
			
			// get the output
			buffer = byteArrayOutputStream.toByteArray();
			return ByteBuffer.wrap(buffer);
			
		} catch (IOException ioe) {
			LOGGER.severe("IOException, file: " + filePath);
		} finally {
			try {
				if (byteArrayOutputStream != null) {
					byteArrayOutputStream.close();
				}
			} catch (Exception e) {
				LOGGER.severe("Error while closing stream.");
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	private byte[] getH264DataOf(byte[] sampleData, int nalLengthSize) 
			throws IOException{
		byte[] nalLengthInByte = new byte[nalLengthSize]; // nal length in byte
		int currentPos = 0; // current position
		// Nal header
		byte[] nalHeader = {
				0x00,
				0x00,
				0x00, 
				0x01}; 
		
		// work as a buffer, temporary mem.
		ByteArrayOutputStream byteArrayOutputStream = 
				new ByteArrayOutputStream();
		
		while (currentPos < sampleData.length) {
			// get nalLengthInByte 
			System.arraycopy(sampleData, currentPos, 
					nalLengthInByte, 0, nalLengthSize);
			// Get len in byte
			int nalLength = ConvertUtility.byteArrayToInteger(nalLengthInByte);
			// update index
			currentPos += nalLengthSize;
			
			// read data to nalData
			byte[] nalData = new byte[nalLength];
			System.arraycopy(sampleData, currentPos, 
					nalData, 0, nalLength);
			
			// ok, now write all of it to buffer
			byteArrayOutputStream.write(nalHeader); // header
			byteArrayOutputStream.write(nalData); // data
			
			// update index
			currentPos += nalLength;
		}
		return byteArrayOutputStream.toByteArray();
	}
	
	/**
	 * Get SPS of the video in Hex String format
	 * @param filePath file path to video
	 * @return Hex String format
	 */
	public String getSpsInHex(String filePath) {
		byte[] sps = getSpsInByte(filePath).array();
		return ConvertUtility.byteArrayToHext(sps);
	}

	/**
	 * Get NalUnitSize
	 * @param filePath path of the video file
	 * @return NalUnitSize or -1 if error occurs
	 */
	private int getNalLengthSize(String filePath) {
		AvcConfigurationBox avcConfigurationBox = 
				getAvcConfigurationBox(filePath);
		if (avcConfigurationBox == null) {
			return -1;
		}
		return (avcConfigurationBox.getLengthSizeMinusOne() + 1);
	}
	
	/**
	 * Get AvcConfigurationBox
	 * @param filePath filePath file path to video
	 * @return AvcConfigurationBox
	 */
	private AvcConfigurationBox getAvcConfigurationBox(String filePath) {
		// All I-Samples located in moov -> trak -> mdia -> minf -> stbl ->
		// stsd. All of them are mandatory.
		IsoFile isoFile = getIsoFile(filePath);
		if (isoFile == null) {
			return null;
		}

		// Get the stsd box
		MovieBox movieBox = isoFile.getMovieBox();
		TrackBox trackBox = getVideoTrackBox(movieBox);
		SampleDescriptionBox sampleDescriptionBox =  trackBox
				.getMediaBox()
				.getMediaInformationBox()
				.getSampleTableBox() 	//stbl
				.getSampleDescriptionBox();	//stsd
		SampleEntry sampleEntry = sampleDescriptionBox.getSampleEntry();
		List<Box> boxes = sampleEntry.getBoxes();
		
		// Find pps
		for (Box box : boxes) {
			if (box instanceof AvcConfigurationBox) {
				return (AvcConfigurationBox) box;
			}
		}
		
		// Not ok, return null
		return null;
	}
	
	/**
	 * Get sample size
	 * @param sampleIndex index of the sample in video file
	 * @param filePath file path of video file
	 * @return sample size or -1 if sampleIndex is invalid
	 */
	private long getSampleSize(long sampleIndex, String filePath){
		IsoFile isoFile = getIsoFile(filePath);
		
		// Unknown sample size
		if (isoFile == null) {
			return -1;
		}
		// Get video track box
		TrackBox trackBox = getVideoTrackBox(isoFile.getMovieBox());
		
		// SampleTableBox located in moov -> trak -> mdia -> minf -> stbl
		// All of them are mandatory.
		SampleTableBox sampleTableBox = trackBox
				.getMediaBox()
				.getMediaInformationBox()
				.getSampleTableBox();
		
		
		long sampleQty = getSampleQuantity(filePath);
		if ((sampleIndex > sampleQty) || (sampleIndex < 0)) {
			return -1;
		}

		// Convert 1-based to 0-based:
		sampleIndex--;

		SampleSizeBox sampleSizeBox = sampleTableBox.getSampleSizeBox();
		return sampleSizeBox.getSampleSizeAtIndex((int)sampleIndex);
	}

	/**
	 * Get chunkInfo of a specific sampleIndex
	 * @param sampleIndex sampleIndex to get chunkData
	 * @param sampleTableBox
	 * @return chunkInfo, if not found, return null;
	 */
	private ChunkInfo getChunkDataOf(long sampleIndex, String filePath) {
		IsoFile isoFile = getIsoFile(filePath);
		
		if (isoFile == null) {
			return null;
		}
		// Get video track box
		TrackBox trackBox = getVideoTrackBox(isoFile.getMovieBox());
		
		// SampleTableBox located in moov -> trak -> mdia -> minf -> stbl
		// All of them are mandatory.
		SampleTableBox sampleTableBox = trackBox
				.getMediaBox()
				.getMediaInformationBox()
				.getSampleTableBox();
		
		// Get number of sample in this box
		long sampleQty = getSampleQuantity(filePath);
		if ((sampleIndex > sampleQty) || (sampleIndex < 0)) {
			return null;
		}

		// Get list of chunk group
		SampleToChunkBox sampleToChunkBox = 
				sampleTableBox.getSampleToChunkBox();
		List<Entry> chunkGroupList = sampleToChunkBox.getEntries();

		// first and last sample in each chunk
		int currentChunk = 1; // 1-based system
		long firstSampleIndex = 1; // 1-based system

		ChunkInfo chunkData = new ChunkInfo();
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

					ChunkOffsetBox chunkOffsetBox = 
							sampleTableBox.getChunkOffsetBox();
					// currentChunk - 1 : convert from 1-based to 0-based
					long chunkOffset = 
							(chunkOffsetBox.getChunkOffsets())[currentChunk -1]; 
					chunkData.setOffset(chunkOffset);

					return chunkData;
				}
				currentChunk ++;
				firstSampleIndex += samplePerChunk;
			}
		}

		// else, it belongs to the last trunk
		chunkData.setIndex(currentChunk);
		chunkData.setFirtSample(firstSampleIndex);

		ChunkOffsetBox chunkOffsetBox = sampleTableBox.getChunkOffsetBox();
		long chunkOffset = (chunkOffsetBox.getChunkOffsets())[currentChunk - 1];
		chunkData.setOffset(chunkOffset);

		return chunkData;
	}

	/**
	 *  Get Sample Qauntity in this video file
	 * @param filePath filePath of the video
	 * @return sample quantity or -1 if an error occured.
	 */
	public long getSampleQuantity(String filePath) {		
		IsoFile isoFile = getIsoFile(filePath);
		if (isoFile == null) {
			return -1;
		}

		// Get the stsd box
		// moov -> trak -> mdia -> minf -> stbl
		MovieBox movieBox = isoFile.getMovieBox();
		TrackBox trackBox = getVideoTrackBox(movieBox);
		SampleTableBox sampleTableBox =  trackBox
				.getMediaBox()
				.getMediaInformationBox()
				.getSampleTableBox(); 	//stbl

		// this box is opitional
		SampleSizeBox sampleSizeBox = sampleTableBox.getSampleSizeBox();
		if (sampleSizeBox == null) {
			return -1;
		}
		
		return sampleSizeBox.getSampleCount();
	}

	/**
	 * Align String, used by format(String input) function
	 * @param indent number of indents to add.
	 * @return formated String.
	 */
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

	public static void main(String[] args) throws IOException {
		String filePath = "war/resources/sample_iPod.m4v";
		MP4Parser mp4Parser = new MP4Parser();
		//		mp4Parser.parse(filePath);
		byte[] sampleData = mp4Parser.getSampleData(1, filePath).array();
//		sampleData = mp4Parser.getPpsInByte(filePath).array();
		String hex = ConvertUtility.byteArrayToHext(sampleData);
		System.out.println(hex);
		System.out.println("*************************");
		sampleData = mp4Parser.getSampleData(2, filePath).array();
		hex = ConvertUtility.byteArrayToHext(sampleData);
		System.out.println(hex);
		
		byte[] h264 = mp4Parser.createH264rawData(filePath).array();
		System.out.println(ConvertUtility.byteArrayToHext(h264));
		
		
		long sampleQty = mp4Parser.getSampleQuantity(filePath);
		if (sampleQty == -1) {
			LOGGER.severe("Cannot get sample quantity, file: " + filePath);
			System.exit(1);
		}
		LOGGER.info("done");
	}
}
