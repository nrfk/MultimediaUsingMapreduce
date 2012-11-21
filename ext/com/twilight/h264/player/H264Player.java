package com.twilight.h264.player;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.common.util.concurrent.ExecutionError;
import com.twilight.h264.decoder.AVFrame;
import com.twilight.h264.decoder.AVPacket;
import com.twilight.h264.decoder.H264Decoder;
import com.twilight.h264.decoder.MpegEncContext;

import fr.telecomParistech.image.bitmap.BitmapHeader;
import fr.telecomParistech.image.bitmap.BitmapHeader.Attribute;
import fr.telecomParistech.image.bitmap.ConvertUtility;

public class H264Player{

	public static final int INBUF_SIZE = 65535;
	private static final Logger LOGGER = 
			Logger.getLogger(H264Player.class.getName());
	private static final FileService fileService = 
			FileServiceFactory.getFileService();
	private static final ImagesService imagesService = 
			ImagesServiceFactory.getImagesService();

	private static final String DEFAULT_URL = 
			"https://dl.dropbox.com/u/27889409/muma/sample-h264j/slamtv10.264";
	
	static {
		LOGGER.setLevel(Level.INFO);
	}

	private String fName;
	private int[] buffer = null;
	private static int imageCounter = 0;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new H264Player(args);
	}
	public H264Player() {}

	public H264Player(String[] args) {



		String[] sample_clips = {
				"sample_clips/sample_iPod.m4v",
				"sample_clips/admiral.264",
				"sample_clips/slamtv10.264",
				"sample_clips/slamtv60.264",
		}; 

		System.out.println(sample_clips.length);
		fName = sample_clips[2];

	}

	public void run() {
		System.out.println("Playing "+ fName);
		playFile(fName, 200);		
	}

	public String playFile(String filename, int imageNo) {
		LOGGER.info("Start play file: " + filename);
		String imageUrl = "";

		URL url = null;
		InputStream inputStream = null;
//		ByteArrayInputStream bais = null;
//		
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			url = new URL(filename);
			inputStream = url.openStream();
			byte[] buffer = new byte[1024];
			int n;
//			while ((n = inputStream.read(buffer, 0, 1024)) > 0) {
//				baos.write(buffer, 0, n);
//			}
//			
//			bais = new ByteArrayInputStream(baos.toByteArray());
			
		} catch (MalformedURLException e) {
			LOGGER.info("MalformedURLException");
			e.printStackTrace();
		} catch (IOException ioe) {
			LOGGER.info("IOException");
			ioe.printStackTrace();
		}
		
		H264Decoder codec;
		MpegEncContext mpegEncContext= null;
		
		
		int frame, len;
		int[] got_picture = new int[1];
		File f = new File(filename);
		AVFrame avFrame;
		//uint8_t inbuf[INBUF_SIZE + H264Context.FF_INPUT_BUFFER_PADDING_SIZE];
		byte[] inbuf_byte = new byte[INBUF_SIZE + MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE];
		int[] inbuf_int = new int[INBUF_SIZE + MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE];
		//char buf[1024];
		AVPacket avPacket = new AVPacket();

		avPacket.av_init_packet();

		/* set end of buffer to 0 (this ensures that no overreading happens for damaged mpeg streams) */
		Arrays.fill(inbuf_byte, INBUF_SIZE, MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE + INBUF_SIZE, (byte)0);

		System.out.println("Video decoding\n");

		/* find the mpeg1 video decoder */
		codec = new H264Decoder();
		mpegEncContext = MpegEncContext.avcodec_alloc_context();
		avFrame= AVFrame.avcodec_alloc_frame();

		if((codec.capabilities & H264Decoder.CODEC_CAP_TRUNCATED)!=0)
			mpegEncContext.flags |= MpegEncContext.CODEC_FLAG_TRUNCATED; /* we do not send complete frames */

		/* For some codecs, such as msmpeg4 and mpeg4, width and height
	       MUST be initialized there because this information is not
	       available in the bitstream. */

		/* open it */
		if (mpegEncContext.avcodec_open(codec) < 0) {
			System.out.println("could not open codec\n");
			System.exit(1);
		}

		try {
			LOGGER.info("Creating file...");
			/* the codec gives us the frame size, in samples */
//			inputStream = new FileInputStream(f);
//			inputStream = bais;
			LOGGER.info("File created...");
			frame = 0;
			int dataPointer; // Current pointer position in the file

			// avPacket must contain exactly 1 NAL Unit in order for decoder to decode correctly.
			// thus we must read until we get next NAL header before sending it to decoder.
			// Find 1st NAL
			int[] cacheRead = new int[3];
			cacheRead[0] = inputStream.read();
			cacheRead[1] = inputStream.read();
			cacheRead[2] = inputStream.read();

			// Find the 1st NAL
			while(!(cacheRead[0] == 0x00 &&
					cacheRead[1] == 0x00 &&
					cacheRead[2] == 0x01 )) {
				cacheRead[0] = cacheRead[1];
				cacheRead[1] = cacheRead[2];
				cacheRead[2] = inputStream.read();
			}
			// End Find the 1st NAL

			boolean hasMoreNAL = true;

			// 4 first bytes always indicate NAL header
			inbuf_int[0] = 0x00;
			inbuf_int[1] = 0x00;
			inbuf_int[2] = 0x00;
			inbuf_int[3] = 0x01;

			while(hasMoreNAL) {
				dataPointer = 4;

				// Find next NAL
				cacheRead[0] = inputStream.read();
				if(cacheRead[0]==-1) {
					hasMoreNAL = false;
				}

				cacheRead[1] = inputStream.read();
				if(cacheRead[1]==-1) {
					hasMoreNAL = false;
				}

				cacheRead[2] = inputStream.read();
				if(cacheRead[2]==-1) {
					hasMoreNAL = false;
				}

				while(!(
						cacheRead[0] == 0x00 &&
						cacheRead[1] == 0x00 &&
						cacheRead[2] == 0x01 
						) && hasMoreNAL) {
					inbuf_int[dataPointer++] = cacheRead[0]; // Save data to data buffer
					cacheRead[0] = cacheRead[1];
					cacheRead[1] = cacheRead[2];
					cacheRead[2] = inputStream.read();
					if(cacheRead[2]==-1) {
						hasMoreNAL = false;
					}
				} // while
				// End next NAL

				avPacket.size = dataPointer;

				avPacket.data_base = inbuf_int;
				avPacket.data_offset = 0;


				while (avPacket.size > 0) {
					LOGGER.info("Creating image frame...");
					len = mpegEncContext.avcodec_decode_video2(avFrame, got_picture, avPacket);
					if (len < 0) {
						LOGGER.info("Error while decoding frame "+ frame);
						// Discard current packet and proceed to next packet
						break;
					} // if
					
					LOGGER.info("got_picture[0]: " + got_picture[0]);
					if (got_picture[0]!=0) {
						
						LOGGER.info("Got a picture...");
						avFrame = mpegEncContext.priv_data.displayPicture;

						int bufferSize = avFrame.imageWidth * avFrame.imageHeight;
						if (buffer == null || bufferSize != buffer.length) {
							buffer = new int[bufferSize];
						}

						LOGGER.info("FrameUtils.YUV2RGB()....");
						FrameUtils.YUV2RGB(avFrame, buffer);
						LOGGER.info("imageCounter: " + imageCounter + " imageNo: " + imageNo);
						if (imageCounter == imageNo) {
							ByteBuffer byteBuffer = ByteBuffer.allocate(buffer.length * 4);
							byte[] data;

							// Data in h264 is stored in reversed order as in 
							// .bmp, so we need to "flip" up side down the image
							for (int i = buffer.length - 1; i >= 0; i--) {
								data = ConvertUtility.integerToByteArray(
												buffer[i], 
												ByteOrder.LITTLE_ENDIAN);
								byte[] newData = new byte[4];
								newData[0] = data[2];
								newData[1] = data[3];
								newData[2] = data[0];
								newData[3] = data[1];
								
								byteBuffer.put(newData);
							}

							// Full image
							data = byteBuffer.array();
							byte[] header = 
									generateImageHeader(
											avFrame.imageWidth, 
											avFrame.imageHeight, 
											data.length);

							byte[] image = new byte[data.length + header.length];
							System.arraycopy(header, 0, image, 0, header.length);
							System.arraycopy(data, 0, image, header.length, data.length);
							LOGGER.info("imageSize: " + image.length);
							

							// Write to blob file
							AppEngineFile file = 
									fileService.createNewBlobFile("image/bmp");
							boolean lock = true;
							FileWriteChannel writeChannel = 
									fileService.openWriteChannel(file, lock);
							writeChannel.write(ByteBuffer.wrap(image, 0, image.length));
							writeChannel.closeFinally();

							BlobKey blobKey = fileService.getBlobKey(file);
							ServingUrlOptions servingUrlOptions = ServingUrlOptions.Builder.withBlobKey(blobKey);



							imageUrl = imagesService.getServingUrl(servingUrlOptions);
//							imageUrl = file.getFullPath();
							
							LOGGER.info("imageUrl #" + imageCounter + ": " + imageUrl);
							return imageUrl;
						}
						imageCounter ++;
					}
					avPacket.size -= len;
					avPacket.data_offset += len;
				}


			} // while


		} catch (FileNotFoundException e) {
			LOGGER.info("Error while creating file");
			e.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			try { inputStream.close(); } catch(Exception ee) {}
		} // try

		mpegEncContext.avcodec_close();
		mpegEncContext = null;
		avFrame = null;
		System.out.println("Stop playing video.");

		return imageUrl;
	}
	private byte[] generateImageHeader(int width, int height, int dataSize) {
		BitmapHeader bitmapHeader = new BitmapHeader();

		// Signature
		bitmapHeader.setAttribute(
				Attribute.SIGNATURE, 
				new byte[] {(byte)0x42,0x4D});

		// File Size, We set a place holder here, we'll come back later 
		// when we fullfill the header.
		bitmapHeader.setAttribute(
				Attribute.FILE_SIZE, 
				new byte[] {(byte)0x00,0x00,0x00,0x00});


		// Reserve 1 
		bitmapHeader.setAttribute(
				Attribute.RESERVED_1, 
				new byte[] {0x00,0x00});

		// Reserve 2 
		bitmapHeader.setAttribute(
				Attribute.RESERVED_2, 
				new byte[] {0x00,0x00});

		// Offset where the pixel array 
		bitmapHeader.setAttribute(
				Attribute.FILE_OFFSET_TO_PIXEL_ARRAY, 
				new byte[] {0x7A,0x00,0x00,0x00});

		// ------------ DIB Header --------------------
		// Number of bytes in the DIB header (from this point)
		bitmapHeader.setAttribute(
				Attribute.DIB_HEADER_SIZE, 
				new byte[] {0x6C,0x00,0x00,0x00});


		bitmapHeader.setAttribute(
				Attribute.IMAGE_WIDTH, 
				ConvertUtility.integerToByteArray(width, ByteOrder.LITTLE_ENDIAN));


		bitmapHeader.setAttribute(
				Attribute.IMAGE_HEIGHT, 
				ConvertUtility.integerToByteArray(height, ByteOrder.LITTLE_ENDIAN));

		// Number of color planes being used
		bitmapHeader.setAttribute(
				Attribute.PLANES, 
				new byte[] {0x01,0x00});

		bitmapHeader.setAttribute(
				Attribute.BITS_PER_PIXEL, 
				new byte[] {0x20,0x00});


		//		bitmapHeader.setAttribute(
		//				Attribute.IMAGE_HEIGHT, 
		//				new byte[] {0x02,0x00,0x00,0x00});

		// Compression
		bitmapHeader.setAttribute(
				Attribute.COMPRESSION, 
				new byte[] {0x03,0x00,0x00,0x00});

		bitmapHeader.setAttribute(
				Attribute.IMAGE_SIZE, 
				new byte[] {0x20,0x00,0x00,0x00});

		// Horizontal resolution of the image
		bitmapHeader.setAttribute(
				Attribute.X_PIXELS_PER_METER, 
				new byte[] {0x13,0x0B,0x00,0x00});

		// Vertical resolution of the image
		bitmapHeader.setAttribute(
				Attribute.Y_PIXELS_PER_METER, 
				new byte[] {0x13,0x0B,0x00,0x00});

		// 	Number of colors in the palette
		bitmapHeader.setAttribute(
				Attribute.COLORS_IN_COLOR_TABLE, 
				new byte[] {0x00,0x00,0x00,0x00});

		// 0 means all colors are important
		bitmapHeader.setAttribute(
				Attribute.IMPORTANT_COLOR_COUNT, 
				new byte[] {0x00,0x00,0x00,0x00});

		//	Red channel bit mask
		bitmapHeader.setAttribute(
				Attribute.RED_CHANNEL_BITMASK, 
				new byte[] {0x00,0x00,(byte)0xFF,0x00});

		// Green
		bitmapHeader.setAttribute(
				Attribute.GREEN_CHANNEL_BITMASK, 
				new byte[] {0x00,(byte)0xFF,0x00,0x00});

		// Blue
		bitmapHeader.setAttribute(
				Attribute.BLUE_CHANNEL_BITMASK, 
				new byte[] {(byte)0xFF,0x00,0x00,0x00});

		// Alpha
		bitmapHeader.setAttribute(
				Attribute.ALPHA_CHANNEL_BITMASK, 
				new byte[] {0x00,0x00,0x00, (byte)0xFF});

		// Type of Color Space
		bitmapHeader.setAttribute(
				Attribute.COLOR_SPACE_TYPE, 
				new byte[] {0x20,0x6E,0x69, 0x57});

		// Color Space end points
		bitmapHeader.setAttribute(
				Attribute.COLOR_SPACE_ENDPOINTS, 
				new byte[] {
						0x00,0x00,0x00, 0x00,
						0x00,0x00,0x00, 0x00,
						0x00,0x00,0x00, 0x00,
						0x00,0x00,0x00, 0x00,
						0x00,0x00,0x00, 0x00,
						0x00,0x00,0x00, 0x00,
						0x00,0x00,0x00, 0x00,
						0x00,0x00,0x00, 0x00,
						0x00,0x00,0x00, 0x00,
				});

		// Red, Green, Blue Gamma
		bitmapHeader.setAttribute(
				Attribute.GAMMA_FOR_RED_CHANNEL, 
				new byte[] {0x00,0x00,0x00, 0x00});

		bitmapHeader.setAttribute(
				Attribute.GAMMA_FOR_GREEN_CHANNEL, 
				new byte[] {0x00,0x00,0x00, 0x00});

		bitmapHeader.setAttribute(
				Attribute.GAMMA_FOR_BLUE_CHANNEL, 
				new byte[] {0x00,0x00,0x00, 0x00});

		int headerSize = bitmapHeader.dump().length;
		int fileSize = headerSize + dataSize;
		LOGGER.info("headerSize: " + headerSize);
		LOGGER.info("file size: " + fileSize);

		bitmapHeader.setAttribute(
				Attribute.FILE_SIZE, 
				ConvertUtility.integerToByteArray(fileSize, ByteOrder.LITTLE_ENDIAN));

		return bitmapHeader.dump();
	}

}
