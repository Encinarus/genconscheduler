package com.lightpegasus.scheduler.servlet;

import com.lightpegasus.scheduler.web.SchedulerApp;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by alek on 4/6/14.
 */
public class OnStartupServlet extends HttpServlet {
  private static Logger log = Logger.getLogger(OnStartupServlet.class.getSimpleName());

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    // In order to get here, the SchedulerApp needed to be called, which does all the real
    // initialization. This is just a place holder.
    log.info("OnStartupServlet got a request? How... odd.");
    resp.setContentType("text/plain");
    resp.getWriter().println("Leave now. You shouldn't be here. \n\n");
  }
}
