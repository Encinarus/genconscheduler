package com.lightpegasus.scheduler.web.controllers;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.SearchServiceFactory;
import com.google.common.base.Function;
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
import com.lightpegasus.scheduler.gencon.GenconScheduleParser;
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
import java.util.Arrays;
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
  public void doProcess(WebContext context, TemplateEngine engine, Optional<User> loggedInUser,
      int genconYear) throws Exception {
    log.info("Got request for EventParser: " +
        RequestHelpers.asDebugString(context.getHttpServletRequest()));

    Multimap<String, String> parameters = RequestHelpers.parameterMultimap(
        context.getHttpServletRequest());

    boolean isFull = Boolean.valueOf(Iterables.getFirst(parameters.get("full"), "false"));

    BackgroundTaskStatus syncStatus = new Queries().getSyncStatus(genconYear);

    log.info("Loading old event ids");

    ImmutableMap.Builder<String, Key<GenconEvent>> storedKeyBuilder = ImmutableMap.builder();
    for (Key<GenconEvent> key : getStoredKeysForYear(genconYear)) {
      storedKeyBuilder.put(key.getName(), key);
    }
    Map<String, Key<GenconEvent>> storedKeys  = storedKeyBuilder.build();

    int eventsSeen = 0;
    final Set<String> parsedEventKeys = new HashSet<>();
    final Map<String, GenconCategory> categories = new HashMap<>();

    try (BufferedReader reader = loadGenconCsv(context, genconYear, isFull)) {
      final int eventsPerBatch = 100;
      List<GenconEvent> eventsToSave = new ArrayList<>(eventsPerBatch);
      List<Document> docsToSave = new ArrayList<>(eventsPerBatch);

      IndexSpec indexSpec = IndexSpec.newBuilder().setName("events").build();
      Index index = SearchServiceFactory.getSearchService().getIndex(indexSpec);

      // Process the file, and update all events which have been updated since
      // the most recent update we've seen in a file
      DateTime mostRecentUpdate = syncStatus.getSyncTime();
      for (GenconEvent event : new GenconScheduleParser(reader, genconYear)) {
        String eventType = event.getEventType();
        if (!categories.containsKey(eventType)) {
          categories.put(eventType, new GenconCategory(eventType, genconYear));
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

  private static void addKeywords(String eventTitle, String... keywords) {
    eventTypeKeywordMap.putAll(eventTitle.toLowerCase(),
        Iterables.transform(Arrays.asList(keywords), new Function<String, String>() {
          @Override public String apply(String input) {
            return input.toLowerCase();
          }}));
  }
  static {
    addKeywords("magic: the gathering",
        "mtg", "wizards of the coast", "wotc");
    addKeywords("magic",
        "mtg", "magic the gathering", "wizards of the coast", "wotc");

    addKeywords("a game of thrones",
        "got", "song of ice & fire");
    addKeywords("a game of thrones: the board game",
        "got", "song of ice & fire");
    addKeywords("song of ice & fire",
        "got", "song of ice & fire", "a game of thrones");

    addKeywords("babylon 5: a call to arms", "b5");
    addKeywords("cards against humanity", "cah");
    // catan!!!
    String[] settlersKeywords = {"settlers of catan"};
    addKeywords("catan: traders and barbarians", settlersKeywords);
    addKeywords("catan dice game", settlersKeywords);
    addKeywords("catan histories: merchants of europe", settlersKeywords);
    addKeywords("catan junior", settlersKeywords);
    addKeywords("catan: explorers and pirates", settlersKeywords);
    addKeywords("catan: seafarers", settlersKeywords);
    addKeywords("catan: traders and barbarians", settlersKeywords);

    // D&D
    String[] dndKeywords = {
        "d&d", "dungeons and dragons", "rpg", "d20", "wizards of the coast", "wotc"};
    addKeywords("d&d gamma world roleplaying game", dndKeywords);
    addKeywords("d&d miniatures", dndKeywords);
    addKeywords("dungeon & dragons", dndKeywords);
    addKeywords("dungeons & dragons", dndKeywords);

    addKeywords("kobolds ate my baby!", "kamb");
    addKeywords("legend of the five rings", "l5r");

    addKeywords("lord of the rings", "lotr");
    addKeywords("the hobbit card game", "lotr", "lord of the rings");
    addKeywords("the hobbit: the defeat of smaug", "lotr", "lord of the rings");
    addKeywords("lord of the rings tradeable miniatures", "lotr");
    addKeywords("lord of the rings: the card game", "lotr");
    addKeywords("the lord of the rings: the battle for middle-earth", "lotr");
    addKeywords("the lord of the rings: the card game", "lotr");
    addKeywords("lord of the rings: the fellowship of the ring dbg", "lotr");
    addKeywords("middle-earth role playing", "lotr", "middle earth");
    addKeywords("meccg", "middle", "earth", "ccg", "collectable card game");

    addKeywords("pathfinder", "paizo", "dungeons and dragons", "d&d");
    addKeywords("pathfinder roleplaying  game", "paizo", "dungeons and dragons", "d&d");
    addKeywords("pathfinder roleplaying game", "paizo", "dungeons and dragons", "d&d");
    addKeywords("spacehulk", "space hulk");
    addKeywords("space hulk", "spacehulk");
    addKeywords("star trek: deep space nine", "ds9", "9");

    addKeywords("super smash bros. brawl", "ssbb", "smash brothers");
    addKeywords("super smash bros. melee", "ssbm", "smash brothers");
    addKeywords("super smash brothers brawl", "ssbb", "bros");
    addKeywords("super smash brothers melee", "ssbm", "bros");

    addKeywords("warhammer 40,000: black crusade", "40k");
    addKeywords("warhammer 40,000: dark heresy", "40k");
    addKeywords("warhammer 40,000: deathwatch", "40k");
    addKeywords("warhammer 40,000: only war", "40k");
    addKeywords("warhammer 40,000: rogue trader", "40k");
    addKeywords("warhammer 40k", "40,000");

    addKeywords("world of warcraft tcg", "wow");
    addKeywords("yggdrasil", "yggdrasill");
    addKeywords("yggdrasill", "yggdrasil");
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

  private BufferedReader loadGenconCsv(WebContext context, int year, boolean isFull) {
    BufferedReader reader = null;
    switch (year) {
      case 2013: {
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
      default: throw new IllegalArgumentException("Unable to get a day of week for " + dayOfWeek);
    }
  }

  private Field.Builder textField(String fieldName, String fieldText) {
    return Field.newBuilder().setName(fieldName).setText(
        Strings.nullToEmpty(fieldText).toLowerCase());
  }
}
