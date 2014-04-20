package com.lightpegasus.scheduler.servlet;

import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.SearchServiceFactory;
import com.google.apphosting.api.search.DocumentPb;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.lightpegasus.scheduler.gencon.GenconSchedulerParser;
import com.lightpegasus.scheduler.gencon.entity.Gencon2013Category;
import com.lightpegasus.scheduler.gencon.entity.Gencon2013Event;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Parses the events and fills the datastore with them.
 */
public class EventParserServlet extends HttpServlet {
  private static Logger log = Logger.getLogger(EventParserServlet.class.getSimpleName());

  private static Multimap<String, String> eventTypeKeywordMap = HashMultimap.create();

  static {
    eventTypeKeywordMap.putAll("magic: the gathering", ImmutableList.of("mtg", "wizards of the coast", "wotc"));
    eventTypeKeywordMap.putAll("magic", ImmutableList.of("mtg", "magic the gathering", "wizards of the coast", "wotc"));

    eventTypeKeywordMap.putAll("a game of thrones", ImmutableList.of("got", "song of ice & fire"));
    eventTypeKeywordMap.putAll("a game of thrones: the board game", ImmutableList.of("got", "song of ice & fire"));
    eventTypeKeywordMap.putAll("song of ice & fire", ImmutableList.of("got", "song of ice & fire", "a game of thrones"));

    eventTypeKeywordMap.putAll("babylon 5: a call to arms", ImmutableList.of("b5"));
    eventTypeKeywordMap.putAll("cards against humanity", ImmutableList.of("cah"));
    // catan!!!
    eventTypeKeywordMap.putAll("catan: traders and barbarians", ImmutableList.of("settlers of catan"));
    eventTypeKeywordMap.putAll("catan dice game", ImmutableList.of("settlers of catan"));
    eventTypeKeywordMap.putAll("catan histories: merchants of europe", ImmutableList.of("settlers of catan"));
    eventTypeKeywordMap.putAll("catan junior", ImmutableList.of("settlers of catan"));
    eventTypeKeywordMap.putAll("catan: explorers and pirates", ImmutableList.of("settlers of catan"));
    eventTypeKeywordMap.putAll("catan: seafarers", ImmutableList.of("settlers of catan"));
    eventTypeKeywordMap.putAll("catan: traders and barbarians", ImmutableList.of("settlers of catan"));

    // D&D
    eventTypeKeywordMap.putAll("d&d gamma world roleplaying game", ImmutableList.of("d&d", "dungeons and dragons", "rpg", "d20", "wizards of the coast", "wotc"));
    eventTypeKeywordMap.putAll("d&d miniatures", ImmutableList.of("d&d", "dungeons and dragons", "rpg", "d20", "wizards of the coast", "wotc"));
    eventTypeKeywordMap.putAll("dungeon & dragons", ImmutableList.of("d&d", "dungeons and dragons", "rpg", "d20", "wizards of the coast", "wotc"));
    eventTypeKeywordMap.putAll("dungeons & dragons", ImmutableList.of("d&d", "dungeons and dragons", "rpg", "d20", "wizards of the coast", "wotc"));

    eventTypeKeywordMap.putAll("kobolds ate my baby!", ImmutableList.of("kamb"));
    eventTypeKeywordMap.putAll("legend of the five rings", ImmutableList.of("l5r"));

    eventTypeKeywordMap.putAll("lord of the rings", ImmutableList.of("lotr"));
    eventTypeKeywordMap.putAll("the hobbit card game", ImmutableList.of("lotr", "lord of the rings"));
    eventTypeKeywordMap.putAll("the hobbit: the defeat of smaug", ImmutableList.of("lotr", "lord of the rings"));
    eventTypeKeywordMap.putAll("lord of the rings tradeable miniatures", ImmutableList.of("lotr"));
    eventTypeKeywordMap.putAll("lord of the rings: the card game", ImmutableList.of("lotr"));
    eventTypeKeywordMap.putAll("the lord of the rings: the battle for middle-earth", ImmutableList.of("lotr"));
    eventTypeKeywordMap.putAll("the lord of the rings: the card game", ImmutableList.of("lotr"));
    eventTypeKeywordMap.putAll("lord of the rings: the fellowship of the ring dbg", ImmutableList.of("lotr"));
    eventTypeKeywordMap.putAll("middle-earth role playing", ImmutableList.of("lotr", "middle earth"));
    eventTypeKeywordMap.putAll("meccg", ImmutableList.of("middle", "earth", "ccg", "collectable card game"));

    eventTypeKeywordMap.putAll("pathfinder", ImmutableList.of("paizo", "dungeons and dragons", "d&d"));
    eventTypeKeywordMap.putAll("pathfinder roleplaying  game", ImmutableList.of("paizo", "dungeons and dragons", "d&d"));
    eventTypeKeywordMap.putAll("pathfinder roleplaying game", ImmutableList.of("paizo", "dungeons and dragons", "d&d"));
    eventTypeKeywordMap.putAll("spacehulk", ImmutableList.of("space hulk"));
    eventTypeKeywordMap.putAll("space hulk", ImmutableList.of("spacehulk"));
    eventTypeKeywordMap.putAll("star trek: deep space nine", ImmutableList.of("ds9", "9"));

    eventTypeKeywordMap.putAll("super smash bros. brawl", ImmutableList.of("ssbb", "smash brothers"));
    eventTypeKeywordMap.putAll("super smash bros. melee", ImmutableList.of("ssbm", "smash brothers"));
    eventTypeKeywordMap.putAll("super smash brothers brawl", ImmutableList.of("ssbb", "bros"));
    eventTypeKeywordMap.putAll("super smash brothers melee", ImmutableList.of("ssbm", "bros"));

    eventTypeKeywordMap.putAll("warhammer 40,000: black crusade", ImmutableList.of("40k"));
    eventTypeKeywordMap.putAll("warhammer 40,000: dark heresy", ImmutableList.of("40k"));
    eventTypeKeywordMap.putAll("warhammer 40,000: deathwatch", ImmutableList.of("40k"));
    eventTypeKeywordMap.putAll("warhammer 40,000: only war", ImmutableList.of("40k"));
    eventTypeKeywordMap.putAll("warhammer 40,000: rogue trader", ImmutableList.of("40k"));
    eventTypeKeywordMap.putAll("warhammer 40k", ImmutableList.of("40,000"));

    eventTypeKeywordMap.putAll("world of warcraft tcg", ImmutableList.of("wow"));
    eventTypeKeywordMap.putAll("yggdrasil", ImmutableList.of("yggdrasill"));
    eventTypeKeywordMap.putAll("yggdrasill", ImmutableList.of("yggdrasil"));
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    log.info("Got request for EventParser: " + RequestHelpers.asDebugString(req));
    log.info("Blowing away the old events");
    List<Gencon2013Event> list = ofy().load().type(Gencon2013Event.class).limit(200).list();
    while (!list.isEmpty()) {
      ofy().delete().entities(list);
      list = ofy().load().type(Gencon2013Event.class).limit(200).list();
    }
    log.info("Old events deleted");
    Multimap<String, String> parameters = RequestHelpers.parameterMultimap(req);

    String resourcePath = "/schedules/short_events.csv";
    if (parameters.containsKey("full")) {
      resourcePath = "/schedules/20130818003001.csv";
    }

    log.info("Parsing " + resourcePath);
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        getServletContext().getResourceAsStream(resourcePath)));

    final int eventsPerBatch = 100;
    List<Gencon2013Event> events = new ArrayList<>(eventsPerBatch);
    List<Document> documents = new ArrayList<>(eventsPerBatch);

    IndexSpec indexSpec = IndexSpec.newBuilder().setName("events").build();
    Index index = SearchServiceFactory.getSearchService().getIndex(indexSpec);
    int eventsSeen = 0;

    Map<String, Gencon2013Category> categories = new HashMap<>();
    for (Gencon2013Event parsedEvent : new GenconSchedulerParser(reader)) {
      String eventType = parsedEvent.getEventType();
      if (!categories.containsKey(eventType)) {
        categories.put(eventType, new Gencon2013Category(eventType));
      }

      categories.get(eventType).addEvent(parsedEvent);

      events.add(parsedEvent);
      documents.add(indexEvent(parsedEvent));

      if (events.size() == eventsPerBatch) {
        log.info("Writing out a batch.");
        ofy().save().entities(events);
        index.put(documents);

        events = new ArrayList<>(eventsPerBatch);
        documents = new ArrayList<>(eventsPerBatch);
      }

      eventsSeen++;
    }

    log.info("Saving last entities.");
    ofy().save().entities(categories.values()).now();
    ofy().save().entities(events).now();
    index.put(documents);

    log.info("All entities saved");

    resp.setContentType("text/plain");
    resp.getWriter().println("Processed events - " + eventsSeen);
    resp.getWriter().println("Categories: " + categories.size());
  }

  private Document indexEvent(Gencon2013Event parsedEvent) {
    return Document.newBuilder()
        .setId(parsedEvent.getGameId())
        .addField(textField("category", parsedEvent.getEventTypeAbbreviation()))
        .addField(textField("shortDescription", parsedEvent.getShortDescription()))
        .addField(textField("longDescription", parsedEvent.getLongDescription()))
        .addField(textField("gameSystem", parsedEvent.getGameSystem()))
        .addField(textField("longDescription", parsedEvent.getRulesEdition()))
        .addField(textField("day", dayOfWeekToText(parsedEvent.getDayOfWeek())))
        .addField(Field.newBuilder()
            .setName("duration")
            .setNumber(parsedEvent.getDuration().getMillis() / (1000.0 * 60 * 60)))
        .addField(textField("keywords", getKeywords(parsedEvent)))
        .build();
  }

  private String getKeywords(Gencon2013Event event) {
    StringBuilder builder = new StringBuilder();
    builder.append(event.getEventType().toLowerCase()).append(" ");

    String gameSystem = Strings.nullToEmpty(event.getGameSystem()).toLowerCase();
    if (eventTypeKeywordMap.containsKey(gameSystem)) {
      for (String keyword : eventTypeKeywordMap.get(gameSystem)) {
        builder.append(keyword).append(" ");
      }
    }

    return builder.toString();
  }

  private String dayOfWeekToText(int dayOfWeek) {
    switch (dayOfWeek) {
      case 1: return "monday";
      case 2: return "tuesday";
      case 3: return "wednesday";
      case 4: return "thursday";
      case 5: return "friday";
      case 6: return "saturday";
      case 7: return "sunday";
      default: return "";
    }
  }

  private Field.Builder textField(String fieldName, String fieldText) {
    return Field.newBuilder().setName(fieldName).setText(
        Strings.nullToEmpty(fieldText).toLowerCase());
  }
}
