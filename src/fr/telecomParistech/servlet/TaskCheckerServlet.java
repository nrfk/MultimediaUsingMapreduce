package fr.telecomParistech.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.tools.pipeline.JobInfo;
import com.google.appengine.tools.pipeline.NoSuchObjectException;
import com.google.appengine.tools.pipeline.PipelineService;
import com.google.appengine.tools.pipeline.PipelineServiceFactory;

public class TaskCheckerServlet extends HttpServlet {
	private static final long serialVersionUID = -1753950672446030106L;
	private final PipelineService pipelineService =
			PipelineServiceFactory.newPipelineService();
	private static final DatastoreService datastoreService = 
			DatastoreServiceFactory.getDatastoreService();
	private static final FileService fileService = 
			FileServiceFactory.getFileService();
	private static final ImagesService imagesService = 
			ImagesServiceFactory.getImagesService();
	
	private static final Map<String,String> loadedImages = 
			new HashMap<String, String>();
	

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		String pipelineId = req.getParameter("pipelineId");
		String imageInfoKeyStr = req.getParameter("imageInfoKey");

		System.out.println("pipelineID: " + pipelineId);
		System.out.println("imageInfoKey: " + imageInfoKeyStr);
		
		if (pipelineId == null || imageInfoKeyStr == null) {
			throw new NullPointerException();
		}

		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("{ ");

		try {
			JobInfo jobInfo = pipelineService.getJobInfo(pipelineId);
			strBuilder.append(" status: '" + jobInfo.getJobState().toString() + 
					"',");
			System.out.println(jobInfo.getJobState());
		} catch (NoSuchObjectException e) {
			e.printStackTrace();
		} 

		// Get image list
		Key imageInfoKey = KeyFactory.stringToKey(imageInfoKeyStr);
		Entity imageInfoEntity = null;
		try {
			imageInfoEntity = datastoreService.get(imageInfoKey);
		} catch (EntityNotFoundException e1) {
			e1.printStackTrace();
		}

		// We save imageList whose type is List<String> in MPDParserServlet, 
		// so we can now safety cast Object --> List<String>
		@SuppressWarnings("unchecked")
		List<String> imagePathList = 
		(List<String>) imageInfoEntity.getProperty("imageList");

		strBuilder.append(" nImages: '" + imagePathList.size() + "' ,");
		strBuilder.append(" images: { ");
		for (int i = 0; i < imagePathList.size(); i++) {
			String imagePath = imagePathList.get(i);
			
			// If image has't already in cache, save it
			if (!loadedImages.containsKey(imagePath)) {
				BlobKey blobKey = fileService.getBlobKey(new AppEngineFile(imagePath));
				if (blobKey == null) { // Not available yet
					System.out.println("BlobKey " + i + " is not available yet");
					continue;
				}
				ServingUrlOptions servingUrlOptions =
						ServingUrlOptions.Builder.withBlobKey(blobKey);
				String imageFullUrl = imagesService.getServingUrl(servingUrlOptions);
				loadedImages.put(imagePath, imageFullUrl);
			}
			
			// Now, write to the result
			if (i != (imagePathList.size() - 1)) {
				strBuilder.append(" image"+ i+ ": '"+loadedImages.get(imagePath)+"' ,");
			} else { // Don't have comma at the end
				strBuilder.append(" image"+ i+ ": '"+loadedImages.get(imagePath)+"'");
			}
		}

		strBuilder.append(" }");
		strBuilder.append("}");
		PrintWriter pw = resp.getWriter();
		
		pw.write(strBuilder.toString());
	}
}