package fr.telecomParistech.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;

/**
 * When receives a request containing a fullpath of the blobstore, it tries to 
 * rest the blobstore content and return to the client.
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class BlobstoreReaderServlet extends HttpServlet {
	private static final long serialVersionUID = -7957261535977201176L;
	private static final ImagesService imagesService = 
			ImagesServiceFactory.getImagesService();
	private static final Set<String> processedImage = new HashSet<String>();
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		FileService fileService = FileServiceFactory.getFileService();
		BlobstoreService blobstoreService = 
				BlobstoreServiceFactory.getBlobstoreService();
		String blobPath = req.getParameter("blobPath");
		if (blobPath == null) {
			return;
		}
		
		
//		if (processedImage.contains(blobPath)) {
//			System.out.println("Already processed: " + blobPath);
//			return;
//		}
		
		BlobKey blobKey = fileService.getBlobKey(new AppEngineFile(blobPath));
		if (blobKey == null) { // Not available yet
			System.out.println("BlobKey is not available yet");
			return;
		}
		
		ServingUrlOptions servingUrlOptions =
				ServingUrlOptions.Builder.withBlobKey(blobKey);
		String imageFullUrl = imagesService.getServingUrl(servingUrlOptions);
		resp.addHeader("Cache-Control", "public");
		resp.setHeader("Content-Type", "text/plain");
		System.out.println("############################: " + imageFullUrl);
//		processedImage.add(blobPath);
//		System.out.println("Done: " + blobPath);
		
		PrintWriter pw = resp.getWriter();
		pw.println(imageFullUrl);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}
}
