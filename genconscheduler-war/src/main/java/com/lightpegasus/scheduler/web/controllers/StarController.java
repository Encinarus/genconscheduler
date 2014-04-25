package com.lightpegasus.scheduler.web.controllers;

import com.google.common.base.Optional;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.thymeleaf.ThymeleafController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

/**
 *
 */
public class StarController extends ThymeleafController {
  @Override
  protected void doProcess(WebContext context, TemplateEngine engine, Optional<User> loggedInUser) throws Exception {

  }
}
