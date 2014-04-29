package com.lightpegasus.scheduler.gencon;

import com.google.common.base.Function;
import com.lightpegasus.csv.CsvParser;
import com.lightpegasus.csv.CsvRow;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

/**
 * Parser for the Gencon Schedules.
 */
public class GenconScheduleParser implements Iterable<GenconEvent>, Closeable {
  private final CsvParser csvParser;
  private final Function<CsvRow, GenconEvent> eventConverter;

  public GenconScheduleParser(Reader input, int year) throws IOException {
    this.csvParser = new CsvParser(input);
    switch (year) {
      case 2013: this.eventConverter = new Gencon2013Converter(); break;
      default: throw new IllegalArgumentException("Unable to build a converter for " + year);
    }
  }

  @Override
  public void close() throws IOException {
    csvParser.close();
  }

  @Override
  public Iterator<GenconEvent> iterator() {
    return new Iterator<GenconEvent>(){
      @Override
      public boolean hasNext() {
        return csvParser.hasNext();
      }

      @Override
      public GenconEvent next() {
        return nextEvent();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("Remove isn't supported");
      }
    };
  }

  private GenconEvent nextEvent() {
    return eventConverter.apply(csvParser.next());
  }

  private static class Gencon2013Converter implements Function<CsvRow, GenconEvent> {
    @Override
    public GenconEvent apply(CsvRow row) {
      GenconEvent event = new GenconEvent(2013, row.stringField("Game ID"));
      event.setGroup(row.stringField("Group"));
      event.setTitle(row.stringField("Title"));
      event.setShortDescription(row.stringField("Short Description"));
      event.setLongDescription(row.stringField("Long Description"));
      event.setEventType(row.stringField("Event Type"));
      event.setGameSystem(row.stringField("Game System"));
      event.setRulesEdition(row.stringField("Rules Edition"));
      event.setMinimumPlayers(row.intField("Minimum Players"));
      event.setMaximumPlayers(row.intField("Maximum Players"));
      event.setAgeRequired(row.stringField("Age Required"));
      event.setExperienceRequired(row.stringField("Experience Required"));
      event.setMaterialsProvided(row.booleanField("Materials Provided"));
      event.setStartTime(row.mdyDateTimeField("Start Date & Time"));
      event.setDuration(row.multipliedIntegerField("Duration", 60));
      event.setEndTime(row.mdyDateTimeField("End Date & Time"));
      event.setGmNames(row.stringListField("GM Names"));
      event.setWebsite(row.stringField("Website"));
      event.setEmail(row.stringField("Email"));
      event.setTournament(row.booleanField("Tournament?"));
      event.setRoundNumber(row.intField("Round Number"));
      event.setTotalRounds(row.intField("Total Rounds"));
      event.setMinimumPlayTime(row.multipliedIntegerField("Minimum Play Time", 60));
      event.setAttendeeRegistration(row.booleanField("Attendee Registration?"));
      event.setCost(row.bigDecimalField("Cost $"));
      event.setLocation(row.stringField("Location"));
      event.setRoomName(row.stringField("Room Name"));
      event.setTableNumber(row.stringField("Table Number"));
      event.setSpecialCategory(row.stringField("Special Category"));
      event.setTicketsAvailable(row.intField("Tickets Available"));
      event.setLastModified(row.ymdDateTimeField("Last Modified"));

      event.updateHash();
      return event;
    }
  }
}
