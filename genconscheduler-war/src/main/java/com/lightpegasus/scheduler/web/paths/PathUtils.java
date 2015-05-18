package com.lightpegasus.scheduler.web.paths;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by alek on 5/17/15.
 */
public class PathUtils {


  public static String getRequestPath(final HttpServletRequest request) {
    String requestURI = request.getRequestURI();
    final String contextPath = request.getContextPath();

    final int fragmentIndex = requestURI.indexOf(';');
    if (fragmentIndex != -1) {
      requestURI = requestURI.substring(0, fragmentIndex);
    }

    if (requestURI.startsWith(contextPath)) {
      return requestURI.substring(contextPath.length());
    }
    return requestURI;
  }
}
