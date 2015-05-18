package com.lightpegasus.scheduler.gencon;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.impl.translate.opt.BigDecimalLongTranslatorFactory;
import com.googlecode.objectify.impl.translate.opt.joda.JodaTimeTranslators;
import com.lightpegasus.objectify.DurationLongValueTranslatorFactory;
import com.lightpegasus.scheduler.gencon.entity.BackgroundTaskStatus;
import com.lightpegasus.scheduler.gencon.entity.GenconCategory;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.entity.GenconEventGroup;
import com.lightpegasus.scheduler.gencon.entity.SearchQuery;
import com.lightpegasus.scheduler.gencon.entity.UpdateHistory;
import com.lightpegasus.scheduler.gencon.entity.User;
import com.lightpegasus.scheduler.web.SchedulerApp;

import java.util.List;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;


/**
 * Static utility class for loading entities.
 */
public class Queries {
  private static Logger log = Logger.getLogger(Queries.class.getSimpleName());

  public static void initializeObjectify() {
    // All translators need to be installed before the entities.
    ObjectifyService.factory().getTranslators().add(new BigDecimalLongTranslatorFactory());
    ObjectifyService.factory().getTranslators().add(
        new DurationLongValueTranslatorFactory());
    JodaTimeTranslators.add(ObjectifyService.factory());

    // Now register entities.
    ObjectifyService.register(GenconEvent.class);
    ObjectifyService.register(GenconEventGroup.class);
    ObjectifyService.register(GenconCategory.class);
    ObjectifyService.register(SearchQuery.class);
    ObjectifyService.register(BackgroundTaskStatus.class);
    ObjectifyService.register(User.class);
    ObjectifyService.register(UpdateHistory.class);

    log.info("Registered entities");
  }

  private Objectify ofy() {
    return ObjectifyService.ofy();
  }

  public Queries() {
  }

  public BackgroundTaskStatus getSyncStatus(int year) {
    BackgroundTaskStatus status = ofy().load().type(BackgroundTaskStatus.class).id(year).now();
    if (status == null) {
      status = new BackgroundTaskStatus(year, BackgroundTaskStatus.TaskType.UPDATE_EVENTS);
      ofy().save().entity(status);
    }
    return status;
  }

  public Optional<GenconEvent> loadGenconEvent(String eventId, int genconYear) {
    return Optional.fromNullable(
        ofy().load().type(GenconEvent.class).id(GenconEvent.idForYear(genconYear, eventId)).now());
  }

  public ImmutableList<GenconEvent> loadEventsByHash(long clusterHash, long year) {
    return ImmutableList.copyOf(ofy().load().type(GenconEvent.class)
        .filter("clusterHash", clusterHash)
        .filter("year", year)
        .orderKey(false)
        .list());
  }

  public ImmutableList<GenconEventGroup> loadEventsForCategory(
      String eventTypeAbbreviation, int genconYear) {
    return ImmutableList.copyOf(ofy().load().type(GenconEventGroup.class)
        .filter("eventTypeAbbreviation", eventTypeAbbreviation)
        .filter("year", genconYear)
        .list());
  }

  public ImmutableList<GenconEvent> loadSimilarEvents(GenconEvent event) {
    return loadEventsByHash(event.getClusterHash(), event.getYear());
  }

  public ImmutableList<GenconCategory> allCategories(long genconYear) {
    return ImmutableList.copyOf(ofy().load()
        .type(GenconCategory.class)
        .filter("year", genconYear)
        .list());
  }

  public GenconEventGroup loadGenconEventGroup(long genconYear, long clusterHash) {
    return ofy().load().type(GenconEventGroup.class)
        .filter("year", genconYear)
        .filter("clusterHash", clusterHash)
        .first()
        .now();
  }

  public Optional<User> loadUser(String userId) {
    return Optional.fromNullable(ofy().load().type(User.class).id(userId).now());
  }

  public User loadOrCreateUser(String userId, String email, String nickname) {
    Optional<User> loadedUser = loadUser(userId);
    if (loadedUser.isPresent()) {
      return loadedUser.get();
    } else {
      User newUser = new User(userId, email, nickname);
      ofy().save().entity(newUser).now();
      return newUser;
    }
  }

  public List<GenconEventGroup> loadEventGroupsForCategory(String category, int genconYear) {
    return ofy().load().type(GenconEventGroup.class)
        .filter("eventTypeAbbreviation", category)
        .filter("year", genconYear)
        .list();
  }
}
