package com.lightpegasus.scheduler.gencon.entity;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import org.joda.time.DateTime;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
@Entity
public class UpdateHistory {
  @Id private long updateTimestampMillis;

  private DateTime updateTimestamp;

  private Set<String> updatedEvents = new HashSet<>();
  private Set<String> newEvents = new HashSet<>();
  private Set<String> deletedEvents = new HashSet<>();

  // TODO(alek): Blob containing the parsed CSV

  public UpdateHistory() {

  }

  public UpdateHistory(DateTime updateTimestamp, Set<String> updatedEvents,
      Set<String> newEvents, Set<String> deletedEvents) {
    this.updateTimestamp = updateTimestamp;
    this.updateTimestampMillis = updateTimestamp.getMillis();
    this.updatedEvents = updatedEvents;
    this.newEvents = newEvents;
    this.deletedEvents = deletedEvents;
  }

}
