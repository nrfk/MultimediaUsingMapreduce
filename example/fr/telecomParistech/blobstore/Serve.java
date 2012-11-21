package fr.telecomParistech.blobstore;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;

@SuppressWarnings("serial")
public class Serve extends HttpServlet {
	private BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		BlobKey blobKey = new BlobKey(req.getParameter("blob-key"));
		blobstoreService.serve(blobKey, resp);
	}
	

}
