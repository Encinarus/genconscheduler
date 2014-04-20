package com.lightpegasus.scheduler.web.controllers;

import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.SearchServiceFactory;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.cmd.QueryKeys;
import com.lightpegasus.scheduler.gencon.entity.GenconCategory;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.web.RequestHelpers;
import com.lightpegasus.thymeleaf.ThymeleafController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 *
 */
public class DeleteGenconYearController implements ThymeleafController {
  private static Logger logger = Logger.getLogger(DeleteGenconYearController.class.getSimpleName());

  @Override
  public void process(WebContext context, TemplateEngine engine) throws Exception {
    int totalEventsDeleted = 0;

    IndexSpec indexSpec = IndexSpec.newBuilder().setName("events").build();
    Index index = SearchServiceFactory.getSearchService().getIndex(indexSpec);

    Multimap<String, String> parameters = RequestHelpers.parameterMultimap(
        context.getHttpServletRequest());

    long year = Long.parseLong(Iterables.getFirst(parameters.get("year"), "2013"));
    boolean purgeAll = Boolean.valueOf(Iterables.getFirst(parameters.get("purgeAll"), "false"));

    List<Key<GenconEvent>> eventKeys = getEventKeys(year, purgeAll);
    while (!eventKeys.isEmpty()) {
      ofy().delete().keys(eventKeys);

      List<String> docIds = new ArrayList<>(eventKeys.size());
      for (Key<GenconEvent> key : eventKeys) {
        docIds.add(key.getName());
      }
      index.delete(docIds);

      logger.info("Deleted a batch of events: " + eventKeys.size());
      totalEventsDeleted += eventKeys.size();

      eventKeys = getEventKeys(year, purgeAll);
    }

    // There's only ~20 categories, safe to load all keys
    ofy().delete().keys(getCategoryKeys(year, purgeAll));
    context.getHttpServletResponse().setContentType("text/plain");
    context.getHttpServletResponse().getWriter().println(totalEventsDeleted + " Events Deleted");
    context.getHttpServletResponse().getWriter().println("All Categories Deleted");
  }

  public List<Key<GenconCategory>> getCategoryKeys(long year, boolean purgeAll) {
    Query<GenconCategory> query = ofy().load().type(GenconCategory.class);

    if (!purgeAll) {
      query = query.filter("year", year);
    }

    return query.keys().list();
  }

  public List<Key<GenconEvent>> getEventKeys(long year, boolean purgeAll) {
    Query<GenconEvent> query = ofy().load().type(GenconEvent.class);

    if (!purgeAll) {
      query = query.filter("year", year);
    }
    return query.limit(200).keys().list();
  }
}
