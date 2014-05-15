package com.lightpegasus.scheduler.gencon;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.lightpegasus.csv.CsvParser;
import com.lightpegasus.csv.CsvRow;
import com.lightpegasus.csv.LowMemorySpreadsheetParser;
import com.lightpegasus.csv.SpreadsheetParser;
import com.lightpegasus.scheduler.gencon.entity.GenconEvent;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.concurrent.Callable;

/**
 * Parser for the Gencon Schedules.
 */
public class GenconScheduleParser implements Closeable {
  private final GenconEventConverter eventConverter;

  public GenconScheduleParser(InputStream input, int year, final Function<GenconEvent, ?> callback)
      throws IOException, InvalidFormatException {
    switch (year) {
      case 2013:
        this.eventConverter = new Gencon2013Converter(input, callback);
        break;
      case 2014:
        this.eventConverter = new Gencon2014Converter(input, callback);
        break;
      default: throw new IllegalArgumentException("Unable to build a converter for " + year);
    }
  }

  public void parse() throws Exception {
    eventConverter.parse();
  }

  @Override
  public void close() throws IOException {
    eventConverter.close();
  }

  private abstract static class GenconEventConverter implements Closeable {
    public abstract GenconEvent convertRow(CsvRow row);
    public abstract void parse() throws Exception;
  }

  private static class Gencon2014Converter extends GenconEventConverter {
    private final LowMemorySpreadsheetParser spreadsheetParser;

    public Gencon2014Converter(InputStream input, final Function<GenconEvent, ?> callback)
        throws IOException, InvalidFormatException {
      this.spreadsheetParser = new LowMemorySpreadsheetParser(input, new Function<CsvRow, Void>() {
        @Override
        public Void apply(CsvRow input) {
          callback.apply(convertRow(input));

          return null;
        }
      });
    }

    public GenconEvent convertRow(CsvRow row) {
      GenconEvent event = new GenconEvent(2014, row.stringField("Game ID"));
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
      event.setLastModified(row.mdyDateTimeField("Last Modified"));

      event.updateHash();
      return event;
    }

    @Override
    public void parse() throws Exception {
      spreadsheetParser.parseInput();
    }

    @Override
    public void close() throws IOException {
      spreadsheetParser.close();
    }
  }

  private static class Gencon2013Converter extends GenconEventConverter {
    private final CsvParser csvParser;
    private final Function<GenconEvent, ?> callback;

    public Gencon2013Converter(InputStream input, Function<GenconEvent, ?> callback)
        throws IOException {
      this.csvParser = new CsvParser(new InputStreamReader(input));
      this.callback = callback;
    }

    public void parse() {
      while (csvParser.hasNext()) {
        callback.apply(convertRow(csvParser.next()));
      }
    }

    @Override
    public GenconEvent convertRow(CsvRow row) {
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

    @Override
    public void close() throws IOException {
      csvParser.close();
    }
  }
}
