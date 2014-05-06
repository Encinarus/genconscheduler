package com.lightpegasus.scheduler.gencon;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.lightpegasus.scheduler.gencon.entity.BackgroundTaskStatus;
import com.lightpegasus.scheduler.gencon.entity.GenconCategory;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;

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

  public ImmutableList<GenconEvent> loadSimilarEvents(GenconEvent event) {
    return ImmutableList.copyOf(ofy().load().type(GenconEvent.class)
        .filter("clusterHash", event.getClusterHash())
        .filter("year", event.getYear())
        .list());
  }

  public ImmutableList<GenconCategory> allCategories(long genconYear) {
    return ImmutableList.copyOf(ofy().load()
        .type(GenconCategory.class)
        .filter("year", genconYear)
        .list());
  }
}
