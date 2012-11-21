package fr.telecomParistech.blobstore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
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
import com.google.appengine.api.files.FileReadChannel;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;

@SuppressWarnings("serial")
public class TestBlobStore extends HttpServlet {
	private static final Logger log = Logger.getLogger(TestBlobStore.class.getName());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		log.setLevel(Level.WARNING);
		// Get a file service
		FileService fileService = FileServiceFactory.getFileService();

		// Create a new Blob file
		AppEngineFile file = fileService.createNewBlobFile("text/plain");

		// Open channel to write to it
		boolean lock = false;
		FileWriteChannel writeChannel = fileService.openWriteChannel(file, lock);

		// Use PrintWriter to write indirectly to the channel
		PrintWriter out = new PrintWriter(Channels.newWriter(writeChannel, "UTF8"));
		out.println("The woods are lovely dark and deep.");
		out.println("But I have promises to keep.");

		// Close the print writer
		out.close();
		String path = file.getFullPath();
		log.warning("Path: " + path);
		// --------------------------------------------------------------


		// Write more, this time, directly to the writeChanel
		file = new AppEngineFile(path);
		log.warning("Blob File: " + file);
		
		// This time lock because we intend to finalize
		lock = true;
		writeChannel = fileService.openWriteChannel(file, lock);
		// This time we write to the channel directly
		writeChannel.write(ByteBuffer.wrap
				("And miles to go before I sleep.".getBytes()));

		// Now finalize
		writeChannel.closeFinally();

		
		// ***************************************************************
		// Now, read file, must have FileReadChannel
		lock = false;
		FileReadChannel readChannel = fileService.openReadChannel(file, false);
		
		// Using Java Standard
		BufferedReader reader = new BufferedReader(Channels.newReader(readChannel,"UTF8"));
		String line = reader.readLine();
		log.warning("BufferedReader, first line: " + line);
		
		readChannel.close();
		
		// Using BlobStoreApi
		BlobKey blobKey = fileService.getBlobKey(file);
		
		BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
		String segment = new String(blobstoreService.fetchData(blobKey, 30, 40));
		log.warning("Using Blob store, segment = " + segment);
	}


}



