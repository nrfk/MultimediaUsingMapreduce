package fr.telecomParistech.mapreduce;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.demos.mapreduce.entitycount.CountEntityServlet;
import com.google.appengine.tools.pipeline.JobInfo;
import com.google.appengine.tools.pipeline.NoSuchObjectException;
import com.google.appengine.tools.pipeline.PipelineService;
import com.google.appengine.tools.pipeline.PipelineServiceFactory;
import fr.telecomParistech.mapreduce.ParseMpdFileJob;

public class ConcatenateDashServlet extends HttpServlet {
	private static final long serialVersionUID = 1210065305362737339L;
	private static final Logger log = 
			Logger.getLogger(CountEntityServlet.class.getName());
	private static final String MDP_URL = "mdpUrl";
	
	private static PipelineService service = 
			PipelineServiceFactory.newPipelineService();
	
	private String getUrlBase(HttpServletRequest req) throws MalformedURLException {
		URL requestUrl = new URL(req.getRequestURL().toString());
		String portString = requestUrl.getPort() == -1 ? "" : ":" + requestUrl.getPort();
		return requestUrl.getProtocol() + "://" + requestUrl.getHost() + portString + "/";
	}

	private String getPipelineStatusUrl(String urlBase, String pipelineId) {
		return urlBase + "_ah/pipeline/status.html?root=" + pipelineId;
	}

	private void redirectToPipelineStatus(HttpServletRequest req, HttpServletResponse resp,
			String pipelineId) throws IOException {
		String destinationUrl = getPipelineStatusUrl(getUrlBase(req), pipelineId);
		log.info("Redirecting to " + destinationUrl);
		resp.sendRedirect(destinationUrl);
	}
	
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
//		String mdpUrl = req.getParameter(MDP_URL);
//		String pipelineId = 
//				service.startNewPipeline(new ParseMpdFileJob.DowloadMpdSubJob(), mdpUrl);
//		redirectToPipelineStatus(req, resp, pipelineId);
//		JobInfo jobInfo = null;
//		try {
//			jobInfo = service.getJobInfo(pipelineId);
//		} catch (NoSuchObjectException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		System.out.println(jobInfo);
	}
}
