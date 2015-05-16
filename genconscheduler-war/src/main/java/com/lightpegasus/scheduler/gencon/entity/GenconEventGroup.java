package com.lightpegasus.scheduler.gencon.entity;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import org.joda.time.DateTimeConstants;

import java.util.List;

/**
 * Represents a cluster of events.
 */
@Entity
@Cache
public class GenconEventGroup {
  @Id
  private String clusterId;

  @Index
  private long clusterHash;

  private String title;
  @Index private String eventTypeAbbreviation;
  @Index private long year;

  private String shortDescription;
  private String gameSystem;
  private String rulesEdition;

  private int similarEventCount;
  private int wedOpenEvents;
  private int wedAvailable;
  private int thursOpenEvents;
  private int thursAvailable;
  private int friOpenEvents;
  private int friAvailable;
  private int satOpenEvents;
  private int satAvailable;
  private int sunOpenEvents;
  private int sunAvailable;

  // Needed for objectify
  public GenconEventGroup() {

  }

  public GenconEventGroup(List<GenconEvent> clusteredEvents) {
    setSeedEvent(clusteredEvents.get(0));
    for (GenconEvent event : clusteredEvents) {
      mergeEvent(event);
    }
  }

  private void setSeedEvent(GenconEvent seedEvent) {
    this.clusterId = seedEvent.getGameId();
    this.clusterHash = seedEvent.getClusterHash();
    this.title = seedEvent.getTitle();
    this.eventTypeAbbreviation = seedEvent.getEventTypeAbbreviation();
    this.year = seedEvent.getYear();
    this.shortDescription = seedEvent.getShortDescription();
    this.gameSystem = seedEvent.getGameSystem();
    this.rulesEdition = seedEvent.getRulesEdition();
    // Don't set the various fields, since we'll process and reload after
  }

  public void mergeEvent(GenconEvent event) {
    if (event.getClusterHash() != clusterHash || event.getCanceled()) {
      return;
    }

    similarEventCount++;
    int openEventIncrement = event.hasTickets() ? 1 : 0;
    int availableIncrement = event.getTicketsAvailable();
    switch (event.getDayOfWeek()) {
      case DateTimeConstants.WEDNESDAY:
        wedAvailable += availableIncrement;
        wedOpenEvents += openEventIncrement;
        break;
      case DateTimeConstants.THURSDAY:
        thursAvailable += availableIncrement;
        thursOpenEvents += openEventIncrement;
        break;
      case DateTimeConstants.FRIDAY:
        friAvailable += availableIncrement;
        friOpenEvents += openEventIncrement;
        break;
      case DateTimeConstants.SATURDAY:
        satAvailable += availableIncrement;
        satOpenEvents += openEventIncrement;
        break;
      case DateTimeConstants.SUNDAY:
        sunAvailable += availableIncrement;
        sunOpenEvents += openEventIncrement;
        break;
    }

  }

  public int getTicketsAvailable() {
    return wedAvailable + thursAvailable + friAvailable + satAvailable + sunAvailable;
  }

  public int getOpenSessions() {
    return wedOpenEvents + thursOpenEvents + friOpenEvents + satOpenEvents + sunOpenEvents;
  }

  public String getClusterId() {
    return clusterId;
  }

  public long getClusterHash() {
    return clusterHash;
  }

  public String getTitle() {
    return title;
  }

  public String getEventTypeAbbreviation() {
    return eventTypeAbbreviation;
  }

  public long getYear() {
    return year;
  }

  public String getShortDescription() {
    return shortDescription;
  }

  public String getGameSystem() {
    return gameSystem;
  }

  public String getRulesEdition() {
    return rulesEdition;
  }

  public int getSimilarEventCount() {
    return similarEventCount;
  }

  public int getWedOpenEvents() {
    return wedOpenEvents;
  }

  public int getWedAvailable() {
    return wedAvailable;
  }

  public int getThursOpenEvents() {
    return thursOpenEvents;
  }

  public int getThursAvailable() {
    return thursAvailable;
  }

  public int getFriOpenEvents() {
    return friOpenEvents;
  }

  public int getFriAvailable() {
    return friAvailable;
  }

  public int getSatOpenEvents() {
    return satOpenEvents;
  }

  public int getSatAvailable() {
    return satAvailable;
  }

  public int getSunOpenEvents() {
    return sunOpenEvents;
  }

  public int getSunAvailable() {
    return sunAvailable;
  }
}
