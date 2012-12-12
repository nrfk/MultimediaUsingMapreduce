package fr.telecomParistech.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This Servlet receives parsed information from MPDParserServlet and then, 
 * it uploads all media segments found in mpd file to Google App Engine
 * @author xuan-hoa.nguyen@telecom-paristech.fr
 *
 */
public class MPDModificatorServlet extends HttpServlet {
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		System.out.println("******************* MPDModificatorServlet");
	}
}
