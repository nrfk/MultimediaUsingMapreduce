package fr.telecomParistech.servlet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;

import fr.telecomParistech.image.bitmap.BitmapHeader;
import fr.telecomParistech.image.bitmap.BitmapHeader.Attribute;
import fr.telecomParistech.image.bitmap.ConvertUtility;

/**
 * 
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class ImageGeneratorServlet extends HttpServlet{
	
	private static final long serialVersionUID = -9217605282151494852L;
	private static final FileService fileService = FileServiceFactory.getFileService();
	private static final BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
	private static final Logger log = 
			Logger.getLogger(ImageGeneratorServlet.class.getName());
	static {
		log.setLevel(Level.INFO);
	}
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		String color = req.getParameter("color");
		String size = req.getParameter("size");
		
		if ((color == null) || (size == null)) {
			return;
		}
		
		/// Set width
		int width = 0;
		int height = 0;
		if ("LARGE".equalsIgnoreCase(size)) {
			width = 640;
			height = 480;
		} else if ("MEDIUM".equalsIgnoreCase(size)) {
			width = 320;
			height = 240;
		} else if ("SMALL".equalsIgnoreCase(size)) {
			width = 160;
			height = 120;
		} else if ("TINY".equalsIgnoreCase(size)) {
			width = 80;
			height = 60;
		} else { // medium, default
			width = 320;
			height = 240;
		}
		log.info("The image size will be: " + width + " x " + height);
		
		byte[] data = generateImageData(width, height, color);
		byte[] header = generateImageHeader(width, height, data.length);
		
		byte[] image = new byte[data.length + header.length];
		System.arraycopy(header, 0, image, 0, header.length);
		System.arraycopy(data, 0, image, header.length, data.length);
		
		// Write to blob file
		AppEngineFile file = 
				fileService.createNewBlobFile("image/bmp");
		boolean lock = true;
		FileWriteChannel writeChannel = 
				fileService.openWriteChannel(file, lock);
		writeChannel.write(ByteBuffer.wrap(image, 0, image.length));
		writeChannel.closeFinally();
		
		log.info("Returning image...");
		BlobKey blobKey = fileService.getBlobKey(file);
		blobstoreService.serve(blobKey, resp);
	}
	
	private byte[] generateImageData(int width, int height, String color) {
		final int PIXEL_SIZE = 32;
		ByteBuffer byteBuffer = ByteBuffer.allocate(width * height * PIXEL_SIZE); 
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				if ("RED".equalsIgnoreCase(color)) {
					// Little endian 
					byte[] data = {
						(byte) 0x00, // Blue
						(byte) 0x00, // Green
						(byte) 0xFF, // Red
						(byte) 0xFF, // Alpha 
					};
					byteBuffer.put(data);
				} else if ("GREEN".equalsIgnoreCase(color)) {
					// Little endian 
					byte[] data = {
						(byte) 0x00, // Blue
						(byte) 0xFF, // Green
						(byte) 0x00, // Red
						(byte) 0xFF, // Alpha 
					};
					byteBuffer.put(data);
				} else if ("BLUE".equalsIgnoreCase(color)) {
					// Little endian 
					byte[] data = {
						(byte) 0xFF, // Blue
						(byte) 0x00, // Green
						(byte) 0x00, // Red
						(byte) 0xFF, // Alpha 
					};
					byteBuffer.put(data);
				}  else { // random
					// Little endian 
					byte[] data = {
						(byte) ((int)Math.random() * 155), // Blue
						(byte) ((int)Math.random() * 155), // Green
						(byte) ((int)Math.random() * 255), // Red
						(byte) 0xFF, // Alpha 
					};
					byteBuffer.put(data);
				}
			}
		}
		log.info("Data created, size: " + byteBuffer.array().length);
		return byteBuffer.array();
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
		log.info("headerSize: " + headerSize);
		log.info("file size: " + fileSize);
				
		bitmapHeader.setAttribute(
				Attribute.FILE_SIZE, 
				ConvertUtility.integerToByteArray(fileSize, ByteOrder.LITTLE_ENDIAN));
		
		return bitmapHeader.dump();
	}
}


//byte[] data = {
//		(byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x7F, // Blue 
//		(byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x7F, // Green
//		(byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x7F, // Red 
//		(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x7F,	// White
//		
//		(byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0xFF, // Blue 
//		(byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0xFF, // Green
//		(byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, // Red 
//		(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF 	// White
//};