package com.lightpegasus.scheduler.web.controllers;

import com.google.common.base.Optional;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.ThymeleafController;
import com.lightpegasus.scheduler.web.paths.PathBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

/**
 * Simply renders a template without needing to do any additional datastore reads.
 */
public class StaticTemplateController extends ThymeleafController {

  private final String templateName;

  public StaticTemplateController(String templateName) {
    this.templateName = templateName;
  }

  @Override
  public void doProcess(PathBuilder pathBuilder, WebContext context, TemplateEngine engine, Optional<User> loggedInUser,
                        int genconYear) throws Exception {
    engine.process(templateName, context, context.getHttpServletResponse().getWriter());
  }
}
