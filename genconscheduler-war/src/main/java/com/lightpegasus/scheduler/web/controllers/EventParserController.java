package com.lightpegasus.scheduler.web.controllers;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.SearchServiceFactory;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.googlecode.objectify.Key;
import com.lightpegasus.scheduler.gencon.Gencon2013ScheduleParser;
import com.lightpegasus.scheduler.gencon.entity.BackgroundTaskStatus;
import com.lightpegasus.scheduler.gencon.entity.GenconCategory;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.Queries;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.RequestHelpers;
import com.lightpegasus.scheduler.web.ThymeleafController;
import org.joda.time.DateTime;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 *
 */
public class EventParserController extends ThymeleafController {
  private static Logger log = Logger.getLogger(EventParserController.class.getSimpleName());

  @Override
  public void doProcess(WebContext context, TemplateEngine engine, Optional<User> loggedInUser) throws Exception {
    log.info("Got request for EventParser: " +
        RequestHelpers.asDebugString(context.getHttpServletRequest()));

    Multimap<String, String> parameters = RequestHelpers.parameterMultimap(
        context.getHttpServletRequest());

    String year = Iterables.getFirst(parameters.get("year"), "2013");
    boolean isFull = Boolean.valueOf(Iterables.getFirst(parameters.get("full"), "false"));

    BackgroundTaskStatus syncStatus = new Queries().getSyncStatus(Integer.parseInt(year));

    log.info("Loading old event ids");

    ImmutableMap.Builder<String, Key<GenconEvent>> storedKeyBuilder = ImmutableMap.builder();
    long genconYear = syncStatus.getYear();
    for (Key<GenconEvent> key : getStoredKeysForYear(genconYear)) {
      storedKeyBuilder.put(key.getName(), key);
    }
    Map<String, Key<GenconEvent>> storedKeys  = storedKeyBuilder.build();

    int eventsSeen = 0;
    final Set<String> parsedEventKeys = new HashSet<>();
    final Map<String, GenconCategory> categories = new HashMap<>();

    try (BufferedReader reader = loadGenconCsv(context, year, isFull)) {
      final int eventsPerBatch = 100;
      List<GenconEvent> eventsToSave = new ArrayList<>(eventsPerBatch);
      List<Document> docsToSave = new ArrayList<>(eventsPerBatch);

      IndexSpec indexSpec = IndexSpec.newBuilder().setName("events").build();
      Index index = SearchServiceFactory.getSearchService().getIndex(indexSpec);

      // Process the file, and update all events which have been updated since
      // the most recent update we've seen in a file
      DateTime mostRecentUpdate = syncStatus.getSyncTime();
      for (GenconEvent event : new Gencon2013ScheduleParser(reader)) {
        String eventType = event.getEventType();
        if (!categories.containsKey(eventType)) {
          categories.put(eventType, new GenconCategory(eventType, 2013));
        }

        categories.get(eventType).addEvent(event);

        parsedEventKeys.add(event.getEventKey());
        DateTime eventLastModified = event.getLastModified();
        if (!storedKeys.containsKey(event.getEventKey())
            || eventLastModified.isAfter(syncStatus.getSyncTime())) {
          eventsToSave.add(event);
          docsToSave.add(indexEvent(event));
          eventsSeen++;

          if (eventLastModified.isAfter(mostRecentUpdate)) {
            mostRecentUpdate = eventLastModified;
          }
        }

        if (eventsToSave.size() == eventsPerBatch) {
          saveBatch(eventsToSave, docsToSave, index);

          eventsToSave = new ArrayList<>(eventsPerBatch);
          docsToSave = new ArrayList<>(eventsPerBatch);
        }
      }

      log.info("Saving last entities.");

      saveBatch(eventsToSave, docsToSave, index);

      ofy().save().entities(categories.values()).now();

      // Now to figure out which events to delete
      // TODO: Once we have starred events, we'll need to notify people
      markEventsDeleted(mostRecentUpdate, storedKeys, parsedEventKeys, index);

      log.info("All entities saved");

      syncStatus.setSyncTime(mostRecentUpdate);
      ofy().save().entity(syncStatus);
    }
    context.getHttpServletResponse().setContentType("text/plain");
    context.getHttpServletResponse().getWriter().println(
        "Processed events - " + eventsSeen + " of " + parsedEventKeys.size());
    context.getHttpServletResponse().getWriter().println(
        "Categories: " + categories.size());
  }

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

  private void saveBatch(List<GenconEvent> events, List<Document> documents, Index index) {
    log.info("Writing out a batch of " + events.size() + "/" + documents.size());
    ofy().save().entities(events).now();
    index.put(documents);
  }

  private void markEventsDeleted(DateTime updateTime, Map<String, Key<GenconEvent>> storedKeys,
                                 final Set<String> parsedEventKeys, Index index) {
    Map<String, Key<GenconEvent>> deletedEventKeys =
        Maps.filterKeys(storedKeys, new Predicate<String>() {
          @Override
          public boolean apply(String key) {
            return !parsedEventKeys.contains(key);
          }
        });

    log.info("Deleting " + deletedEventKeys.size() + " of " + storedKeys.size());
    for (List<Key<GenconEvent>> removedKeys : Iterables.partition(deletedEventKeys.values(), 100)) {
      Set<String> docIdsToDelete = new HashSet<>();
      Collection<GenconEvent> eventsToDelete = ofy().load().keys(removedKeys).values();
      for (GenconEvent event : eventsToDelete) {
        event.setStatus(GenconEvent.Status.DEAD);
        event.setLastModified(updateTime);
        docIdsToDelete.add(event.getEventKey());
      }
      ofy().save().entities(eventsToDelete);
      index.delete(docIdsToDelete);
    }
  }

  private QueryResultIterable<Key<GenconEvent>> getStoredKeysForYear(long genconYear) {
    return ofy().load().type(GenconEvent.class).filter("year", genconYear).keys().iterable();
  }

  private BufferedReader loadGenconCsv(WebContext context, String year, boolean isFull) {
    BufferedReader reader = null;
    switch (year) {
      case "2013": {
        String resourcePath = "/WEB-INF/schedules/short_events.csv";
        if (isFull) {
          resourcePath = "/WEB-INF/schedules/20130818003001.csv";
        }

        log.info("Parsing " + resourcePath);
        reader = new BufferedReader(new InputStreamReader(
            context.getServletContext().getResourceAsStream(resourcePath)));
      }
      break;
      default:
        throw new UnsupportedOperationException("Year not supported: " + year);
    }
    return reader;
  }

  private Document indexEvent(GenconEvent parsedEvent) {
    return Document.newBuilder()
        .setId(parsedEvent.getEventKey())
        .addField(textField("category", parsedEvent.getEventTypeAbbreviation()))
        .addField(textField("shortDescription", parsedEvent.getShortDescription()))
        .addField(textField("longDescription", parsedEvent.getLongDescription()))
        .addField(textField("gameSystem", parsedEvent.getGameSystem()))
        .addField(textField("longDescription", parsedEvent.getRulesEdition()))
        .addField(textField("day", dayOfWeekToText(parsedEvent.getDayOfWeek())))
        .addField(Field.newBuilder().setName("year").setNumber(parsedEvent.getYear()))
        .addField(Field.newBuilder()
            .setName("duration")
            .setNumber(parsedEvent.getDuration().getMillis() / (1000.0 * 60 * 60)))
        .addField(textField("keywords", getKeywords(parsedEvent)))
        .build();
  }

  private String getKeywords(GenconEvent event) {
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
