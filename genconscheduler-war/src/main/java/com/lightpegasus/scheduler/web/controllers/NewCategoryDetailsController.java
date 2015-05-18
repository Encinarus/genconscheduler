package com.lightpegasus.scheduler.web.controllers;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.lightpegasus.scheduler.gencon.entity.GenconEventGroup;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.ThymeleafController;
import com.lightpegasus.scheduler.web.paths.PathBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.common.html.HtmlEscapers.htmlEscaper;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Created by alek on 5/12/15.
 */
public class NewCategoryDetailsController  extends ThymeleafController {

  private static ImmutableSet<String> newReleases = ImmutableSet.of(
      "Rattle, Battle, Grab the Loot",
      "Tides of Time",
      "Mage Wars Arena Battlegrounds Domination",
      "Master Fox",
      "Sapiens",
      "Pingo Pingo"
  );

  @Override
  protected void doProcess(PathBuilder pathBuilder, WebContext context, TemplateEngine engine,
                           Optional<User> loggedInUser, int genconYear) throws Exception {
    String requestURI = context.getHttpServletRequest().getRequestURI();
    List<String> splitUrl = Splitter.on("/").omitEmptyStrings().splitToList(requestURI);

    String category = Iterables.getLast(splitUrl);

    log.info("GenconYear: " + genconYear);
//    ImmutableList<GenconEventGroup> genconEventGroups =
//        new Queries().loadEventsForCategory(category, genconYear);
    List<GenconEventGroup> genconEventGroups = ofy().load().type(GenconEventGroup.class)
        .filter("eventTypeAbbreviation", category)
        .filter("year", genconYear)
        .list();
    log.info("Loaded " + genconEventGroups.size() + " groups");
    context.setVariable("category", category);
    context.setVariable("eventListHtml", createEventListHtml(pathBuilder, genconEventGroups));
    if (category.equals("BGM")) {
      context.setVariable("bannerText",
          "See BGG's list of <a href=\"https://boardgamegeek.com/geeklist/190570/gen-con-2015-releases/page/1\">GenCon 2015 releases</a>");
    }
    engine.process("newCategoryDetails", context, context.getHttpServletResponse().getWriter());
  }

  private String createEventListHtml(PathBuilder urls, List<GenconEventGroup> events) {
    StringBuilder html = new StringBuilder();

    log.info("Partitioning " + events.size() + " by rule system");
    List<RulesGroup> rulesGroups = partitionEvents(events);
    Collections.sort(rulesGroups, new Comparator<RulesGroup>() {
      @Override
      public int compare(RulesGroup o1, RulesGroup o2) {
        return o1.getName().compareToIgnoreCase(o2.getName());
      }
    });

    for (RulesGroup group : rulesGroups) {
      html.append("<h4>")
          .append(escape(group.getName()))
          .append(" - ")
          .append(group.getSearchResults().size())
          .append(" events");
      if (newReleases.contains(group.getName())) {
        html.append(" (GenCon 2015 release)");
      }

      html.append("</h4>")
          .append("<div class=\"list-group col-md-12\">"); // Begin group

      for (GenconEventGroup result : group.getSearchResults()) {
        buildSearchResult(urls, html, result);
      }
      html.append("</div>"); // End group
    }

    return html.toString();
  }

  private String escape(String input) {
    return htmlEscaper().escape(Strings.nullToEmpty(input));
  }

  private void buildSearchResult(PathBuilder urls, StringBuilder html, GenconEventGroup event) {
    html.append("<a href=\"")
        .append(urls.sitePath("event") + htmlEscaper().escape(event.getClusterId()))
        .append("\" ")
        .append("class=\"list-group-item\" style=\"font-size: small;\">");
    // Header
    html.append("<h4>");
    if (event.getSimilarEventCount() > 1) {
      html.append(" <span class=\"badge\">")
          .append(event.getSimilarEventCount())
          .append(" sessions</span> ");
    }
    html.append(escape(event.getTitle()));
    html.append(" <span class=\"small\">")
        .append(escape(event.getGameSystem()))
        .append(" ")
        .append(escape(event.getRulesEdition()))
        .append("</span>")
        .append("</h4>"); // end header
    html.append("<p>").append(escape(event.getShortDescription())).append("</p>");
    html.append("<ul class=\"list-inline\">");
    html.append("<li><strong>Wed</strong> ")
        .append(event.getWedAvailable())
        .append(" tickets / ")
        .append(event.getWedOpenEvents())
        .append(event.getWedOpenEvents() > 1 ? " sessions" : " session")
        .append("</li>");
    html.append("<li><strong>Thurs</strong> ")
        .append(event.getThursAvailable())
        .append(" tickets / ")
        .append(event.getThursOpenEvents())
        .append(event.getThursOpenEvents() > 1 ? " sessions" : " session")
        .append("</li>");
    html.append("<li><strong>Fri</strong> ")
        .append(event.getFriAvailable())
        .append(" tickets / ")
        .append(event.getFriOpenEvents())
        .append(event.getFriOpenEvents() > 1 ? " sessions" : " session")
        .append("</li>");
    html.append("<li><strong>Sat</strong> ")
        .append(event.getSatAvailable())
        .append(" tickets / ")
        .append(event.getSatOpenEvents())
        .append(event.getSatOpenEvents() > 1 ? " sessions" : " session")
        .append("</li>");
    html.append("<li><strong>Sun</strong> ")
        .append(event.getSunAvailable())
        .append(" tickets / ")
        .append(event.getSunOpenEvents())
        .append(event.getSunOpenEvents() > 1 ? " sessions" : " session")
        .append("</li>");
    html.append("</ul></a>");
  }

  private List<RulesGroup> partitionEvents(List<GenconEventGroup> events) {
    ListMultimap<String, GenconEventGroup> eventsByRules = ArrayListMultimap.create();

    log.info("Partitioning " + events.size() + " events");
    for (GenconEventGroup event : events) {
      String gameSystem = event.getGameSystem();
      if (Strings.isNullOrEmpty(gameSystem)) {
        gameSystem = "Unspecified";
      }
      eventsByRules.put(gameSystem, event);
    }

    List<RulesGroup> groups = new ArrayList<>();
    for (String ruleSystem : eventsByRules.keySet()) {
      groups.add(new RulesGroup(ruleSystem, eventsByRules.get(ruleSystem)));
    }

    return groups;
  }

  public static class RulesGroup {
    private final List<GenconEventGroup> searchResults;
    private final String name;

    public RulesGroup(String name, List<GenconEventGroup> events) {
      this.name = name;
      searchResults = events;
    }

    public String getName() {
      return name;
    }

    public List<GenconEventGroup> getSearchResults() {
      return searchResults;
    }
  }
}
