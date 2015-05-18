package com.lightpegasus.scheduler.web.controllers;

import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.SearchServiceFactory;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import com.lightpegasus.scheduler.gencon.entity.GenconCategory;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.RequestHelpers;
import com.lightpegasus.scheduler.web.ThymeleafController;
import com.lightpegasus.scheduler.web.paths.PathBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 *
 */
public class DeleteGenconYearController extends ThymeleafController {
  private static Logger logger = Logger.getLogger(DeleteGenconYearController.class.getSimpleName());

  @Override
  public void doProcess(PathBuilder pathBuilder, WebContext context, TemplateEngine engine, Optional<User> loggedInUser,
                        int genconYear) throws Exception {
    int totalEventsDeleted = 0;

    IndexSpec indexSpec = IndexSpec.newBuilder().setName("events").build();
    Index index = SearchServiceFactory.getSearchService().getIndex(indexSpec);

    Multimap<String, String> parameters = RequestHelpers.parameterMultimap(
        context.getHttpServletRequest());

    boolean purgeAll = Boolean.valueOf(Iterables.getFirst(parameters.get("purgeAll"), "false"));

    List<Key<GenconEvent>> eventKeys = getEventKeys(genconYear, purgeAll);
    while (!eventKeys.isEmpty()) {
      ofy().delete().keys(eventKeys);

      List<String> docIds = new ArrayList<>(eventKeys.size());
      for (Key<GenconEvent> key : eventKeys) {
        docIds.add(key.getName());
      }
      index.delete(docIds);

      logger.info("Deleted a batch of events: " + eventKeys.size());
      totalEventsDeleted += eventKeys.size();

      eventKeys = getEventKeys(genconYear, purgeAll);
    }

    // There's only ~20 categories, safe to load all keys
    ofy().delete().keys(getCategoryKeys(genconYear, purgeAll));
    context.getHttpServletResponse().setContentType("text/plain");
    context.getHttpServletResponse().getWriter().println(totalEventsDeleted + " Events Deleted");
    context.getHttpServletResponse().getWriter().println("All Categories Deleted");
  }

  public List<Key<GenconCategory>> getCategoryKeys(long genconYear, boolean purgeAll) {
    Query<GenconCategory> query = ofy().load().type(GenconCategory.class);

    if (!purgeAll) {
      query = query.filter("year", genconYear);
    }

    return query.keys().list();
  }

  public List<Key<GenconEvent>> getEventKeys(long genconYear, boolean purgeAll) {
    Query<GenconEvent> query = ofy().load().type(GenconEvent.class);

    if (!purgeAll) {
      query = query.filter("year", genconYear);
    }
    return query.limit(200).keys().list();
  }
}
