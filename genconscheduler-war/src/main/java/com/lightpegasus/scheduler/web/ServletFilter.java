package com.lightpegasus.scheduler.web;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.common.base.Optional;
import com.lightpegasus.scheduler.gencon.Queries;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.paths.PathBuilder;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by alek on 5/17/15.
 */
public class ServletFilter implements Filter {
  private static Logger log = Logger.getLogger(ServletFilter.class.getSimpleName());

  static {
    log.info("Initializing Objectify.");
    Queries.initializeObjectify();
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
                       FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String uri = httpRequest.getRequestURI();

    RequestContext.Builder contextBuilder = new RequestContext.Builder();
    PathBuilder pathBuilder = new PathBuilder(uri);

    contextBuilder.setPathBuilder(pathBuilder);
    Queries queries = new Queries();
    contextBuilder.setQueries(queries);
    contextBuilder.setSyncStatus(queries.getSyncStatus(pathBuilder.getYear()));
    UserService userService = UserServiceFactory.getUserService();
    User loggedInUser = null;
    if (userService.isUserLoggedIn()) {
      com.google.appengine.api.users.User googleUser = userService.getCurrentUser();
      loggedInUser = new Queries().loadOrCreateUser(
          googleUser.getUserId(), googleUser.getEmail(), googleUser.getNickname());
      loggedInUser.setAdmin(userService.isUserAdmin());
    }
    contextBuilder.setLoggedInUser(Optional.fromNullable(loggedInUser));
    contextBuilder.setLoginUrl(userService.createLoginURL(uri));
    contextBuilder.setLogoutUrl(userService.createLogoutURL(uri));

    httpRequest.setAttribute("requestContext", contextBuilder.build());

    // Make sure this stays last, required to execute the servlet
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {

  }
}
