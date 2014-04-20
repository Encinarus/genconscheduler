package com.lightpegasus.scheduler.gencon.entity;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;

/**
 * Represents the most recent time synced from the gencon schedule.
 */
@Entity
public class SyncStatus {
  @Id int year;

  DateTime syncTime;

  public SyncStatus() {

  }

  public SyncStatus(int year) {
    this.year = year;
    this.syncTime = new DateTime(year, 1, 1, 1, 1, DateTimeZone.UTC);
  }

  public void setSyncTime(DateTime syncTime) {
    this.syncTime = syncTime;
  }

  public DateTime getSyncTime() {
    return syncTime;
  }

  public int getYear() {
    return year;
  }
}
