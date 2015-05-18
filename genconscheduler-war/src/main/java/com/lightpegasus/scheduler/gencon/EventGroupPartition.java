package com.lightpegasus.scheduler.gencon;

import com.lightpegasus.scheduler.gencon.entity.GenconEventGroup;

import java.util.List;

/**
* Created by alek on 5/17/15.
*/
public class EventGroupPartition {
  private final List<GenconEventGroup> searchResults;
  private final String name;

  public EventGroupPartition(String name, List<GenconEventGroup> events) {
    this.name = name;
    searchResults = events;
  }

  public String getName() {
    return name;
  }

  public List<GenconEventGroup> getEventGroups() {
    return searchResults;
  }
}
