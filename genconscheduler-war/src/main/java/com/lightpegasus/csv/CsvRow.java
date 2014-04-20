package com.lightpegasus.csv;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
* Created by alek on 3/24/14.
*/
public class CsvRow {
  private final String[] parsedRow;
  private final ImmutableMap<String, Integer> headerColumnMap;

  public CsvRow(Map<String, Integer> headerColumnMap, String[] parsedRow) {
    this.headerColumnMap = ImmutableMap.copyOf(headerColumnMap);
    this.parsedRow = parsedRow;
  }

  // TODO(alek): Handle orNull, or maybe use optionals?
  public String stringField(String columnName) {
    String field = parsedRow[headerColumnMap.get(columnName)];
    // These quotes are problematic, so replace them
    field = field.replace('“', '"');
    field = field.replace('”', '"');
    return Strings.emptyToNull(field.trim());
  }

  public Integer intField(String columnName) {
    if (Strings.isNullOrEmpty(stringField(columnName))) {
      return null;
    }
    return Integer.parseInt(stringField(columnName));
  }

  public Integer multipliedIntegerField(String columnName, int factor) {
    if (Strings.isNullOrEmpty(stringField(columnName))) {
      return null;
    }

    return new BigDecimal(stringField(columnName))
        .multiply(BigDecimal.valueOf(factor))
        .intValue();
  }

  public BigDecimal bigDecimalField(String columnName) {
    if (Strings.isNullOrEmpty(stringField(columnName))) {
      return BigDecimal.ZERO;
    }

    return new BigDecimal(stringField(columnName));
  }

  public Boolean booleanField(String columnName) {
    return Boolean.valueOf(stringField(columnName));
  }

  public DateTime mdyDateTimeField(String columnName) {
    return dateTimeField(columnName, "MM/dd/yyyy hh:mm a", "America/Indiana/Indianapolis");
  }

  public DateTime ymdDateTimeField(String columnName) {
    return dateTimeField(columnName, "yyyy/MM/dd hh:mm a", "America/Indiana/Indianapolis");
  }

  public DateTime dateTimeField(String columnName, String format, String timezone) {
    DateTimeFormatter formatter = DateTimeFormat.forPattern(format);
    return formatter.parseDateTime(stringField(columnName)).withZoneRetainFields(
        DateTimeZone.forTimeZone(TimeZone.getTimeZone(timezone)));
  }

  public List<String> stringListField(String columnName) {
    String field = stringField(columnName);
    if (field == null) {
      return new ArrayList<>();
    }
    return Splitter.on(",").omitEmptyStrings().trimResults().splitToList(field);
  }
}
