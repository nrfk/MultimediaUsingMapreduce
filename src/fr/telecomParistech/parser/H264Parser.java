package fr.telecomParistech.parser;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.twilight.h264.decoder.AVFrame;
import com.twilight.h264.decoder.AVPacket;
import com.twilight.h264.decoder.H264Decoder;
import com.twilight.h264.decoder.MpegEncContext;
import com.twilight.h264.player.FrameUtils;

import fr.telecomParistech.image.bitmap.BitmapHeader;
import fr.telecomParistech.image.bitmap.BitmapHeader.Attribute;
import fr.telecomParistech.image.bitmap.ConvertUtility;

/**
 * H264Parser is the modified version of H264Player situated at 
 * com.twilight.h264.player. It's used to parse H264 raw file
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class H264Parser {

	public static final int INBUF_SIZE = 65535;
	private static final Logger LOGGER = 
			Logger.getLogger(H264Parser.class.getName());
	

	private int[] buffer = null;
	

	static {
		LOGGER.setLevel(Level.SEVERE);
	}

	/**
	 * parse H264 raw and return its I-image
	 * @param h264Raw h264 raw data in byte array
	 * @return
	 */
	public byte[] parseH264Raw(byte[] h264Raw) {
		// Get Inputstream
		InputStream inputStream = new ByteArrayInputStream(h264Raw);

		H264Decoder codec;
		MpegEncContext mpegEncContext= null;


		int frame, len;
		int[] got_picture = new int[1];
		AVFrame avFrame;
		//uint8_t inbuf[INBUF_SIZE + H264Context.FF_INPUT_BUFFER_PADDING_SIZE];
		byte[] inbuf_byte = new byte[INBUF_SIZE + 
		                             MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE];
		int[] inbuf_int = new int[INBUF_SIZE + 
		                          MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE];
		//char buf[1024];
		AVPacket avPacket = new AVPacket();

		avPacket.av_init_packet();

		/* set end of buffer to 0 (this ensures that no overreading happens 
		 * for damaged mpeg streams) */
		Arrays.fill(inbuf_byte, 
				INBUF_SIZE, 
				MpegEncContext.FF_INPUT_BUFFER_PADDING_SIZE + INBUF_SIZE, 
				(byte)0);

		System.out.println("Video decoding\n");

		/* find the mpeg1 video decoder */
		codec = new H264Decoder();
		mpegEncContext = MpegEncContext.avcodec_alloc_context();
		avFrame= AVFrame.avcodec_alloc_frame();

		if((codec.capabilities & H264Decoder.CODEC_CAP_TRUNCATED)!=0) {
			/* we do not send complete frames */
			mpegEncContext.flags |= MpegEncContext.CODEC_FLAG_TRUNCATED;
		}


		/* For some codecs, such as msmpeg4 and mpeg4, width and height
	       MUST be initialized there because this information is not
	       available in the bitstream. */

		/* open it */
		if (mpegEncContext.avcodec_open(codec) < 0) {
			System.out.println("could not open codec\n");
			System.exit(1);
		}

		try {
			/* the codec gives us the frame size, in samples */
			//			inputStream = new FileInputStream(f);
			//			inputStream = bais;
			frame = 0;
			int dataPointer; // Current pointer position in the file

			// avPacket must contain exactly 1 NAL Unit in order for decoder 
			// to decode correctly.thus we must read until we get next NAL 
			// header before sending it to decoder. Find 1st NAL
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
					// Save data to data buffer
					inbuf_int[dataPointer++] = cacheRead[0]; 
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
					len = mpegEncContext.avcodec_decode_video2(
							avFrame, got_picture, avPacket);
					if (len < 0) {
						// Discard current packet and proceed to next packet
						break;
					} // if

					if (got_picture[0]!=0) {

						avFrame = mpegEncContext.priv_data.displayPicture;

						int bufferSize = avFrame.imageWidth 
								* avFrame.imageHeight;
						if (buffer == null || bufferSize != buffer.length) {
							buffer = new int[bufferSize];
						}

						FrameUtils.YUV2RGB(avFrame, buffer);
						ByteBuffer byteBuffer = 
								ByteBuffer.allocate(buffer.length * 4);
						byte[] data;

						// Data in h264 is stored in reversed order as in 
						// .bmp, so we need to "flip" up side down the image
						for (int i = avFrame.imageHeight - 1; i >= 0; i--) {
							for (int j = 0; j < avFrame.imageWidth; j++) {
								data = ConvertUtility.integerToByteArray(
										buffer[avFrame.imageWidth*i + j], 
										ByteOrder.LITTLE_ENDIAN);
								byteBuffer.put(data);
							}
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
						System.arraycopy(
								data, 0, 
								image, header.length, 
								data.length);
						LOGGER.info("imageSize: " + image.length);

						// Now, we return the image, don't need to continue 
						// the decoding-precess.
						inputStream.close();
						return image;
					}
					avPacket.size -= len;
					avPacket.data_offset += len;
				}


			} // while


		} catch (FileNotFoundException fe) {
			LOGGER.info("Error while creating file");
			fe.printStackTrace();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		} finally {
			try { inputStream.close(); } catch(Exception ee) {}
		} // try

		mpegEncContext.avcodec_close();
		mpegEncContext = null;
		avFrame = null;
		System.out.println("Stop playing video.");
		return null;
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
				ConvertUtility.integerToByteArray(fileSize, 
						ByteOrder.LITTLE_ENDIAN));

		return bitmapHeader.dump();
	}

}
