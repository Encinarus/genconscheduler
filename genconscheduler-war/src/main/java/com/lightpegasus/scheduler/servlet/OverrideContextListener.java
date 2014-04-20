package com.lightpegasus.scheduler.servlet;

import ognl.OgnlRuntime;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 *
 */
public class OverrideContextListener implements ServletContextListener {
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    // This gets around a reflection problem with thymeleaf's templates. Sigh.
    // The problem only happens when we deploy to appengine, not locally.
    // http://stackoverflow.com/questions/22314530/illegalaccessexception-doing-thymeleaf-ognl-method-access
    OgnlRuntime.setSecurityManager(null);
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
  }
}
