package fr.telecomParistech.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.api.images.Image;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.Transform;


public class ImageTransformerServlet extends HttpServlet {
	
	private static final long serialVersionUID = 738753237936169233L;
	private static final ImagesService imageService = ImagesServiceFactory.getImagesService();
	private static final FileService fileService = FileServiceFactory.getFileService();
	private static final BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
	private static final Logger log = 
			Logger.getLogger(ImageTransformerServlet.class.getName());
	static {
		log.setLevel(Level.INFO);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String senderUrl = req.getParameter("senderUrl");
		URL url = null;
		
		log.info("Readfile, SenderUrl: " + senderUrl);
		url = new URL(senderUrl);
		InputStream is = url.openStream();
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] data = new byte[4096];
		int len;
		while ((len = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, len);
		}
		buffer.flush();
		Image image = ImagesServiceFactory.makeImage(buffer.toByteArray());
		buffer.close();
		is.close();
		Transform transform;
		
		String transformType = req.getParameter("transform");
		log.info("Tranforming image...");
		if ("VERTICAL".equalsIgnoreCase(transformType)) {
			transform = ImagesServiceFactory.makeVerticalFlip();
		} else if ("HORIZONTAL".equalsIgnoreCase(transformType)) {
			transform = ImagesServiceFactory.makeHorizontalFlip();
		} else if ("ROTATE".equalsIgnoreCase(transformType)) {
			transform = ImagesServiceFactory.makeRotate(90);
		}  else { // default
			transform = ImagesServiceFactory.makeHorizontalFlip();;
		}
		
		Image newImage = imageService.applyTransform(transform, image);
		byte[] newImageData = newImage.getImageData();
		
		// Write to blob file
		AppEngineFile file = 
				fileService.createNewBlobFile("image/bmp");
		boolean lock = true;
		FileWriteChannel writeChannel = 
				fileService.openWriteChannel(file, lock);
		writeChannel.write(ByteBuffer.wrap(newImageData, 0, newImageData.length));
		writeChannel.closeFinally();
		
		log.info("Returning image...");
		BlobKey blobKey = fileService.getBlobKey(file);
		blobstoreService.serve(blobKey, resp);
		// Servle
	}
}
