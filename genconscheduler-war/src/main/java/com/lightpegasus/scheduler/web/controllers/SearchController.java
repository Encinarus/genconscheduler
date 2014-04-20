package com.lightpegasus.scheduler.web.controllers;

import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.Query;
import com.google.appengine.api.search.QueryOptions;
import com.google.appengine.api.search.Results;
import com.google.appengine.api.search.ScoredDocument;
import com.google.appengine.api.search.SearchServiceFactory;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.entity.SearchQuery;
import com.lightpegasus.scheduler.web.RequestHelpers;
import com.lightpegasus.thymeleaf.ThymeleafController;
import org.joda.time.DateTimeComparator;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Controller backing the search text box feature. This does not handle advanced search
 * which is it's own controller in it's own right.
 */
public class SearchController implements ThymeleafController {
  private static Logger log = Logger.getLogger(SearchController.class.getSimpleName());

  @Override
  public void process(WebContext context, TemplateEngine engine) throws Exception {
    Multimap<String, String> parameters =
        RequestHelpers.parameterMultimap(context.getHttpServletRequest());

    List<SearchResult> results = new ArrayList<>();

    String query = Iterables.getFirst(parameters.get("q"), null);

    // Default to searching for 2013 events if year isn't supplied
    String year = Iterables.getFirst(parameters.get("year"), "2013");

    IndexSpec indexSpec = IndexSpec.newBuilder().setName("events").build();
    Index index = SearchServiceFactory.getSearchService().getIndex(indexSpec);

    QueryOptions options = QueryOptions.newBuilder()
        .setLimit(1000)
        .setReturningIdsOnly(true)
        .setNumberFoundAccuracy(10000)
        .build();
    Results<ScoredDocument> documents = index.search(Query.newBuilder()
        .setOptions(options)
        .build(query + " year:" + year));

    if (documents.getNumberFound() > 0) {
      // Convert the documents to events
      Map<String, Integer> eventRanking = new HashMap<>();
      for (ScoredDocument doc : documents.getResults()) {
        eventRanking.put(doc.getId(), doc.getRank());
      }

      // Now cluster the events
      Multimap<Long, GenconEvent> clusteredEvents = HashMultimap.create();
      for (GenconEvent event :
          ofy().load().type(GenconEvent.class).ids(eventRanking.keySet()).values()) {
        clusteredEvents.put(event.getClusterHash(), event);
      }

      for (Long clusterHash : clusteredEvents.keySet()) {
        Collection<GenconEvent> cluster = clusteredEvents.get(clusterHash);
        @SuppressWarnings("ConstantConditions")
        int rank = eventRanking.get(Iterables.getFirst(cluster, null).getGameId());
        results.add(new SearchResult(cluster, rank));
      }
    }

    Collections.sort(results, new ResultComparator());

    // Save the search query and the result count for later inspection
    ofy().save().entities(new SearchQuery(query, results.size()));

    context.setVariable("query", query);
    context.setVariable("results", results);
    engine.process("searchResults", context, context.getHttpServletResponse().getWriter());
  }

  private static class ResultComparator implements Comparator<SearchResult> {
    @Override
    public int compare(SearchResult o1, SearchResult o2) {
      return Ordering.natural().reverse().compare(
          o1.getSimilarEventCount(), o2.getSimilarEventCount());
    }
  }

  public static class SearchResult {
    private final int rank;
    private List<GenconEvent> clusteredEvents;

    public SearchResult(Collection<GenconEvent> events, int rank) {
      // Assumes all events in the cluster have the same cluster hash
      ArrayList<GenconEvent> sortedEvents = Lists.newArrayList(events);
      Collections.sort(sortedEvents,
          new Comparator<GenconEvent>() {
            @Override
            public int compare(GenconEvent o1, GenconEvent o2) {
              return DateTimeComparator.getInstance().compare(o1.getStartTime(), o2.getStartTime());
            }
          });
      this.clusteredEvents = sortedEvents;
      this.rank = rank;
    }

    public GenconEvent getEvent() {
      return clusteredEvents.get(0);
    }

    public int getSimilarEventCount() {
      return clusteredEvents.size();
    }

    public int getRank() {
      return rank;
    }
  }
}
