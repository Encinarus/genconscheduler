package com.lightpegasus.scheduler.web;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.controllers.SearchController;
import org.joda.time.DateTimeComparator;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

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

    SchedulerApp.PathBuilder pathBuilder = new SchedulerApp.PathBuilder(2013);
    SchedulerApp.LocalPath localPath = pathBuilder.parseUrl(requestURI);

    context.setVariable("urls", pathBuilder);
    context.setVariable("year", localPath.year);
    User loggedInUser = null;

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
      loggedInUser = ofy().load().type(User.class).id(googleUser.getUserId()).now();

      if (loggedInUser == null) {
        loggedInUser = new User(googleUser.getUserId(),
            googleUser.getEmail(), googleUser.getNickname());
        ofy().save().entity(loggedInUser).now();
      }

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

    doProcess(context, engine, Optional.fromNullable(loggedInUser), localPath.year);
  }

  protected boolean requiresLogin() {
    return false;
  }

  protected boolean requiresAdmin() {
    return false;
  }

  protected abstract void doProcess(WebContext context, TemplateEngine engine,
      Optional<User> loggedInUser, int genconYear) throws Exception;

  protected static List<SearchResult> composeSearchResults(Collection<GenconEvent> foundEvents) {
    // Now cluster the events
    List<SearchResult> results = new ArrayList<>();
    Multimap<Long, GenconEvent> clusteredEvents = EventFilters.clusterEvents(foundEvents);
    for (Long clusterHash : clusteredEvents.keySet()) {
      Collection<GenconEvent> cluster = clusteredEvents.get(clusterHash);
      results.add(new SearchResult(cluster));
    }

    // Maybe keep the default sorting of the clusters?
//    Collections.sort(results, new ResultComparator());
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
