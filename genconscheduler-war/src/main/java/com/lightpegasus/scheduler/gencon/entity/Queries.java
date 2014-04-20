package com.lightpegasus.scheduler.gencon.entity;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.cmd.Query;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static com.googlecode.objectify.ObjectifyService.ofy;


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

  public Optional<Gencon2013Event> eventByEventId(String eventId) {
    return Optional.fromNullable(ofy().load().type(Gencon2013Event.class).id(eventId).now());
  }

  public List<Gencon2013Event> eventsForHash(Gencon2013Event event) {
    return ImmutableList.copyOf(ofy().load().type(Gencon2013Event.class)
        .filter("clusterHash", event.getClusterHash()).list());
  }

  public List<Gencon2013Category> allCategories() {
    return ImmutableList.copyOf(ofy().load().type(Gencon2013Category.class).list());
  }
}
