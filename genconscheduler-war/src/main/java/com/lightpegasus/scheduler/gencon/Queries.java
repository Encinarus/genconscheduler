package com.lightpegasus.scheduler.gencon;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.lightpegasus.scheduler.gencon.entity.BackgroundTaskStatus;
import com.lightpegasus.scheduler.gencon.entity.GenconCategory;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.entity.GenconEventGroup;

import java.util.List;
import java.util.logging.Logger;


/**
 * Static utility class for loading entities.
 */
public class Queries {
  private static Logger log = Logger.getLogger(Queries.class.getSimpleName());

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
}
