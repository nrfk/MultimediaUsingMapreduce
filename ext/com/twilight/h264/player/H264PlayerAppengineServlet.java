package com.twilight.h264.player;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;

public class H264PlayerAppengineServlet extends HttpServlet {
	private static final long serialVersionUID = 6598441388531935173L;
	private static final Logger LOGGER = 
			Logger.getLogger(H264PlayerAppengineServlet.class.getName());
	private static final FileService fileService = 
			FileServiceFactory.getFileService();
	private static final BlobstoreService blobstoreService = 
			BlobstoreServiceFactory.getBlobstoreService();
	static {
		LOGGER.setLevel(Level.INFO);
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String url = req.getParameter("url");
		if (url == null) { // default
			url = "https://dl.dropbox.com/u/27889409/muma/sample-h264j/slamtv10.264";
		}
		LOGGER.info("url: " + url);
		
		String imageNo = req.getParameter("imageNo");
		if (imageNo == null) { // default
			imageNo = "200";
		}
		LOGGER.info("imageNo: " + imageNo);
		
		H264Player h264Player = new H264Player();
		String imageUrl = h264Player.playFile(url, Integer.parseInt(imageNo));
		LOGGER.info("imageUrl: " + imageUrl);
		
//		BlobKey blobKey = fileService.getBlobKey(new AppEngineFile(imageUrl));
//		blobstoreService.serve(blobKey, resp);
		LOGGER.info("imageUrl: " + imageUrl);
		PrintWriter pw = resp.getWriter();
		pw.println("imageUrl is: ");
		pw.println(imageUrl);
		pw.close();
	}
}
