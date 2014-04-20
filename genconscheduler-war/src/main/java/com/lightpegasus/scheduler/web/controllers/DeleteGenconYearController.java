package com.lightpegasus.scheduler.web.controllers;

import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.SearchServiceFactory;
import com.google.common.collect.Iterables;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.QueryKeys;
import com.lightpegasus.scheduler.gencon.entity.GenconCategory;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
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

    List<Key<GenconEvent>> eventKeys = ofy().load().type(GenconEvent.class)
        .filter("year", 2013).limit(200).keys().list();
    while (!eventKeys.isEmpty()) {
      ofy().delete().keys(eventKeys);

      List<String> docIds = new ArrayList<>(eventKeys.size());
      for (Key<GenconEvent> key : eventKeys) {
        docIds.add(key.getName());
      }
      index.delete(docIds);

      logger.info("Deleted a batch of events: " + eventKeys.size());
      totalEventsDeleted += eventKeys.size();

      eventKeys = ofy().load().type(GenconEvent.class)
          .filter("year", 2013).limit(200).keys().list();
    }

    // There's only ~20 categories, safe to load all keys
    ofy().delete().keys(
        ofy().load().type(GenconCategory.class).keys().list());
    context.getHttpServletResponse().setContentType("text/plain");
    context.getHttpServletResponse().getWriter().println(totalEventsDeleted + " Events Deleted");
    context.getHttpServletResponse().getWriter().println("All Categories Deleted");
  }
}
