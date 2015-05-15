package com.lightpegasus.scheduler.web.controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.RequestHelpers;
import com.lightpegasus.scheduler.web.SchedulerApp;
import com.lightpegasus.scheduler.web.ThymeleafController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

/**
 * Displays user preferences if the user is signed in. Redirects to the root if not.
 */
public class UserPreferencesController extends ThymeleafController {
  @Override
  protected void doProcess(
      SchedulerApp.PathBuilder pathBuilder, WebContext context, TemplateEngine engine, Optional<User> loggedInUser,
      int genconYear) throws Exception {
    if (!loggedInUser.isPresent()) {
      context.getHttpServletResponse().sendRedirect("/");
      return;
    }

    Multimap<String, String> parameters =
        RequestHelpers.parameterMultimap(context.getHttpServletRequest());

    context.setVariable("src", Iterables.getFirst(parameters.get("src"), "/"));

    engine.process("userPreferences", context, context.getHttpServletResponse().getWriter());
  }
}
