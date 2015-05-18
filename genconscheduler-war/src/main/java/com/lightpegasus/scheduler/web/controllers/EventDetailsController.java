package com.lightpegasus.scheduler.web.controllers;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.Queries;
import com.lightpegasus.scheduler.gencon.entity.GenconEventGroup;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.EventFilters;
import com.lightpegasus.scheduler.web.ThymeleafController;
import com.lightpegasus.scheduler.web.paths.PathBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EventDetailsController extends ThymeleafController {
  @Override
  public void doProcess(PathBuilder pathBuilder, WebContext context,
                        TemplateEngine engine, Optional<User> loggedInUser,
                        int genconYear) throws Exception {
    String requestURI = context.getHttpServletRequest().getRequestURI();
    List<String> splitUrl = Splitter.on("/").omitEmptyStrings().splitToList(requestURI);

    String eventId = Iterables.getLast(splitUrl);

    GenconEvent event = null;

    if(eventId != null) {
      Queries queries = new Queries();
      event = queries.loadGenconEvent(eventId, genconYear).orNull();

      if (event != null) {
        GenconEventGroup eventGroup = queries.loadGenconEventGroup(
            genconYear, event.getClusterHash());
        List<GenconEvent> relatedEvents = ImmutableList.copyOf(queries.loadSimilarEvents(event));

        context.setVariable("group", eventGroup);
        context.setVariable("event", event);
        context.setVariable("hasRelatedEvents", !relatedEvents.isEmpty());
        Multimap<String, GenconEvent> relatedEventsByDay = EventFilters.eventsByDay(relatedEvents);
        Multimap<String, String> relatedEventIdsByDay = idsForEventsByDay(relatedEventsByDay);
        context.setVariable("eventsByDay", relatedEventsByDay);
        context.setVariable("eventIdsByDay", relatedEventIdsByDay.asMap());
        context.setVariable("areAllStarred",
            loggedInUser.isPresent()
                && loggedInUser.get().getStarredEvents(genconYear).containsAll(relatedEvents));
      }
    }

    if (event == null) {
      engine.process("eventNotFound", context, context.getHttpServletResponse().getWriter());
    } else {
      engine.process("eventDetails", context, context.getHttpServletResponse().getWriter());
    }
  }

  private Multimap<String, String> idsForEventsByDay(
      Multimap<String, GenconEvent> relatedEventsByDay) {
    ImmutableMultimap.Builder<String, String> idsByDay = ImmutableMultimap.builder();
    for (String day : relatedEventsByDay.keySet()) {
      idsByDay.putAll(day, idsForEvents(relatedEventsByDay.get(day)));
    }

    return idsByDay.build();
  }

  // TODO(alek) dedupe this with other, similar methods throughout the app
  private List<String> idsForEvents(Collection<GenconEvent> relatedEvents) {
    List<String> ids = new ArrayList<>();
    for (GenconEvent event : relatedEvents) {
      ids.add(event.getGameId());
    }
    return ids;
  }
}
