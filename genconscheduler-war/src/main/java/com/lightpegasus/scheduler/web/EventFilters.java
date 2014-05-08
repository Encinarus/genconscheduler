package com.lightpegasus.scheduler.web;

import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import org.joda.time.DateTimeConstants;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 */
public class EventFilters {
  public static List<GenconEvent> sortByStartTime(Iterable<GenconEvent> events) {
    List<GenconEvent> sortableEvents = Lists.newArrayList(events);
    Collections.sort(sortableEvents, new Comparator<GenconEvent>() {
      @Override
      public int compare(GenconEvent first, GenconEvent second) {
        return Ordering.natural().compare(first.getStartTime(), second.getStartTime());
      }
    });

    return sortableEvents;
  }

  public static Multimap<Long, GenconEvent> clusterEvents(Collection<GenconEvent> events) {
    ImmutableMultimap.Builder<Long, GenconEvent> clusters = ImmutableMultimap.builder();

    for (GenconEvent event : events) {
      clusters.put(event.getClusterHash(), event);
    }

    return clusters.build();
  }

  public static ImmutableMultimap<String, GenconEvent> eventsByDay(Collection<GenconEvent> events) {
    ImmutableMultimap.Builder<String, GenconEvent> partitionBuilder = ImmutableMultimap.builder();

    partitionBuilder.putAll("Wednesday", DayFilter.WEDNESDAY.filterSorted(events));
    partitionBuilder.putAll("Thursday", DayFilter.THURSDAY.filterSorted(events));
    partitionBuilder.putAll("Friday", DayFilter.FRIDAY.filterSorted(events));
    partitionBuilder.putAll("Saturday", DayFilter.SATURDAY.filterSorted(events));
    partitionBuilder.putAll("Sunday", DayFilter.SUNDAY.filterSorted(events));

    return partitionBuilder.build();
  }

  public static ImmutableMultimap<String, GenconEvent> eventsByCategory(
      Collection<GenconEvent> events) {
    ListMultimap<String, GenconEvent> partitions = ArrayListMultimap.create();

    for (GenconEvent event : events) {
      partitions.put(event.getEventType(), event);
    }

    // New arrayList over the keys? Yes, to avoid concurrent modification exceptions while iterating
    for (String eventType : Lists.newArrayList(partitions.keys())) {
      List<GenconEvent> unsortedEvents = partitions.removeAll(eventType);
      partitions.putAll(eventType, sortByStartTime(unsortedEvents));
    }

    return ImmutableMultimap.copyOf(partitions);
  }


  public enum DayFilter implements Predicate<GenconEvent> {
    WEDNESDAY(DateTimeConstants.WEDNESDAY),
    THURSDAY(DateTimeConstants.THURSDAY),
    FRIDAY(DateTimeConstants.FRIDAY),
    SATURDAY(DateTimeConstants.SATURDAY),
    SUNDAY(DateTimeConstants.SUNDAY);

    private final int dayOfWeek;

    DayFilter(int dayOfWeek) {
      this.dayOfWeek = dayOfWeek;
    }

    @Override
    public boolean apply(GenconEvent input) {
      return input.getDayOfWeek() == dayOfWeek;
    }

    public Iterable<GenconEvent> filterSorted(Iterable<GenconEvent> events) {
      return sortByStartTime(Iterables.filter(events, this));
    }
  }
}
