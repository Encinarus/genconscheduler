package com.lightpegasus.scheduler.gencon;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.lightpegasus.scheduler.gencon.entity.GenconEventGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by alek on 5/17/15.
 */
public class EventOrganizer {

  public static List<EventGroupPartition> partitionByRules(List<GenconEventGroup> events) {
    ListMultimap<String, GenconEventGroup> eventsByRules = ArrayListMultimap.create();

    //log.info("Partitioning " + events.size() + " events");
    for (GenconEventGroup event : events) {
      String gameSystem = event.getGameSystem();
      if (Strings.isNullOrEmpty(gameSystem)) {
        gameSystem = "Unspecified";
      }
      eventsByRules.put(gameSystem, event);
    }

    List<EventGroupPartition> groups = new ArrayList<>();
    for (String ruleSystem : eventsByRules.keySet()) {
      groups.add(new EventGroupPartition(ruleSystem, eventsByRules.get(ruleSystem)));
    }

    Collections.sort(groups, new Comparator<EventGroupPartition>() {
      @Override
      public int compare(EventGroupPartition o1, EventGroupPartition o2) {
        return o1.getName().compareToIgnoreCase(o2.getName());
      }
    });
    return groups;
  }
}
