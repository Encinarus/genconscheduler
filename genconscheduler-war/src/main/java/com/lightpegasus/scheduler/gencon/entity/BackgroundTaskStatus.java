package com.lightpegasus.scheduler.gencon.entity;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import java.util.TimeZone;

/**
 * Represents the most recent time synced from the gencon schedule.
 */
@Entity
public class BackgroundTaskStatus {
  public enum TaskType {
    UPDATE_EVENTS,      // For updating the datastore with new events.
    NOTIFY_ON_CHANGES,  // For informing folks that events have been changed.
  }

  @Id long year;
  @Index TaskType taskType;
  DateTime syncTime;

  public BackgroundTaskStatus() {

  }

  public BackgroundTaskStatus(int year, TaskType taskType) {
    this.year = year;
    this.syncTime = new DateTime(year, 1, 1, 1, 1, DateTimeZone.UTC);
    this.taskType = taskType;
  }

  public void setSyncTime(DateTime syncTime) {
    this.syncTime = syncTime;
  }

  public DateTime getSyncTime() {
    return syncTime;
  }

  public String getReadableSyncTime() {
    return DateTimeFormat.forPattern("EEE MMMM dd hh:mm:ss a zzz").print(
        syncTime.withZone(DateTimeZone.forTimeZone(
            TimeZone.getTimeZone("America/Indiana/Indianapolis"))));
  }

  public long getYear() {
    return year;
  }
}
