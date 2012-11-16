package com.twilight.h264.player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.logging.Logger;

import com.twilight.h264.decoder.AVFrame;
import com.twilight.h264.decoder.AVPacket;
import com.twilight.h264.decoder.H264Decoder;
import com.twilight.h264.decoder.MpegEncContext;

import fr.telecomParistech.image.bitmap.BitmapHeader;
import fr.telecomParistech.image.bitmap.BitmapHeader.Attribute;
import fr.telecomParistech.image.bitmap.ConvertUtility;

/**
 * Decode frame encoded by h264 encoder into a bitmap file
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class H264DecoderUtil {
	public static final int INBUF_SIZE = 65535;
	private static final Logger LOGGER = 
			Logger.getLogger(H264DecoderUtil.class.getName());
	private int[] buffer = null;
	private static int imageCounter = 0;
	/**
	 * Default constructor
	 */
	public H264DecoderUtil() {}
	
	public void decode(String filename) {
		FileInputStream fileInputStream = null;
		int frame;
		int length;
		int[] got_picture = new int[1];
		File file = new File(filename);
		
		//uint8_t inbuf[INBUF_SIZE + H264Context.FF_INPUT_BUFFER_PADDING_SIZE];
		byte[] inbuf_byte = 
				new byte[INBUF_SIZE + 
				         MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE];
		int[] packageData = new int[INBUF_SIZE + 
		                          MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE];
		//char buf[1024];
		AVPacket avPacket = new AVPacket();
		avPacket.av_init_packet();
	
		// set end of buffer to 0 (this ensures that no overreading happens 
		// for damaged mpeg streams)
		Arrays.fill(inbuf_byte, INBUF_SIZE, 
				MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE + INBUF_SIZE, 
				(byte)0);
	
		LOGGER.info("Video decoding");
		/* find the mpeg1 video decoder */
		H264Decoder h264Decoder = new H264Decoder();
		MpegEncContext mpegEncContext = MpegEncContext.avcodec_alloc_context();
		AVFrame avFrame = AVFrame.avcodec_alloc_frame();
	
		if((h264Decoder.capabilities & H264Decoder.CODEC_CAP_TRUNCATED)!=0) {
			/* we do not send complete frames */
			mpegEncContext.flags |= MpegEncContext.CODEC_FLAG_TRUNCATED; 
		}
			
		/* For some codecs, such as msmpeg4 and mpeg4, width and height
	       MUST be initialized there because this information is not
	       available in the bitstream. */
	
		/* open it */
		if (mpegEncContext.avcodec_open(h264Decoder) < 0) {
			LOGGER.info("Could not open codec");
			System.exit(1);
		}
	
		
		try {
			fileInputStream = new FileInputStream(file);
			frame = 0;
			int dataPointer; // Current pointer position in the file
		
			// avPacket must contain exactly 1 NAL Unit in order for 
			// decoder to decode correctly. Thus we must read until we get next 
			// NAL header before sending it to decoder.
			
			// Find 1st NAL
			LOGGER.info("Find first");
			int[] cacheRead = new int[3]; // Cache for checking NAL
			cacheRead[0] = fileInputStream.read();
			cacheRead[1] = fileInputStream.read();
			cacheRead[2] = fileInputStream.read();

			// Find the 1st NAL
			while(!(cacheRead[0] == 0x00 &&
					cacheRead[1] == 0x00 &&
					cacheRead[2] == 0x01 )) {
				cacheRead[0] = cacheRead[1];
				cacheRead[1] = cacheRead[2];
				cacheRead[2] = fileInputStream.read();
			}
			// End Find the 1st NAL
			
			boolean hasMoreNAL = true;
			
			// 4 first bytes always indicate NAL header
			packageData[0] = 0x00;
			packageData[1] = 0x00;
			packageData[2] = 0x00;
			packageData[3] = 0x01;
			
			// Find out a NAL, extract its frame and save
			while (hasMoreNAL) {
				
				// *************** 1. Copy all data into frameData ************
				LOGGER.info("Copy data in to frameData...");
				// Start from 4 because we've already wrote 4 bytes as NAL
				// header
				dataPointer = 4;
				
				// Find next NAL
				cacheRead[0] = fileInputStream.read();
				if(cacheRead[0] == -1) {
					hasMoreNAL = false;
				}
				
				cacheRead[1] = fileInputStream.read();
				if(cacheRead[1]==-1) {
					hasMoreNAL = false;
				}

				cacheRead[2] = fileInputStream.read();
				if(cacheRead[2]==-1) {
					hasMoreNAL = false;
				}
				
				// while hasMoreNAL and data in cacheRead not a NAL
				// In other words, we will copy all data into innuf_int until
				// we see the next NAL
				while(!(cacheRead[0] == 0x00 && 
						cacheRead[1] == 0x00 && 
						cacheRead[2] == 0x01 ) 
						&& hasMoreNAL) {
					// Save data to data buffer
					packageData[dataPointer++] = cacheRead[0]; 
					cacheRead[0] = cacheRead[1];
					cacheRead[1] = cacheRead[2];
					cacheRead[2] = fileInputStream.read();
					if(cacheRead[2]==-1) {
						hasMoreNAL = false;
					}
				} // while
				
				// Ok, now we have all the frame's data in frameData var
				
				// *************** 2. Decode h264 data in frameData ************
				LOGGER.info("Decode data in frame Data");
				avPacket.size = dataPointer;
				avPacket.data_base = packageData;
				avPacket.data_offset = 0;
				
				while (avPacket.size > 0) {
					length = mpegEncContext.avcodec_decode_video2(
							avFrame, got_picture, avPacket);
					
					if (length < 0) {
						LOGGER.info("Error while decoding frame "+ 
								frame + " length: " + length);
						// Discard current packet and proceed to next packet
						break;
					} // if
					
					if(got_picture[0] != 0) {
						avFrame = mpegEncContext.priv_data.displayPicture;
						
						int bufferSize = 
								avFrame.imageWidth * avFrame.imageHeight;
						if (buffer == null || bufferSize != buffer.length) {
							buffer = new int[bufferSize];
						}
						
						FrameUtils.YUV2RGB(avFrame, buffer);
						
						ByteBuffer byteBuffer = 
								ByteBuffer.allocate(buffer.length * 4);
						byte[] data;

						// Data in h264 is stored in reversed order as in 
						// .bmp, so we need to "flip" up side down the image
						for (int i = buffer.length - 1; i >= 0; i--) {
							data = ConvertUtility.integerToByteArray(
											buffer[i], 
											ByteOrder.LITTLE_ENDIAN);
							byteBuffer.put(data);
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
						System.arraycopy(data, 0, image, header.length, 
								data.length);

//						String imageName = "image" + imageCounter + ".bmp";
//						FileOutputStream fileOutputStream = 
//								new FileOutputStream(imageName);
//						fileOutputStream.write(image);
//						fileOutputStream.close();
						
//						String dataName = "image" + imageCounter + ".dat";
//						fileOutputStream = new FileOutputStream(imageName);
//						fileOutputStream.write(packageData);
//						fileOutputStream.close();
						
						LOGGER.info("image #" +imageCounter +" created");
						LOGGER.info("imageSize: " + image.length);
						imageCounter++;
						if (imageCounter == 10) {
							return;
						}
					}
					LOGGER.info("avPacket size: " + avPacket.size);
					LOGGER.info("length: " + length);
					avPacket.size -= length;
					avPacket.data_offset += length;
				}
			}
		} catch (FileNotFoundException fe) {
			LOGGER.info("File not found: " + file);
		} catch (IOException ioe) {
			LOGGER.info("IO exception");
			ioe.printStackTrace();
		}
		mpegEncContext.avcodec_close();
		mpegEncContext = null;
		avFrame = null;
		System.out.println("Stop playing video.");
	}
	
	
	/**
	 * Generate bitmap header
	 * @param width
	 * @param height
	 * @param dataSize size of RGB array
	 * @return byte array of header file
	 */
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
				ConvertUtility.integerToByteArray(width, 
						ByteOrder.LITTLE_ENDIAN));
		
		
		bitmapHeader.setAttribute(
				Attribute.IMAGE_HEIGHT, 
				ConvertUtility.integerToByteArray(height, 
						ByteOrder.LITTLE_ENDIAN));
		
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

	public static void main(String[] args) {
		H264DecoderUtil decoder = new H264DecoderUtil();
		decoder.decode("sample_clips/image.h264");
	}
}
