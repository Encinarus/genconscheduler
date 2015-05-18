package com.lightpegasus.scheduler.web;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.lightpegasus.scheduler.gencon.Queries;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.paths.LocalPath;
import com.lightpegasus.scheduler.web.paths.PathBuilder;
import com.lightpegasus.scheduler.web.paths.PlannerPaths;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * ThymeleafController handles setting the login / logout urls.
 */
public abstract class ThymeleafController {
  private static Logger staticLog = Logger.getLogger(ThymeleafController.class.getSimpleName());
  protected Logger log = Logger.getLogger(getClass().getSimpleName());

  public final void process(WebContext context, TemplateEngine engine) throws Exception {
    UserService userService = UserServiceFactory.getUserService();

    String requestURI = context.getHttpServletRequest().getRequestURI();

    // We default to 2015 for the year
    PathBuilder pathBuilder = new PathBuilder(requestURI);
    LocalPath localPath = pathBuilder.getLocalPath();
    // Then update the path builder based on the parsed year
    log.info("Parsed year: " + localPath.getYear());

    context.setVariable("urls", pathBuilder);
    context.setVariable("year", localPath.getYear());
    User loggedInUser = null;

    context.setVariable("syncStatus", new Queries().getSyncStatus(localPath.getYear()));

    if (userService.isUserLoggedIn()) {
      context.setVariable("authText", "Sign out");
      context.setVariable("authUrl",
          userService.createLogoutURL(requestURI));
      context.setVariable("isAdmin", userService.isUserAdmin());

      if (requiresAdmin() && !userService.isUserAdmin()) {
        // Short circuit if we need to be an admin and we're not.
        context.getHttpServletResponse().sendRedirect(pathBuilder.sitePath("notAuthorized"));
        return;
      }

      com.google.appengine.api.users.User googleUser = userService.getCurrentUser();
      loggedInUser = new Queries().loadOrCreateUser(
          googleUser.getUserId(), googleUser.getEmail(), googleUser.getNickname());

      context.setVariable("user", loggedInUser);
    } else if (requiresLogin() || requiresAdmin()) {
      context.getHttpServletResponse().sendRedirect(userService.createLoginURL(requestURI));
    } else {
      context.setVariable("user", null);
      context.setVariable("authText", "Sign in");
      context.setVariable("authUrl",
          userService.createLoginURL(requestURI));
      context.setVariable("isAdmin", false);
    }

    doProcess(pathBuilder, context, engine, Optional.fromNullable(loggedInUser), localPath.getYear());
  }

  protected boolean requiresLogin() {
    return false;
  }

  protected boolean requiresAdmin() {
    return false;
  }

  protected abstract void doProcess(PathBuilder pathBuilder, WebContext context, TemplateEngine engine,
                                    Optional<User> loggedInUser, int genconYear) throws Exception;

  protected static List<SearchResult> composeSearchResults(Collection<GenconEvent> foundEvents) {
    // Now cluster the events
    List<SearchResult> results = new ArrayList<>();
    staticLog.info("Clustering " + foundEvents.size() + "events");
    Multimap<Long, GenconEvent> clusteredEvents = EventFilters.clusterEvents(foundEvents);
    for (Long clusterHash : clusteredEvents.keySet()) {
      Collection<GenconEvent> cluster = clusteredEvents.get(clusterHash);
      results.add(new SearchResult(cluster));
    }

    return results;
  }

  public static class SearchResult {
    private static final Logger log = Logger.getLogger(SearchResult.class.getSimpleName());
    private final Map<String, Integer> availableByDay;
    private final Map<String, Integer> openEventCounts;
    private final List<GenconEvent> clusteredEvents;
    private final int totalTickets;
    private final int availableTickets;
    private final int largestGroup;

    public SearchResult(Collection<GenconEvent> events) {
      // Assumes all events in the cluster have the same cluster hash
      this.clusteredEvents = EventFilters.sortByStartTime(events);

      Map<String, Integer> dailyAvailability = new HashMap<>();
      Map<String, Integer> openEventsPerDay = new HashMap<>();
      ImmutableMultimap<String, GenconEvent> eventsByDay =
          EventFilters.eventsByDay(clusteredEvents);
      int tickets = 0;
      int available = 0;
      int largestGroup = 0;

      for (String day : eventsByDay.keySet()) {
        int runningTotalAvailable = 0;
        int openEvents = 0;

        ImmutableCollection<GenconEvent> dayEvents = eventsByDay.get(day);
        for (GenconEvent event : dayEvents) {
          largestGroup = Math.max(largestGroup, event.getTicketsAvailable());
          runningTotalAvailable = event.getTicketsAvailable();

          available += event.getTicketsAvailable();
          tickets += event.getMaximumPlayers();
          if (event.getTicketsAvailable() > 0) {
            openEvents++;
          }
        }

        dailyAvailability.put(day, runningTotalAvailable);
        openEventsPerDay.put(day, openEvents);
      }

      this.availableTickets = available;
      this.totalTickets = tickets;
      this.largestGroup = largestGroup;
      this.availableByDay = dailyAvailability;
      this.openEventCounts = openEventsPerDay;
    }

    public GenconEvent getEvent() {
      return clusteredEvents.get(0);
    }

    public int getLargestGroup() {
      return largestGroup;
    }

    public int getSimilarEventCount() {
      return clusteredEvents.size();
    }

    public int getAvailableTickets() {
      return availableTickets;
    }

    public int getTotalTickets() {
      return totalTickets;
    }

    public int getWedAvailable() {
      return Objects.firstNonNull(availableByDay.get("Wednesday"), 0);
    }

    public int getThursAvailable() {
      return Objects.firstNonNull(availableByDay.get("Thursday"), 0);
    }

    public int getFriAvailable() {
      return Objects.firstNonNull(availableByDay.get("Friday"), 0);
    }

    public int getSatAvailable() {
      return Objects.firstNonNull(availableByDay.get("Saturday"), 0);
    }

    public int getSunAvailable() {
      return Objects.firstNonNull(availableByDay.get("Sunday"), 0);
    }

    public int getWedOpenEvents() {
      return Objects.firstNonNull(openEventCounts.get("Wednesday"), 0);
    }

    public int getThursOpenEvents() {
      return Objects.firstNonNull(openEventCounts.get("Thursday"), 0);
    }

    public int getFriOpenEvents() {
      return Objects.firstNonNull(openEventCounts.get("Friday"), 0);
    }

    public int getSatOpenEvents() {
      return Objects.firstNonNull(openEventCounts.get("Saturday"), 0);
    }

    public int getSunOpenEvents() {
      return Objects.firstNonNull(openEventCounts.get("Sunday"), 0);
    }
  }
}
