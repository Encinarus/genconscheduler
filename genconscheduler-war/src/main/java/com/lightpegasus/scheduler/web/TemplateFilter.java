package com.lightpegasus.scheduler.web;

import org.thymeleaf.context.WebContext;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Filter which directs certain calls to the appropriate controller.
 */
public class TemplateFilter implements Filter {
  private static Logger log = Logger.getLogger(TemplateFilter.class.getSimpleName());

  private ServletContext servletContext;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    this.servletContext = filterConfig.getServletContext();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    if (!process(httpRequest, (HttpServletResponse)response)) {
      log.info("Passing on request for " + httpRequest.getRequestURL());
      chain.doFilter(request, response);
    }
  }

  private boolean process(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    WebContext context = new WebContext(request, response, servletContext, request.getLocale());

    ThymeleafController controller = SchedulerApp.resolveControllerForRequest(request);
    if (controller == null) {
      return false;
    }

    log.info("Handling request for " + request.getRequestURL()
        + " through " + controller.getClass().getSimpleName());

    try {
      controller.process(context, SchedulerApp.getTemplateEngine());
    } catch(IOException | ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }

    return true;
  }

  @Override
  public void destroy() {

  }
}
