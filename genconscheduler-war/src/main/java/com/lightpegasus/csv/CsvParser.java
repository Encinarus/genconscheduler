package com.lightpegasus.csv;

import com.google.common.collect.ImmutableMap;
import com.googlecode.jcsv.CSVStrategy;
import com.googlecode.jcsv.reader.CSVReader;
import com.googlecode.jcsv.reader.internal.CSVReaderBuilder;
import com.googlecode.jcsv.reader.internal.DefaultCSVEntryParser;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Wraps parsing a CSV file.
 */
public class CsvParser implements Closeable, Iterator<CsvRow> {
  private final CSVReader<String[]> csvReader;
  private final Map<String, Integer> headerColumnMap;
  private String[] nextRow;
  private int rowsRead = 0;

  public CsvParser(Reader fileReader) throws IOException {
    // UK_DEFAULT? Weird...
    this.csvReader = new CSVReaderBuilder<String[]>(fileReader)
        .entryParser(new DefaultCSVEntryParser())
        .strategy(CSVStrategy.UK_DEFAULT).build();

    String[] headerRow = csvReader.readNext();

    ImmutableMap.Builder<String, Integer> headerBuilder = ImmutableMap.builder();
    for (int i = 0; i < headerRow.length; i++) {
      headerBuilder.put(headerRow[i], i);
    }
    headerColumnMap = headerBuilder.build();

    // Set nextRow so hasNext passes
    nextRow = headerRow;
    next();
  }

  @Override
  public void close() throws IOException {
    csvReader.close();
  }

  @Override
  public boolean hasNext() {
    return nextRow != null;
  }

  @Override
  public CsvRow next() {
    if (hasNext()) {
      String[] cachedNext = nextRow;
      try {
        nextRow = csvReader.readNext();
        rowsRead++;
      } catch (IOException e) {
        nextRow = null;
        throw new RuntimeException("Exception while reading row #" + rowsRead, e);
      }
      return new CsvRow(headerColumnMap, cachedNext);
    } else {
      throw new NoSuchElementException();
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("remove not supported");
  }
}
