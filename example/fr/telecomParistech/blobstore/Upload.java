package fr.telecomParistech.blobstore;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;


@SuppressWarnings("serial")
public class Upload extends HttpServlet{
	private BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		Map<String, BlobKey> blobs = blobstoreService.getUploadedBlobs(req);
		BlobKey blobKey = blobs.get("myFile");
		
		if (blobKey == null) {
			resp.sendRedirect("/");
		} else {
			resp.sendRedirect("/serve?blob-key=" + blobKey.getKeyString());
		}
	}
}
