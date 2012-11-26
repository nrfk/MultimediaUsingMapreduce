package fr.telecomParistech.servlet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.tools.pipeline.JobInfo;
import com.google.appengine.tools.pipeline.NoSuchObjectException;
import com.google.appengine.tools.pipeline.PipelineService;
import com.google.appengine.tools.pipeline.PipelineServiceFactory;

import fr.telecomParistech.dash.mpd.MPD;
import fr.telecomParistech.mapreduce.ImageExtractorPipeline;

public class ImageExtractorServlet extends HttpServlet {
	private static final long serialVersionUID = 6136470907483535456L;
	
	private static final Logger LOGGER = 
			Logger.getLogger(ImageExtractorServlet.class.getName());
	private static final PipelineService pipelineService = 
			PipelineServiceFactory.newPipelineService();

	/**
	 * Get UrlBase of the request
	 * @param req request to getUrl
	 * @return UrlBase
	 * @throws MalformedURLException
	 */
	private String getUrlBase(HttpServletRequest req) 
			throws MalformedURLException {
		URL requestUrl = new URL(req.getRequestURL().toString());
		String portString = 
				requestUrl.getPort() == -1 ? "" : ":" + requestUrl.getPort();
		return requestUrl.getProtocol() + 
				"://" + requestUrl.getHost() + portString + "/";
	}

	/**
	 * Get url of pipeline status of a pipeline process
	 * @param urlBase Urlbase of the request
	 * @param pipelineId pipeline id
	 * @return the url of pipeline status
	 */
	private String getPipelineStatusUrl(String urlBase, String pipelineId) {
		return urlBase + "_ah/pipeline/status.html?root=" + pipelineId;
	}

	/**
	 * Redirect to pipeline status
	 * @param req current request of the Servlet
	 * @param resp current response of the Servlet
	 * @param pipelineId pipelineId to redirect to
	 * @throws IOException
	 */
	private void redirectToPipelineStatus(HttpServletRequest req, 
			HttpServletResponse resp,
			String pipelineId) throws IOException {
		String destinationUrl = 
				getPipelineStatusUrl(getUrlBase(req), pipelineId);
		LOGGER.info("Redirecting to " + destinationUrl);
		resp.sendRedirect(destinationUrl);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		System.out.println("**************************");
		String mpdLocation = 
				"https://dl.dropbox.com/u/27889409/muma/sample-dash/sample-dash2/sample_dash.mpd";
		
		String pipelineId = pipelineService
				.startNewPipeline(new ImageExtractorPipeline(), mpdLocation);
		
		JobInfo jobInfo = null;
		try {
			jobInfo = pipelineService.getJobInfo(pipelineId);
		} catch (NoSuchObjectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (jobInfo == null) {
			return;
		}
		
		while (jobInfo.getJobState() != JobInfo.State.COMPLETED_SUCCESSFULLY) {
			try {
				jobInfo = pipelineService.getJobInfo(pipelineId);
			} catch (NoSuchObjectException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		System.out.println("******* : " + jobInfo.getJobState() );
		
		MPD mpd = (MPD) jobInfo.getOutput();
		while (mpd == null) {
			try {
				jobInfo = pipelineService.getJobInfo(pipelineId);
			} catch (NoSuchObjectException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mpd = (MPD) jobInfo.getOutput();
		}
		System.out.println(mpd.toString());
//		redirectToPipelineStatus(req, resp, pipelineId);
		
		
		
	}
}
