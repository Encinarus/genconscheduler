package com.lightpegasus.scheduler.web.controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.EventFilters;
import com.lightpegasus.scheduler.web.ThymeleafController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.util.List;

import static com.lightpegasus.scheduler.web.EventFilters.DayFilter.*;

/**
 * Presents a list of events for a user.
 */
public class UserStarredController extends ThymeleafController {
  @Override
  protected void doProcess(WebContext context, TemplateEngine engine,
      Optional<User> loggedInUser, int genconYear) throws Exception {

    List<GenconEvent> starredEvents = loggedInUser.get().getStarredEvents();

    context.setVariable("eventsByDay", EventFilters.eventsByDay(starredEvents));
    context.setVariable("eventsByCategory", EventFilters.eventsByCategory(starredEvents));

    engine.process("starredList", context, context.getHttpServletResponse().getWriter());
  }

  @Override
  protected boolean requiresLogin() {
    return true;
  }
}
