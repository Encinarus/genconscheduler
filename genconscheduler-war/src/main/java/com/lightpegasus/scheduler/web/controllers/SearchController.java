package com.lightpegasus.scheduler.web.controllers;

import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.Query;
import com.google.appengine.api.search.QueryOptions;
import com.google.appengine.api.search.Results;
import com.google.appengine.api.search.ScoredDocument;
import com.google.appengine.api.search.SearchServiceFactory;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.entity.SearchQuery;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.RequestHelpers;
import com.lightpegasus.scheduler.web.ThymeleafController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Controller backing the search text box feature. This does not handle advanced search
 * which is it's own controller in it's own right.
 */
public class SearchController extends ThymeleafController {
  private static Logger log = Logger.getLogger(SearchController.class.getSimpleName());

  @Override
  public void doProcess(WebContext context, TemplateEngine engine, Optional<User> loggedInUser,
      int genconYear) throws Exception {
    Multimap<String, String> parameters =
        RequestHelpers.parameterMultimap(context.getHttpServletRequest());

    String query = Iterables.getFirst(parameters.get("q"), null);

    IndexSpec indexSpec = IndexSpec.newBuilder().setName("events").build();
    Index index = SearchServiceFactory.getSearchService().getIndex(indexSpec);

    QueryOptions options = QueryOptions.newBuilder()
        .setLimit(1000)
        .setReturningIdsOnly(true)
        .setNumberFoundAccuracy(10000)
        .build();
    Results<ScoredDocument> documents = index.search(Query.newBuilder()
        .setOptions(options)
        .build(query + " year:" + genconYear));

    Collection<GenconEvent> foundEvents = ImmutableList.of();

    if (documents.getNumberFound() > 0) {
      // Convert the documents to events
      Set<String> foundIds = new HashSet<>();
      for (ScoredDocument doc : documents.getResults()) {
        foundIds.add(doc.getId());
      }

      foundEvents = ofy().load().type(GenconEvent.class).ids(foundIds).values();
    }

    List<SearchResult> results = composeSearchResults(foundEvents);

    // Save the search query and the result count for later inspection
    ofy().save().entities(new SearchQuery(query, results.size()));

    context.setVariable("query", query);
    context.setVariable("results", results);
    engine.process("searchResults", context, context.getHttpServletResponse().getWriter());
  }

}
