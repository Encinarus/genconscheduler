package com.lightpegasus.scheduler.web.controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.lightpegasus.scheduler.gencon.entity.Gencon2013Event;
import com.lightpegasus.scheduler.gencon.entity.Queries;
import com.lightpegasus.scheduler.servlet.RequestHelpers;
import com.lightpegasus.thymeleaf.ThymeleafController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 */
public class EventDetailsController implements ThymeleafController {
  @Override
  public void process(WebContext context, TemplateEngine engine) throws Exception {
    Multimap<String, String> parameters =
        RequestHelpers.parameterMultimap(context.getHttpServletRequest());

    String eventId = Iterables.getFirst(parameters.get("eventId"), null);

    Gencon2013Event event = null;
    Collection<Gencon2013Event> relatedEvents = new ArrayList<>();
    if(eventId != null) {
      Queries queries = new Queries();
      event = queries.eventByEventId(eventId).orNull();

      if (event != null) {
        relatedEvents = queries.eventsForHash(event);
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
