package com.lightpegasus.scheduler.web.controllers;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.EventFilters;
import com.lightpegasus.scheduler.web.ThymeleafController;
import org.joda.time.DateTimeConstants;
import org.joda.time.Interval;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
    context.setVariable("calendarEvents", partitionEvents(starredEvents));

    engine.process("starredList", context, context.getHttpServletResponse().getWriter());
  }

  @Override
  protected boolean requiresLogin() {
    return true;
  }

  // TODO(alek): This should live somewhere else, probably at a top level
  // A calendar event is a semi-clustered event. Normally, we group events by their hash. In this
  // case, we want events to be clustered both by their hash and by overlapping times. So this
  // way TrueDungeon events will appear as a single event in the calendar, with annotations stating
  // which TrueDungeon event it actually is. One calendar event may have multiple instances.
  public static class CalendarEvent {
    public final String title;
    public final int startTimeSeconds;
    public final int endTimeSeconds;
    public final List<String> eventIds;
    public final String genconUrl;
    public final String shortCat;

    // Passed in events are assumed to overlap in time.
    public CalendarEvent(List<GenconEvent> events) {
      // get instead of getFirst because getFirst returns a nullable, which leads to a warning
      GenconEvent firstEvent = Iterables.get(events, 0);
      this.title = firstEvent.getTitle() +
          (events.size() > 1 ? " (" + events.size() + " events in group)": "");
      this.shortCat = firstEvent.getEventTypeAbbreviation();
      this.startTimeSeconds = (int) (firstEvent.getStartTime().getMillis() / 1000);
      this.endTimeSeconds = (int) (Iterables.getLast(events).getEndTime().getMillis() / 1000);

      ImmutableList.Builder<String> eventIds = ImmutableList.builder();
      for (GenconEvent event : events) {
        eventIds.add(event.getGameId());
      }
      this.eventIds = eventIds.build();

      Escaper escaper = UrlEscapers.urlFormParameterEscaper();
      this.genconUrl = "https://gencon.com/events/search?utf8=✓"
          // The gencon search doesn't like the !, so we remove it...
          + "&event_type=" + escaper.escape(firstEvent.getEventType().replace("!", ""))
          + "&title=" + escaper.escape(firstEvent.getTitle())
          + "&wed=" + (Boolean.toString(firstEvent.getDayOfWeek() == DateTimeConstants.WEDNESDAY))
          + "&thur=" + (Boolean.toString(firstEvent.getDayOfWeek() == DateTimeConstants.THURSDAY))
          + "&fri=" + (Boolean.toString(firstEvent.getDayOfWeek() == DateTimeConstants.FRIDAY))
          + "&sat=" + (Boolean.toString(firstEvent.getDayOfWeek() == DateTimeConstants.SATURDAY))
          + "&sun=" + (Boolean.toString(firstEvent.getDayOfWeek() == DateTimeConstants.SUNDAY));
    }

    public String getTitle() {
      return title;
    }

    public long getStartTimeSeconds() {
      return startTimeSeconds;
    }

    public long getEndTimeSeconds() {
      return endTimeSeconds;
    }

    public List<String> getEventIds() {
      return eventIds;
    }

    public String getGenconUrl() {
      return genconUrl;
    }

    public String getShortCat() {
      return shortCat;
    }

    public String toString() {
      return Objects.toStringHelper(this)
          .add("title", title)
          .add("startSeconds", startTimeSeconds)
          .add("endSeconds", endTimeSeconds)
          .add("eventIds", Joiner.on(", ").join(eventIds))
          .add("genconUrl", genconUrl)
          .toString();
    }
  }

  public List<CalendarEvent> partitionEvents(Collection<GenconEvent> eventsToCluster) {
    // First, cluster by hash
    Multimap<Long, GenconEvent> hashClusteredEvents = HashMultimap.create();

    for (GenconEvent event : eventsToCluster) {
      hashClusteredEvents.put(event.getClusterHash(), event);
    }

    List<CalendarEvent> calendarEvents = new ArrayList<>();
    // Now, cluster each into a different time
    for (Collection<GenconEvent> eventCluster : hashClusteredEvents.asMap().values()) {
      ArrayList<GenconEvent> sortedCluster = Lists.newArrayList(eventCluster);

      Collections.sort(sortedCluster, new Comparator<GenconEvent>() {
        @Override
        public int compare(GenconEvent firstEvent, GenconEvent secondEvent) {
          return firstEvent.getStartTime().compareTo(secondEvent.getStartTime());
        }
      });

      List<GenconEvent> timeCluster = new ArrayList<>();
      Interval clusterInterval = null;
      for (GenconEvent event : sortedCluster) {
        // Since the cluster is sorted, we just need to check if the start time is in the
        // existing cluster.
        if (clusterInterval == null || clusterInterval.contains(event.getStartTime())) {
          // Event is in the interval! Add to the list
          timeCluster.add(event);

          // Now adjust the interval as appropriate
          if (clusterInterval == null) {
            clusterInterval = new Interval(event.getStartTime(), event.getEndTime());
          }

          if (clusterInterval.getEnd().isBefore(event.getEndTime())) {
            clusterInterval = clusterInterval.withEnd(event.getEndTime());
          }
        } else {
          // New interval time!
          calendarEvents.add(new CalendarEvent(timeCluster));
          timeCluster = new ArrayList<>();
          clusterInterval = null;
        }
      }

      if (clusterInterval != null) {
        calendarEvents.add(new CalendarEvent(timeCluster));
      }
    }


    return calendarEvents;
  }
}
