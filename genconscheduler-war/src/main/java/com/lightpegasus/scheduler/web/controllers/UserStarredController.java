package com.lightpegasus.scheduler.web.controllers;

import com.google.common.base.Optional;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.ThymeleafController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.util.List;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Presents a list of events for a user.
 */
public class UserStarredController extends ThymeleafController {
  @Override
  protected void doProcess(WebContext context, TemplateEngine engine,
      Optional<User> loggedInUser, int genconYear) throws Exception {

    List<GenconEvent> starredEvents = loggedInUser.get().getStarredEvents();

    context.setVariable("results", composeSearchResults(starredEvents));

    engine.process("starredList", context, context.getHttpServletResponse().getWriter());
  }

  @Override
  protected boolean requiresLogin() {
    return true;
  }
}
