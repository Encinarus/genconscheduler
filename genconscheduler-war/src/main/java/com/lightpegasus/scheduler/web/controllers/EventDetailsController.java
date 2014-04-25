package com.lightpegasus.scheduler.web.controllers;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.Queries;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.thymeleaf.ThymeleafController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EventDetailsController extends ThymeleafController {
  @Override
  public void doProcess(WebContext context, TemplateEngine engine, Optional<User> loggedInUser) throws Exception {
    String requestURI = context.getHttpServletRequest().getRequestURI();
    List<String> splitUrl = Splitter.on("/").omitEmptyStrings().splitToList(requestURI);

    String eventId = Iterables.getLast(splitUrl);

    GenconEvent event = null;
    Collection<GenconEvent> relatedEvents = new ArrayList<>();
    if(eventId != null) {
      Queries queries = new Queries();
      event = queries.loadGencon2013Event(eventId).orNull();

      if (event != null) {
        relatedEvents = queries.loadSimilarGencon2013Events(event);
      }
    }

    if (event == null) {
      engine.process("eventNotFound", context, context.getHttpServletResponse().getWriter());
    } else {
      context.setVariable("event", event);
      context.setVariable("related", relatedEvents);
      engine.process("eventDetails", context, context.getHttpServletResponse().getWriter());
    }
  }
}
