package com.lightpegasus.scheduler.web.controllers;

import com.google.common.base.Optional;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.ThymeleafController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

/**
 * Provides many options for advanced search.
 */
public class AdvancedSearchController extends ThymeleafController {

  @Override
  protected void doProcess(WebContext context, TemplateEngine engine, Optional<User> loggedInUser, int genconYear) throws Exception {
    engine.process("advancedSearch", context, context.getHttpServletResponse().getWriter());
  }
}
