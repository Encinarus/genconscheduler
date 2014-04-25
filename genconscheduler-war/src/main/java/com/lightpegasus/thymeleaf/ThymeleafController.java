package com.lightpegasus.thymeleaf;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.controllers.SearchController;
import org.joda.time.DateTimeComparator;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * ThymeleafController handles setting the login / logout urls.
 */
public abstract class ThymeleafController {
  protected Logger log = Logger.getLogger(getClass().getSimpleName());

  public final void process(WebContext context, TemplateEngine engine) throws Exception {
    UserService userService = UserServiceFactory.getUserService();

    String requestURI = context.getHttpServletRequest().getRequestURI();
    User loggedInUser = null;

    if (userService.isUserLoggedIn()) {
      context.setVariable("authText", "Sign out");
      context.setVariable("authUrl",
          userService.createLogoutURL(requestURI));
      context.setVariable("isAdmin", userService.isUserAdmin());

      com.google.appengine.api.users.User googleUser = userService.getCurrentUser();
      loggedInUser = ofy().load().type(User.class).id(googleUser.getUserId()).now();

      if (loggedInUser == null) {
        loggedInUser = new User(googleUser.getUserId(),
            googleUser.getEmail(), googleUser.getNickname());
        ofy().save().entity(loggedInUser).now();

        // At this point, we should send the user to see their preferences
        context.setVariable("goal_page", requestURI);
        context.setVariable("user", loggedInUser);

        context.getHttpServletResponse().sendRedirect(
            "/userPreferences?src=" + URLEncoder.encode(requestURI, "UTF-8"));
        return;
      }

      context.setVariable("user", loggedInUser);
    } else {
      context.setVariable("authText", "Sign in");
      context.setVariable("authUrl",
          userService.createLoginURL(requestURI));
      context.setVariable("isAdmin", false);
    }

    doProcess(context, engine, Optional.fromNullable(loggedInUser));
  }

  protected abstract void doProcess(WebContext context, TemplateEngine engine, Optional<User> loggedInUser)
      throws Exception;

  protected static List<SearchResult> composeSearchResults(Collection<GenconEvent> foundEvents) {
    // Now cluster the events
    List<SearchResult> results = new ArrayList<>();
    ImmutableMultimap.Builder<Long, GenconEvent> clusters = ImmutableMultimap.builder();

    for (GenconEvent event : foundEvents) {
      clusters.put(event.getClusterHash(), event);
    }

    Multimap<Long, GenconEvent> clusteredEvents = clusters.build();
    for (Long clusterHash : clusteredEvents.keySet()) {
      Collection<GenconEvent> cluster = clusteredEvents.get(clusterHash);
      results.add(new SearchResult(cluster));
    }

    Collections.sort(results, new ResultComparator());
    return results;
  }

  private static class ResultComparator implements Comparator<SearchController.SearchResult> {
    @Override
    public int compare(SearchController.SearchResult o1, SearchController.SearchResult o2) {
      return Ordering.natural().reverse().compare(
          o1.getSimilarEventCount(), o2.getSimilarEventCount());
    }
  }

  public static class SearchResult {
    private List<GenconEvent> clusteredEvents;

    public SearchResult(Collection<GenconEvent> events) {
      // Assumes all events in the cluster have the same cluster hash
      ArrayList<GenconEvent> sortedEvents = Lists.newArrayList(events);
      Collections.sort(sortedEvents,
          new Comparator<GenconEvent>() {
            @Override
            public int compare(GenconEvent o1, GenconEvent o2) {
              return DateTimeComparator.getInstance().compare(o1.getStartTime(), o2.getStartTime());
            }
          }
      );
      this.clusteredEvents = sortedEvents;
    }

    public GenconEvent getEvent() {
      return clusteredEvents.get(0);
    }

    public int getSimilarEventCount() {
      return clusteredEvents.size();
    }
  }
}
