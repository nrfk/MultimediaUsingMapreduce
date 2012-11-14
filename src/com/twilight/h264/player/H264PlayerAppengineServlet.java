package com.twilight.h264.player;

import java.io.IOException;
import javax.servlet.http.*;

@SuppressWarnings("serial")
public class H264PlayerAppengineServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/plain");
		resp.getWriter().println("Hello, world");
		H264Player h264Player = new H264Player();
		h264Player.playFile("resources/slamtv10.264");
	}
}
