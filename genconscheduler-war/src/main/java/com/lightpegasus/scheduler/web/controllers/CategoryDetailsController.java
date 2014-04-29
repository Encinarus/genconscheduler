package com.lightpegasus.scheduler.web.controllers;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.ThymeleafController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.util.List;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Displays the details of a single category.
 */
public class CategoryDetailsController extends ThymeleafController {

  @Override
  protected void doProcess(WebContext context, TemplateEngine engine, Optional<User> loggedInUser) throws Exception {
    String requestURI = context.getHttpServletRequest().getRequestURI();
    List<String> splitUrl = Splitter.on("/").omitEmptyStrings().splitToList(requestURI);

    String category = Iterables.getLast(splitUrl);

    List<GenconEvent> categoryEvents = ofy().load().type(GenconEvent.class)
        .filter("eventTypeAbbreviation", category)
        .filter("year", 2013)
        .list();

    context.setVariable("results", composeSearchResults(categoryEvents));

    engine.process("categoryDetails", context, context.getHttpServletResponse().getWriter());
  }
}
