package com.lightpegasus.scheduler.gencon;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.lightpegasus.scheduler.gencon.entity.GenconCategory;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import com.lightpegasus.scheduler.gencon.entity.SyncStatus;

import java.util.ArrayList;
import java.util.Collection;
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

  public SyncStatus getSyncStatus(int year) {
    SyncStatus status = ofy().load().type(SyncStatus.class).id(year).now();
    if (status == null) {
      status = new SyncStatus(year);
      ofy().save().entity(status);
    }
    return status;
  }

  public Optional<GenconEvent> loadGencon2013Event(String eventId) {
    return Optional.fromNullable(
        ofy().load().type(GenconEvent.class).id(eventId + ":2013").now());
  }

  public java.util.Map<String, GenconEvent> loadGencon2013Events(Collection<String> eventIds) {
    List<String> gencon2013Ids = new ArrayList<>(eventIds.size());
    for (String eventId : eventIds) {
      gencon2013Ids.add(eventId + ":2013");
    }
    return ofy().load().type(GenconEvent.class).ids(gencon2013Ids);
  }

  public List<GenconEvent> loadSimilarGencon2013Events(GenconEvent event) {
    return ImmutableList.copyOf(ofy().load().type(GenconEvent.class)
        .filter("clusterHash", event.getClusterHash())
        .filter("year", event.getYear())
        .list());
  }

  public List<GenconCategory> allCategories() {
    return ImmutableList.copyOf(ofy().load().type(GenconCategory.class).list());
  }
}
