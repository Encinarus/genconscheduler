package com.lightpegasus.scheduler.web.controllers;

import com.google.common.base.Optional;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.ThymeleafController;
import com.lightpegasus.scheduler.web.paths.PathBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

/**
 * Created by alek on 5/18/15.
 */
public class RedirectController extends ThymeleafController {

  private final String redirectPath;

  public RedirectController(String redirectPath) {
    this.redirectPath = redirectPath;
  }

  @Override
  protected void doProcess(PathBuilder pathBuilder, WebContext context,
                           TemplateEngine engine, Optional<User> loggedInUser,
                           int genconYear) throws Exception {
    context.getHttpServletResponse().sendRedirect(pathBuilder.sitePath(redirectPath));
  }
}
