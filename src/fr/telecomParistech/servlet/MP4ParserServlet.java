package fr.telecomParistech.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

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
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;

import fr.telecomParistech.mp4parser.ChunkInfo;

/**
 * Parser MP4 file to get I-Frame
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class MP4ParserServlet extends HttpServlet {
	private static final long serialVersionUID = 297497904326701394L;
	private static final FileService fileService = 
			FileServiceFactory.getFileService();
	private static final Logger LOGGER = 
			Logger.getLogger(MP4ParserServlet.class.getName());
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String senderUrl = req.getParameter("senderUrl");
		senderUrl = "http://localhost:8888/resources/sample_iTunes.mov";
		URL url = null;
		SortedMap<String, String> imageUriMap = new TreeMap<String, String>();
		try {

			url = new URL(senderUrl);
			InputStream inputStream = url.openStream();


			ReadableByteChannel chanel = Channels.newChannel(inputStream);

			IsoFile isoFile = new IsoFile(chanel);
			MovieBox movieBox = isoFile.getMovieBox();

			TrackBox trackBox = getVideoTrackBox(movieBox);
			MediaBox mediaBox = trackBox.getMediaBox();
			MediaInformationBox mediaInformationBox = 
					mediaBox.getMediaInformationBox();
			SampleTableBox sampleTableBox = 
					mediaInformationBox.getSampleTableBox();
			SyncSampleBox syncSampleBox = sampleTableBox.getSyncSampleBox();

			long[] sampleNumber = syncSampleBox.getSampleNumber();
			
			byte[] mediaData = IOUtils.toByteArray(url.openStream());
			for (int i = 0; i < sampleNumber.length; i++) {
				System.out.print(sampleNumber[i] + "   ");
				byte[] image = getSampleData(sampleNumber[i], 
						sampleTableBox, mediaData);
				System.out.println(image.length);

				AppEngineFile file = 
						fileService.createNewBlobFile("application/zip");
				boolean lock = true;
				FileWriteChannel writeChannel = 
						fileService.openWriteChannel(file, lock);


				writeChannel.write(ByteBuffer.wrap(image, 0, image.length));
				writeChannel.closeFinally();
				imageUriMap.put("I-Frame " + sampleNumber[i], 
						file.getFullPath());
			}
			System.out.println("\n");

			isoFile.close();
			chanel.close();
			//			fis.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 

		PrintWriter pw = resp.getWriter();
		pw.println("List of Iframes: " );

		Set<java.util.Map.Entry<String, String>> imageSet = 
				imageUriMap.entrySet();
		for (java.util.Map.Entry<String, String> image : imageSet) {
			pw.println("   " + image.getKey() + ": " + image.getValue());
		}
		pw.close();
	}

	
	/**
	 * Get VideoTrackBox form movieBox
	 * @param movieBox movieBox to get the track box
	 * @return TrackBox
	 */
	public TrackBox getVideoTrackBox(MovieBox movieBox) {
		List<TrackBox> trackBoxList = 
				movieBox.getBoxes(TrackBox.class);
		for (TrackBox trackBox : trackBoxList) {
			TrackHeaderBox trackHeaderBox = 
					trackBox.getTrackHeaderBox();
			// Audio Tracks don't have width and weight
			if ((trackHeaderBox.getWidth() > 0) 
					&& (trackHeaderBox.getHeight() > 0)) {
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
	 * @return
	 */
	public byte[] getSampleData(long sampleIndex, 
			SampleTableBox sampleTableBox, byte[] mediaData) {

		ChunkInfo chunkData = getChunkDataOf(sampleIndex, sampleTableBox);
		long offset = chunkData.getOffset();
		long currentSample = chunkData.getFirtSample();

		while (currentSample < sampleIndex) {
			offset += getSampleSize(currentSample, sampleTableBox);
			currentSample ++;
		}

		long size = getSampleSize(sampleIndex, sampleTableBox);
		byte[] image = new byte[(int) size];
		System.arraycopy(mediaData, (int)offset , image, 0, (int) size);

		return image;

	}

	/**
	 * Get size of a specific sample
	 * @param sampleIndex
	 * @param sampleTableBox
	 * @return
	 */
	public long getSampleSize(long sampleIndex, SampleTableBox sampleTableBox) {
		long sampleQty = getSampleQuantity(sampleTableBox);
		if ((sampleIndex > sampleQty) || (sampleIndex < 0)) {
			return -1;
		}

		// Convert 1-based to 0-based:
		sampleIndex--;

		SampleSizeBox sampleSizeBox = sampleTableBox.getSampleSizeBox();
		return sampleSizeBox.getSampleSizeAtIndex((int)sampleIndex);
	}

	/**
	 * Get chunkData of a specific sample
	 * @param sampleIndex
	 * @param sampleTableBox
	 * @return
	 */
	public ChunkInfo getChunkDataOf(long sampleIndex, 
			SampleTableBox sampleTableBox) {
		long sampleQty = getSampleQuantity(sampleTableBox);
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

		// else, the it belongs to the last trunk
		chunkData.setIndex(currentChunk);
		chunkData.setFirtSample(firstSampleIndex);

		ChunkOffsetBox chunkOffsetBox = sampleTableBox.getChunkOffsetBox();
		long chunkOffset = (chunkOffsetBox.getChunkOffsets())[currentChunk - 1];
		chunkData.setOffset(chunkOffset);

		return chunkData;
	}

	/**
	 * Get chunk Quantity
	 * @param sampleTableBox sampleTableBox in which we want to get chunk qty.
	 * @return chunk quantity
	 */
	public long getChunkQuantity(SampleTableBox sampleTableBox) {
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

	/**
	 * Get sample Quantity
	 * @param sampleTableBox sampleTableBox in which we want to get sample qty
	 * @return sample quantity
	 */
	public long getSampleQuantity(SampleTableBox sampleTableBox) {		
		SampleSizeBox sampleSizeBox = sampleTableBox.getSampleSizeBox();
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
	 * @return formated String.
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

}
