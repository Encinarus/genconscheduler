package com.lightpegasus.scheduler.gencon.entity;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import org.joda.time.DateTime;

/**
 * Represents something someone searched for and how many results it had.
 */
@Entity
@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class SearchQuery {
  @Id private Long queryId;

  private DateTime searchTime;
  private String searchQuery;
  private int resultCount;

  public SearchQuery() {

  }

  public SearchQuery(String searchQuery, int resultCount) {
    this.searchQuery = searchQuery;
    this.resultCount = resultCount;
    this.searchTime = DateTime.now();
  }
}
