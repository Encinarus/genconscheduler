package com.lightpegasus.scheduler.web.controllers;

import com.lightpegasus.thymeleaf.ThymeleafController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

/**
 * Simply renders a template without needing to do any additional datastore reads.
 */
public class StaticTemplateController implements ThymeleafController {

  private final String templateName;

  public StaticTemplateController(String templateName) {
    this.templateName = templateName;
  }

  @Override
  public void process(WebContext context, TemplateEngine engine) throws Exception {
    engine.process(templateName, context, context.getHttpServletResponse().getWriter());
  }
}
