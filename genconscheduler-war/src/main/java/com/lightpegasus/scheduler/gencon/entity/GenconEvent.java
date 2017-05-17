package com.lightpegasus.scheduler.gencon.entity;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Full details of a gencon event.
 * <p/>
 */
@Entity
@Cache
public class GenconEvent {
  @Id
  private String eventKey;
  @Index private long year;
  private String gameId;
  @Index private Status status = Status.ALIVE;
  // Index the fields we'll want to query on.
  @Index private long clusterHash;
  @Index private String title;
  // This is the 3 or 4 letter code which represents the event type.
  @Index private String eventTypeAbbreviation;
  /**
   * References DateTimeConstants.MONDAY/TUESDAY/... with MONDAY=1 and SUNDAY=7
   */
  @Index
  private int dayOfWeek;
  private String group;
  private String eventType;
  private String shortDescription;
  private String longDescription;
  private String gameSystem;
  private String rulesEdition;
  private Integer minimumPlayers;
  private Integer maximumPlayers;
  private String ageRequired;
  private String experienceRequired;
  private Boolean materialsProvided;
  private DateTime startTime;
  private Duration duration;
  private DateTime endTime;
  private List<String> gmNames;
  private String website;
  private String email;
  private Boolean isTournament;
  private Integer roundNumber;
  private Integer totalRounds;
  private Duration minimumPlayTime;
  private Boolean attendeeRegistration;
  private BigDecimal cost;
  private String location;
  private String roomName;
  private String tableNumber;
  private String specialCategory;
  private Integer ticketsAvailable;
  private DateTime lastModified;

  public GenconEvent() {

  }

  public GenconEvent(int year, String gameId) {
    this.year = year;
    this.gameId = gameId;
    this.eventKey = idForYear(year, gameId);
  }

  public String getEventKey() {
    return eventKey;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public boolean getCanceled() {
    return status == Status.DEAD;
  }

  public void setTournament(Boolean isTournament) {
    this.isTournament = isTournament;
  }

  public String getGameId() {
    return gameId;
  }

  public String getGenconUrl() {
    if (year >= 2014) {
      return "http://gencon.com/events/" + gameId.substring(eventTypeAbbreviation.length() + 2);
    }
    return "#";
  }

  public String getPlannerUrl() {
    return "http://www.genconplanner.com/" + year + "/event/" + gameId;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getShortDescription() {
    return shortDescription;
  }

  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
  }

  public String getLongDescription() {
    return longDescription;
  }

  public void setLongDescription(String longDescription) {
    this.longDescription = longDescription;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
    this.eventTypeAbbreviation = eventType.substring(0, eventType.indexOf(' '));
  }

  public String getEventTypeAbbreviation() {
    return eventTypeAbbreviation;
  }

  public String getGameSystem() {
    return gameSystem;
  }

  public void setGameSystem(String gameSystem) {
    this.gameSystem = gameSystem;
  }

  public String getRulesEdition() {
    return rulesEdition;
  }

  public void setRulesEdition(String rulesEdition) {
    this.rulesEdition = rulesEdition;
  }

  public Integer getMinimumPlayers() {
    return minimumPlayers;
  }

  public void setMinimumPlayers(int minimumPlayers) {
    this.minimumPlayers = minimumPlayers;
  }

  public Integer getMaximumPlayers() {
    return maximumPlayers;
  }

  public void setMaximumPlayers(int maximumPlayers) {
    this.maximumPlayers = maximumPlayers;
  }

  public String getAgeRequired() {
    return ageRequired;
  }

  public void setAgeRequired(String ageRequired) {
    this.ageRequired = ageRequired;
  }

  public String getExperienceRequired() {
    return experienceRequired;
  }

  public void setExperienceRequired(String experienceRequired) {
    this.experienceRequired = experienceRequired;
  }

  public Boolean getMaterialsProvided() {
    return materialsProvided;
  }

  public void setMaterialsProvided(Boolean materialsProvided) {
    this.materialsProvided = materialsProvided;
  }

  public DateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(DateTime startTime) {
    this.startTime = startTime;
    this.dayOfWeek = startTime.getDayOfWeek();
  }

  public String getReadableDay() {
    switch (dayOfWeek) {
      case DateTimeConstants.WEDNESDAY: return "Wednesday";
      case DateTimeConstants.THURSDAY: return "Thursday";
      case DateTimeConstants.FRIDAY: return "Friday";
      case DateTimeConstants.SATURDAY: return "Saturday";
      case DateTimeConstants.SUNDAY: return "Sunday";
    }
    return "";
  }

  public String getReadableDate() {
    return DateTimeFormat.forPattern("EE")
        .withZone(DateTimeZone.forID("America/Indiana/Indianapolis"))
        .print(startTime);
  }

  public String getReadableStartTime() {
    return DateTimeFormat.forPattern("hh:mm a")
        .withZone(DateTimeZone.forID("America/Indiana/Indianapolis"))
        .print(startTime);
  }

  public Duration getDuration() {
    return duration;
  }

  public void setDuration(int durationMinutes) {
    this.duration = new Duration(TimeUnit.MINUTES.toMillis(durationMinutes));
  }

  public DateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(DateTime endTime) {
    this.endTime = endTime;
  }

  public String getReadableEndTime() {
    return DateTimeFormat.forPattern("hh:mm a")
        .withZone(DateTimeZone.forID("America/Indiana/Indianapolis"))
        .print(endTime);
  }

  public List<String> getGmNames() {
    return gmNames;
  }

  public String getReadableGmNames() {
    if (gmNames == null) {
      return "";
    }
    return Joiner.on(", ").join(gmNames);
  }

  public void setGmNames(List<String> gmNames) {
    this.gmNames = ImmutableList.copyOf(gmNames);
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
  }

  public String getEmail() {
    return email;
  }

  public String getSpamEmail() {
    return Strings.nullToEmpty(email).replace("@", "__at__");
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Boolean getIsTournament() {
    return isTournament;
  }

  public Integer getRoundNumber() {
    return roundNumber;
  }

  public void setRoundNumber(int roundNumber) {
    this.roundNumber = roundNumber;
  }

  public Integer getTotalRounds() {
    return totalRounds;
  }

  public void setTotalRounds(Integer totalRounds) {
    this.totalRounds = totalRounds;
  }

  public Duration getMinimumPlayTime() {
    return minimumPlayTime;
  }

  public void setMinimumPlayTime(Integer minPlayTimeMinutes) {
    if (minPlayTimeMinutes == null) {
      this.minimumPlayTime = null;
    } else {
      this.minimumPlayTime = new Duration(TimeUnit.MINUTES.toMillis(minPlayTimeMinutes));
    }
  }

  public Boolean getAttendeeRegistration() {
    return attendeeRegistration;
  }

  public void setAttendeeRegistration(Boolean attendeeRegistration) {
    this.attendeeRegistration = attendeeRegistration;
  }

  public BigDecimal getCost() {
    return cost;
  }

  public int getDollarCost() {
    return cost.intValue();
  }

  public void setCost(BigDecimal cost) {
    this.cost = cost;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getRoomName() {
    return roomName;
  }

  public void setRoomName(String roomName) {
    this.roomName = roomName;
  }

  public String getTableNumber() {
    return tableNumber;
  }

  public void setTableNumber(String tableNumber) {
    this.tableNumber = tableNumber;
  }

  public String getSpecialCategory() {
    return specialCategory;
  }

  public void setSpecialCategory(String specialCategory) {
    this.specialCategory = specialCategory;
  }

  public Integer getTicketsAvailable() {
    return ticketsAvailable;
  }

  public boolean hasTickets() {
    return ticketsAvailable > 0;
  }

  public void setTicketsAvailable(Integer ticketsAvailable) {
    this.ticketsAvailable = ticketsAvailable;
  }

  public DateTime getLastModified() {
    return lastModified;
  }

  public void setLastModified(DateTime lastModified) {
    this.lastModified = lastModified;
  }

  public long getClusterHash() {
    return clusterHash;
  }

  public int getDayOfWeek() {
    return dayOfWeek;
  }

  public void updateHash() {
    this.clusterHash = MoreObjects.toStringHelper(this)
        .add("group", scrub(group))
        .add("title", scrub(title))
        .add("shortDescription", scrub(shortDescription))
        .add("longDescription", scrub(longDescription))
        .add("eventType", scrub(eventType))
        .add("gameSystem", scrub(gameSystem))
        .add("rulesEdition", scrub(rulesEdition))
        .add("ageRequired", scrub(ageRequired))
        .add("experienceRequired", experienceRequired)
        .add("materialsProvided", materialsProvided)
        .add("duration", duration)
        .add("website", website)
        .add("email", email)
        .add("isTournament", isTournament)
        .add("totalRounds", totalRounds)
        .add("minimumPlayTime", minimumPlayTime)
        .add("cost", cost)
        .add("specialCategory", specialCategory)
        .add("year", year)
        .toString()
        .hashCode();
  }

  private String scrub(String input) {
    return Strings.nullToEmpty(input).toLowerCase()
        .replaceAll("[\\.,!?@#$%^&\\*()&]?(and)?", "");
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("group", group)
        .add("title", title)
        .add("clusterHash", clusterHash)
        .add("shortDescription", shortDescription)
        .add("longDescription", longDescription)
        .add("eventType", eventType)
        .add("gameSystem", gameSystem)
        .add("rulesEdition", rulesEdition)
        .add("minimumPlayers", minimumPlayers)
        .add("maximumPlayers", maximumPlayers)
        .add("ageRequired", ageRequired)
        .add("experienceRequired", experienceRequired)
        .add("materialsProvided", materialsProvided)
        .add("startTime", startTime)
        .add("duration", duration)
        .add("endTime", endTime)
        .add("gmNames", gmNames)
        .add("website", website)
        .add("email", email)
        .add("isTournament", isTournament)
        .add("roundNumber", roundNumber)
        .add("totalRounds", totalRounds)
        .add("minimumPlayTime", minimumPlayTime)
        .add("attendeeRegistration", attendeeRegistration)
        .add("cost", cost)
        .add("location", location)
        .add("roomName", roomName)
        .add("tableNumber", tableNumber)
        .add("specialCategory", specialCategory)
        .add("ticketsAvailable", ticketsAvailable)
        .add("lastModified", lastModified)
        .add("year", year)
        .toString();
  }

  public long getYear() {
    return year;
  }

  public static String idForYear(int year, String eventId) {
    return eventId + ":" + year;
  }

  public enum Status {
    ALIVE, DEAD
  }
}
