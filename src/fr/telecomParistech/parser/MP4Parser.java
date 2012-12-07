package fr.telecomParistech.parser;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import com.coremedia.iso.boxes.fragment.MovieFragmentBox;
import com.coremedia.iso.boxes.fragment.SegmentTypeBox;
import com.coremedia.iso.boxes.fragment.TrackFragmentBox;
import com.coremedia.iso.boxes.fragment.TrackRunBox;
import com.coremedia.iso.boxes.h264.AvcConfigurationBox;
import com.coremedia.iso.boxes.sampleentry.SampleEntry;
import com.googlecode.mp4parser.boxes.threegpp26244.SegmentIndexBox;

import fr.telecomParistech.image.bitmap.ConvertUtility;

/**
 * A class used to parse MP4 file.
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class MP4Parser {
	private static final Logger log = 
			Logger.getLogger(MP4Parser.class.getName());
	
	public byte[] createFirstImageFromDashSegment(
			byte[] nalHeader, 
			byte[] sps, 
			byte[] pps, 
			byte[] segmentData, 
			int nalLengthSize, int videoTrackId) {
		// We just want to get the first image. This function is dedicated to 
		// the image extractor mapreduce
		boolean justFirstImage = true;
		return createH264rawDataFromDashSegment(
				nalHeader, 
				sps, 
				pps, 
				segmentData, 
				nalLengthSize, 
				videoTrackId, 
				justFirstImage);
	}
	
	public byte[] createH264rawDataFromDashSegment(
			byte[] nalHeader, 
			byte[] sps, 
			byte[] pps, 
			byte[] segmentData, 
			int nalLengthSize, int videoTrackId) {
		
		// We won't to get just the first image, so we get all
		boolean justFirstImage = false;
		return createH264rawDataFromDashSegment(
				nalHeader, 
				sps, 
				pps, 
				segmentData, 
				nalLengthSize, 
				videoTrackId, 
				justFirstImage);
	}
	
	/**
	 * Create H264 raw data From Dash Segment
	 * @param nalHeader
	 * @param sps
	 * @param pps
	 * @param segmentData
	 * @param nalLengthSize
	 * @param videoTrackId
	 * @return a byte array contains h264 raw data
	 */
	private byte[] createH264rawDataFromDashSegment(
			byte[] nalHeader, 
			byte[] sps, 
			byte[] pps, 
			byte[] segmentData, 
			int nalLengthSize, int videoTrackId, boolean justFirstImage) {
		
		// Current offset to the beginning of file
		int offset = 0;
		
		if (segmentData == null) {
			log.info("SegmentData is null, cannot create h264 file");
			return null;
		}
		
		IsoFile isoFile = getIsoFile(segmentData);
		if (isoFile == null) {
			log.info("Cannot create iso file");
			return null;
		}
		
		// Update offset, SegmentTypeBox
		SegmentTypeBox segmentTypeBox = 
				isoFile.getBoxes(SegmentTypeBox.class).get(0);
		if (segmentTypeBox != null) {
			offset += segmentTypeBox.getSize();
		}

		// Update offset, SegmentTypeBox
		SegmentIndexBox segmentIndexBox = 
				isoFile.getBoxes(SegmentIndexBox.class).get(0);
		offset += segmentIndexBox.getSize();

		List<SegmentIndexBox.Entry> entries = 
				segmentIndexBox.getEntries();
		List<MovieFragmentBox> movieFragmentBoxes =
				isoFile.getBoxes(MovieFragmentBox.class);


		// Current MovieFramentBox
		int current = 0;
		ByteArrayOutputStream byteArrayOutputStream = null;

		try {
			byteArrayOutputStream =  new ByteArrayOutputStream();

			// If file is too large, throw an Runtime Exception, because casting 
			// will not work here. Note that it's a RUN TIME exception
			if (segmentData.length > Integer.MAX_VALUE) {
				throw new FileTooLargeException(segmentData.length);
			}

			// Write SPS 
			byteArrayOutputStream.write(nalHeader);
			byteArrayOutputStream.write(sps);

			// Write PPS 
			byteArrayOutputStream.write(nalHeader);
			byteArrayOutputStream.write(pps);

			// Write Data
			while (current < movieFragmentBoxes.size()) {
				MovieFragmentBox movieFragmentBox = 
						movieFragmentBoxes.get(current);
				int videoTrack = videoTrackId; // get Video Track

				// Get TrackRunBox, which contains Sample Inside
				TrackRunBox trackRunBox = 
						getVideoTrackRunBox(movieFragmentBox, videoTrack);
				
				// 
				if (trackRunBox == null ) {
					
					// If current > 0 (we've succeeded in reading some video 
					// segment before) In this case, number of video resources
					// is least than number of audio resource, we simply return
					// the result (sooner return)
					if (current > 0) {
						byte[] result = byteArrayOutputStream.toByteArray();
						byteArrayOutputStream.close();
						return result;
					} else { // cannot parse
						byteArrayOutputStream.close();
						return null;
					}
					
				}
				
				// Offset of each sample in TrackRunBox
				int subOffset = offset + trackRunBox.getDataOffset();
				
				List<TrackRunBox.Entry> trackRunEntries = 
						trackRunBox.getEntries();
				for (TrackRunBox.Entry entry : trackRunEntries) {
					int sampleSize = (int) entry.getSampleSize();
					byte[] sampleData = new byte[sampleSize];
					// read sample data
					System.arraycopy(segmentData, subOffset, 
							sampleData, 0, sampleSize);
					subOffset += sampleSize;

					// convert to .H264 raw
					sampleData = getH264DataOf(sampleData, nalLengthSize);
					// write to output
					byteArrayOutputStream.write(sampleData);
					
					// Just want the first sample (which is a I-sample)
					// return early if we just want to get the first image. Dont
					// need to process the rest
					if (justFirstImage) {
						sampleData = byteArrayOutputStream.toByteArray();
						byteArrayOutputStream.close();
						return sampleData;
					}
					
				}

				// Update offset, index.
				offset += entries.get(current).getReferencedSize();
				current++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}  finally {
			if (byteArrayOutputStream != null) {
				try {
					byteArrayOutputStream.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		if (byteArrayOutputStream == null) {
			return null;
		} else {

			return byteArrayOutputStream.toByteArray();
		}
	}

	/**
	 * Create h264 raw data from an MP4 input file
	 * @param filePath file path of MP4 video
	 * @return h264 raw data
	 */
	public byte[] createH264rawDataFromMP4(File filePath) {
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

			// read whole video data into byte array
			byte[] videoData = 
					FileUtils.readFileToByteArray(filePath);
			// If file is too large, throw an Runtime Exception, because casting 
			// will not work here. Note that it's a RUN TIME exception
			if (videoData.length > Integer.MAX_VALUE) {
				throw new FileTooLargeException(videoData.length);
			}

			// Write NAL
			// 1-based system
			for (long i = 1; i <= sampleQuantity; i++) {
				buffer = getSampleData(i, filePath, videoData).array();

				// NOte that each sample may have many NAL
				// Write NalRaw data of this sample 
				byte[] h264Raw = getH264DataOf(buffer, nalLengthSize);
				byteArrayOutputStream.write(h264Raw);
			}

			// get the output
			buffer = byteArrayOutputStream.toByteArray();
			return buffer;

		} catch (IOException ioe) {
			log.severe("IOException, file: " + filePath);
		} finally {
			try {
				if (byteArrayOutputStream != null) {
					byteArrayOutputStream.close();
				}
			} catch (Exception e) {
				log.severe("Error while closing stream.");
				e.printStackTrace();
			}
		}
		// default, return null.
		return null;
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
	
	/**
	 * Get AvcConfigurationBox from filePath
	 * @param filePath filePath file path to video
	 * @return AvcConfigurationBox
	 */
	private AvcConfigurationBox getAvcConfigurationBox(File filePath) {
		// All I-Samples located in moov -> trak -> mdia -> minf -> stbl ->
		// stsd. All of them are mandatory.
		IsoFile isoFile = getIsoFile(filePath);
		if (isoFile == null) {
			return null;
		}
		return getAvcConfigurationBox(isoFile);
	}

	/**
	 *  Get AvcConfigurationBox from file URL
	 * @param fileUrl url to the file
	 * @return AvcConfigurationBox
	 */
	private AvcConfigurationBox getAvcConfigurationBox(URL fileUrl) {
		IsoFile isoFile = getIsoFile(fileUrl);
		if (isoFile == null) {
			return null;
		}
		return getAvcConfigurationBox(isoFile);
	}

	/**
	 * AvcConfigurationBox from its data (in byte array)
	 * @param videoData
	 * @return AvcConfigurationBox
	 */
	private AvcConfigurationBox getAvcConfigurationBox(byte[] videoData) {
		IsoFile isoFile = getIsoFile(videoData);
		if (isoFile == null) {
			return null;
		}
		return getAvcConfigurationBox(isoFile);
	}
	
	/**
	 * AvcConfigurationBox from an isofile. This's an helper class, used by 
	 * the three above functions.
	 * @param isoFile
	 * @return AvcConfigurationBox
	 */
	private AvcConfigurationBox getAvcConfigurationBox(IsoFile isoFile) {
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
	 * Get chunkInfo of a specific sampleIndex, a helper function used by 
	 * createH264rawDataFromMP4
	 * @param sampleIndex sampleIndex to get chunkData
	 * @param sampleTableBox
	 * @return chunkInfo, if not found, return null;
	 */
	private ChunkInfo getChunkDataOf(long sampleIndex, File filePath) {
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
	 * Get all ISamples of a specific video
	 * @param filePath filePath to the file
	 * @return List of all Isamples in the video file
	 */
	public List<Long> getISamples(File filePath) {
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
	public IsoFile getIsoFile(File filePath) {
		FileInputStream fileInputStream = null;
		IsoFile isoFile = null;
		try {
			fileInputStream = new FileInputStream(filePath);
			isoFile = new IsoFile(fileInputStream.getChannel());
		} catch (FileNotFoundException fe) {
			log.info("File not found: " + filePath);
			fe.printStackTrace();
		} catch (IOException ioe) {
			log.info("IO exception");
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
				log.info("Error while closing stream");
				e.printStackTrace();
			}
		}

		return isoFile;
	}

	/**
	 * Get IsoFile from video's url
	 * @param fileUrl video's url
	 * @return isoFile
	 */
	public IsoFile getIsoFile(URL fileUrl) {
		try {
			InputStream inputStream = fileUrl.openStream();
			ReadableByteChannel chanel = Channels.newChannel(inputStream);
			IsoFile isoFile = new IsoFile(chanel);
			return isoFile;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Get IsoFile from byte array
	 * @param byteArray video's byteArray
	 * @return isoFile
	 */
	public IsoFile getIsoFile(byte[] byteArray) {
		ReadableByteChannel byteChannel = 
				Channels.newChannel(new ByteArrayInputStream(byteArray));
		IsoFile isoFile = null;
		try {
			isoFile = new IsoFile(byteChannel);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(0);
		}
		return isoFile;
	}
	
	/**
	 * Get h264Data of a sample, a helper function used by 
	 * createH264rawDataFromDashSegment() and createH264rawDataFromMP4()
	 * @param sampleData 
	 * @param nalLengthSize
	 * @return h264 raw data of a sample
	 * @throws IOException
	 */
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

		byte[] result = byteArrayOutputStream.toByteArray();
		byteArrayOutputStream.close();
		return result;
	}

	/**
	 * Get NalUnitSize
	 * @param filePath path of the video file
	 * @return NalUnitSize or -1 if error occurs
	 */
	public int getNalLengthSize(File filePath) {
		AvcConfigurationBox avcConfigurationBox = 
				getAvcConfigurationBox(filePath);
		if (avcConfigurationBox == null) {
			return -1;
		}
		return (avcConfigurationBox.getLengthSizeMinusOne() + 1);
	}

	public int getNalLengthSize(URL fileUrl) {
		AvcConfigurationBox avcConfigurationBox = 
				getAvcConfigurationBox(fileUrl);
		if (avcConfigurationBox == null) {
			return -1;
		}
		return (avcConfigurationBox.getLengthSizeMinusOne() + 1);
	}

	public int getNalLengthSize(byte[] videoData) {
		AvcConfigurationBox avcConfigurationBox = 
				getAvcConfigurationBox(videoData);
		if (avcConfigurationBox == null) {
			return -1;
		}
		return (avcConfigurationBox.getLengthSizeMinusOne() + 1);
	}
	
	/**
	 * Get PPS of the video in byte Array format
	 * @param filePath file path to video
	 * @return PPS byte array
	 */
	public ByteBuffer getPpsInByte(File filePath) {
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
	 * Get PPS of the video in byte Array format
	 * @param fileUrl url to the video
	 * @return PPS byte array
	 */
	public ByteBuffer getPpsInByte(URL fileUrl) {
		AvcConfigurationBox avcConfigurationBox = 
				getAvcConfigurationBox(fileUrl);
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
	 *  Get PPS of the video in byte Array format
	 * @param videoData video to get PPS (in byte array)
	 * @return
	 */
	public ByteBuffer getPpsInByte(byte[] videoData) {
		AvcConfigurationBox avcConfigurationBox = 
				getAvcConfigurationBox(videoData);
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
	public String getPpsInHex(File filePath) {
		byte[] pps = getPpsInByte(filePath).array();
		return ConvertUtility.byteArrayToHext(pps);
	}

	/**
	 * Get PPS of the video in Hex String format
	 * @param fileUrl url to the video
	 * @return Hex String format
	 */
	public String getPpsInHex(URL fileUrl) {
		byte[] pps = getPpsInByte(fileUrl).array();
		return ConvertUtility.byteArrayToHext(pps);
	}

	/**
	 * Get PPS of the video in Hex String format
	 * @param videoData videoData video to get PPS (in byte array)
	 * @return Hex String format
	 */
	public String getPpsInHex(byte[] videoData) {
		byte[] pps = getPpsInByte(videoData).array();
		return ConvertUtility.byteArrayToHext(pps);
	}
	
	/**
	 * Get SPS of the video in byte Array format
	 * @param filePath file path to video
	 * @return PPS byte array
	 */
	public ByteBuffer getSpsInByte(File filePath) {
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


	/**
	 * Get SPS of the video in byte Array format
	 * @param fileUrl url to the video
	 * @return PPS byte array
	 */
	public ByteBuffer getSpsInByte(URL fileUrl) {
		AvcConfigurationBox avcConfigurationBox = 
				getAvcConfigurationBox(fileUrl);
		if (avcConfigurationBox == null) {
			return null;
		}

		byte[] sps = avcConfigurationBox.getSequenceParameterSets().get(0);
		if (sps == null) {
			return null;
		}

		return ByteBuffer.wrap(sps);
	}
	
	/**
	 * Get SPS of the video in byte Array format
	 * @param videoData videoData videoData video to get SPS (in byte array)
	 * @return PPS byte array
	 */
	public ByteBuffer getSpsInByte(byte[] videoData) {
		AvcConfigurationBox avcConfigurationBox = 
				getAvcConfigurationBox(videoData);
		if (avcConfigurationBox == null) {
			return null;
		}

		byte[] sps = avcConfigurationBox.getSequenceParameterSets().get(0);
		if (sps == null) {
			return null;
		}

		return ByteBuffer.wrap(sps);
	}

	/**
	 * Get SPS of the video in Hex String format
	 * @param filePath file path to video
	 * @return Hex String format
	 */
	public String getSpsInHex(File filePath) {
		byte[] sps = getSpsInByte(filePath).array();
		return ConvertUtility.byteArrayToHext(sps);
	}

	public String getSpsInHex(URL fileUrl) {
		byte[] sps = getSpsInByte(fileUrl).array();
		return ConvertUtility.byteArrayToHext(sps);
	}

	public String getSpsInHex(byte[] videoData) {
		byte[] sps = getSpsInByte(videoData).array();
		return ConvertUtility.byteArrayToHext(sps);
	}
	
	

	/**
	 * Get sample size
	 * @param sampleIndex index of the sample in video file
	 * @param filePath file path of video file
	 * @return sample size or -1 if sampleIndex is invalid
	 */
	private long getSampleSize(long sampleIndex, File filePath){
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
	 *  Get Sample Qauntity in this video file
	 * @param filePath filePath of the video
	 * @return sample quantity or -1 if an error occured.
	 */
	public long getSampleQuantity(File filePath) {		
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
	 * Get sampleData of a sample checkBox
	 * @param sampleIndex
	 * @param sampleTableBox
	 * @param filePath
	 * @return ByteBuffer of sampleData (if any) or null
	 */
	private ByteBuffer getSampleData(long sampleIndex, File filePath, 
			byte[] videoData) {
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
		// Read sample data from video data
		byte[] sampleData = new byte[(int) size];


		// read sample data
		System.arraycopy(videoData, (int)offset , sampleData, 0, (int)size);

		return ByteBuffer.wrap(sampleData);

	}
	

	private int getVideoTrackBoxId(IsoFile isoFile) {
		MovieBox movieBox = isoFile.getMovieBox();
		List<TrackBox> trackBoxList = movieBox.getBoxes(TrackBox.class);
		
		// 1-based
		int id = 0;
		for (TrackBox trackBox : trackBoxList) {
			id++;
			MediaBox mediaBox = trackBox.getMediaBox();
			HandlerBox handlerBox = mediaBox.getHandlerBox();
			String type = handlerBox.getHandlerType();
			if (type.equalsIgnoreCase("VIDE")) {
				return id;
			}
		}
		
		return -1;
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
	
	public int getVideoTrackBoxId(byte[] videoHeader) {
		IsoFile isoFile = getIsoFile(videoHeader);
		return getVideoTrackBoxId(isoFile);
	}
	
	public int getVideoTrackBoxId(URL videoUrl) {
		IsoFile isoFile = getIsoFile(videoUrl);
		return getVideoTrackBoxId(isoFile);
	}
	
	public int getVideoTrackBoxId(File filePath) {
		IsoFile isoFile = getIsoFile(filePath);
		return getVideoTrackBoxId(isoFile);
	}
	
	private TrackRunBox getVideoTrackRunBox(MovieFragmentBox movieFragmentBox, 
			int videoTrackId) {
		List<TrackFragmentBox> trackFragmentBoxes = 
				movieFragmentBox.getBoxes(TrackFragmentBox.class);
		for (TrackFragmentBox trackFragmentBox : trackFragmentBoxes) {
			if (trackFragmentBox.getTrackFragmentHeaderBox()
					.getTrackId() == videoTrackId) {
				return trackFragmentBox.getBoxes(TrackRunBox.class).get(0);
			}
		}
		return null;
	}
	

	public static void main(String[] args) throws IOException {
		URL header = 
				new URL("https://dl.dropbox.com/u/27889409/muma/sample-dash/sample-dash3/sample_iPod_dash.mp4");
		MP4Parser mp4Parser = new MP4Parser();
		//		byte[] result = mp4Parser.createH264rawDataFromMP4(filePath);

		String sps = mp4Parser.getSpsInHex(header);
		byte[] spsData = ConvertUtility.hexStringToByteArray(sps);
		System.out.println(sps);
		
		String pps = mp4Parser.getPpsInHex(header);
		byte[] ppsData = ConvertUtility.hexStringToByteArray(pps);
		System.out.println(pps);
		
		// Nal header
		byte[] nalHeader = {
				0x00,
				0x00,
				0x00, 
				0x01}; 
		
		int nalLengthSize = mp4Parser.getNalLengthSize(header);
		System.out.println("nalLengthSize: " + nalLengthSize);

		int videoTrackId = mp4Parser.getVideoTrackBoxId(header);
		System.out.println("videoTrackId: " + videoTrackId); 
		
		URL segmentUrl = new URL("https://dl.dropbox.com/u/27889409/muma/sample-dash/sample-dash3/sample_iPod_out6.m4s");
		byte[] segmentData = IOUtils.toByteArray(segmentUrl.openStream());
		
		
		
		byte[] result = mp4Parser.createH264rawDataFromDashSegment(
				nalHeader, 
				spsData, 
				ppsData, 
				segmentData, 
				nalLengthSize, 
				videoTrackId);

		//
		//		byte[] result = 
		//				mp4Parser.createH264rawDataFromDashSegment(filePath, sps, pps);

//				BufferedOutputStream bos = new BufferedOutputStream(
//						new FileOutputStream(new File("resources/nxhoaf.h264")));
//				bos.write(result);
//				bos.close();

		//		byte[] sampleData = mp4Parser.getSampleData(1, filePath).array();
		//		System.out.println(mp4Parser.getSampleSize(1, filePath));
		//		System.out.println(ConvertUtility.byteArrayToHext(sampleData));

		log.info("done");
	}
}

