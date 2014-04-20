package com.lightpegasus.scheduler.gencon.entity;

import com.google.appengine.labs.repackaged.com.google.common.base.Preconditions;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a category of event at gencon, for
 */
@Entity
@Cache
public class GenconCategory {
  public static Set<String> CATEGORY_ABBREVIATIONS = Sets.newHashSet(
      "ANI", "BGM", "CGM", "EGM", "ENT", "FLM", "GEN", "HMN", "KID", "LRP", "MHE",
      "NMN", "RPG", "RPGA", "SEM", "SPA", "TCG", "TDA", "TRD", "WKS", "ZED"
  );

  @Id private String key;

  private int year;
  private String eventTypeAbbreviation;
  private String categoryName;

  private Set<String> gameSystems;

  @Ignore private Set<Long> hashes;

  private int eventCount;

  @SuppressWarnings("UnusedDeclaration")
  public GenconCategory() {
    // No-arg ctor needed for objectify
  }

  public GenconCategory(String categoryName, int year) {
    this.eventTypeAbbreviation = categoryName.substring(0, categoryName.indexOf(' '));
    this.year = year;
    this.key = eventTypeAbbreviation + ":" + year;
    this.categoryName = categoryName;
    this.gameSystems = new HashSet<>();
    this.hashes = new HashSet<>();
  }

  public String getEventTypeAbbreviation() {
    return eventTypeAbbreviation;
  }

  public void addEvent(GenconEvent event) {
    Preconditions.checkNotNull(event);
    Preconditions.checkArgument(Objects.equal(event.getEventType(), categoryName),
        "Event is for the wrong category, expected " + categoryName
            + " and found " + event.getEventType());

    eventCount++;
    hashes.add(event.getClusterHash());

    if (!Strings.isNullOrEmpty(event.getGameSystem())) {
      gameSystems.add(event.getGameSystem());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    GenconCategory that = (GenconCategory) o;

    return Objects.equal(this.eventCount, that.eventCount)
        && Objects.equal(this.categoryName, that.categoryName)
        && Objects.equal(this.gameSystems, that.gameSystems)
        && Objects.equal(this.year, that.year);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(categoryName, gameSystems, eventCount, year);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("categoryName", categoryName)
        .add("gameSystems", gameSystems)
        .add("eventCount", eventCount)
        .add("year", year)
        .toString();
  }

  public String getCategoryName() {
    return categoryName;
  }

  public Set<String> getGameSystems() {
    return ImmutableSet.copyOf(gameSystems);
  }

  public int getEventCount() {
    return eventCount;
  }

  public int getYear() {
    return year;
  }

  public String getKey() {
    return key;
  }
}
