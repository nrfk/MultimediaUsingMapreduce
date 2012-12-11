package fr.telecomParistech.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.tools.pipeline.JobInfo;
import com.google.appengine.tools.pipeline.NoSuchObjectException;
import com.google.appengine.tools.pipeline.PipelineService;
import com.google.appengine.tools.pipeline.PipelineServiceFactory;

public class TaskCheckerServlet extends HttpServlet {
	private static final long serialVersionUID = -1753950672446030106L;
	private static final boolean USE_BACKENDS = false;
	private final PipelineService pipelineService = 
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

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		System.out.println("************************************************");
		String pipelineId = req.getParameter("pipelineId");
		if (pipelineId == null) {
			throw new NullPointerException();
		}
		System.out.println(pipelineId);
		try {
			JobInfo jobInfo = pipelineService.getJobInfo(pipelineId);
			PrintWriter pw = resp.getWriter();
			pw.write(jobInfo.getJobState().toString());
			System.out.println(jobInfo.getJobState());
		} catch (NoSuchObjectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}
}
