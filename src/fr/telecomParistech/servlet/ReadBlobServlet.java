package fr.telecomParistech.servlet;

import java.io.IOException;

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

public class ReadBlobServlet extends HttpServlet {
	private static final long serialVersionUID = -7957261535977201176L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		FileService fileService = FileServiceFactory.getFileService();
		BlobstoreService blobstoreService = 
				BlobstoreServiceFactory.getBlobstoreService();
		String blobPath = req.getParameter("blobPath");
		BlobKey blobKey = fileService.getBlobKey(new AppEngineFile(blobPath));
		if (blobKey == null) { // Not available yet
			return;
		}
		blobstoreService.serve(blobKey, resp);
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}
}
