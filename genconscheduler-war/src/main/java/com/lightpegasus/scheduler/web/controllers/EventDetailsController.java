package com.lightpegasus.scheduler.web.controllers;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.Queries;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.ThymeleafController;
import org.joda.time.DateTimeConstants;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;

public class EventDetailsController extends ThymeleafController {
  @Override
  public void doProcess(WebContext context, TemplateEngine engine, Optional<User> loggedInUser,
      int genconYear) throws Exception {
    String requestURI = context.getHttpServletRequest().getRequestURI();
    List<String> splitUrl = Splitter.on("/").omitEmptyStrings().splitToList(requestURI);

    String eventId = Iterables.getLast(splitUrl);

    GenconEvent event = null;

    if(eventId != null) {
      Queries queries = new Queries();
      event = queries.loadGenconEvent(eventId, genconYear).orNull();

      if (event != null) {
        context.setVariable("event", event);

        List<GenconEvent> relatedEvents = ImmutableList.copyOf(
            Iterables.filter(queries.loadSimilarEvents(event), not(equalTo(event))));

        context.setVariable("hasRelatedEvents", !relatedEvents.isEmpty());

        context.setVariable("thursdayRelated",
            Iterables.filter(relatedEvents, isThursday()));
        context.setVariable("fridayRelated",
            Iterables.filter(relatedEvents, isFriday()));
        context.setVariable("saturdayRelated",
            Iterables.filter(relatedEvents, isSaturday()));
        context.setVariable("sundayRelated",
            Iterables.filter(relatedEvents, isSunday()));
      }
    }

    if (event == null) {
      engine.process("eventNotFound", context, context.getHttpServletResponse().getWriter());
    } else {
      engine.process("eventDetails", context, context.getHttpServletResponse().getWriter());
    }
  }

  private static Predicate<GenconEvent> isThursday() {
    return matchesDay(DateTimeConstants.THURSDAY);
  }

  private static Predicate<GenconEvent> isFriday() {
    return matchesDay(DateTimeConstants.FRIDAY);
  }

  private static Predicate<GenconEvent> isSaturday() {
    return matchesDay(DateTimeConstants.SATURDAY);
  }

  private static Predicate<GenconEvent> isSunday() {
    return matchesDay(DateTimeConstants.SUNDAY);
  }

  private static Predicate<GenconEvent> matchesDay(final int dayOfWeek) {
    return new Predicate<GenconEvent>() {
      @Override public boolean apply(GenconEvent input) {
        return input.getDayOfWeek() == dayOfWeek;
      }
    };
  }
}
