package com.lightpegasus.csv;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * This is intended to be a low-memory overhead parser for the gencon excel
 * spreadsheet. Unfortunately, loading the entire thing into memory takes
 * more mem than we have in appengine, so the instance falls over. This tries
 * to mitigate that by processing the records as they come in.
 *
 * Oh, and this is specialized to gencon... I doubt it'll generalize well.
 */
public class LowMemorySpreadsheetParser implements Closeable {
  public static void main(String[] args) throws Exception {
    LowMemorySpreadsheetParser lowMemorySpreadsheetParser = new LowMemorySpreadsheetParser(
        new FileInputStream("/Users/alek/projects/genconscheduler/genconscheduler-war/src/main/"
            + "webapp/WEB-INF/schedules/events.may.9.2014.xlsx"
    ), new Function<CsvRow, Void>() {
      @Override
      public Void apply(CsvRow row) {
        System.out.println("Row :: " + row);
        return null;
      }
    });

    lowMemorySpreadsheetParser.parseInput();
  }

  private final InputStream inputStream;
  private final Function<CsvRow, ?> callback;

  @Override
  public void close() throws IOException {
    inputStream.close();
  }

  public LowMemorySpreadsheetParser(InputStream inputStream, Function<CsvRow, ?> callback) {
    this.inputStream = inputStream;
    this.callback = callback;
  }

  public void parseInput() throws IOException, OpenXML4JException, SAXException {
    OPCPackage pkg = OPCPackage.open(inputStream);

    XSSFReader reader = new XSSFReader(pkg);

    SharedStringsTable sharedStringsTable = reader.getSharedStringsTable();

    XMLReader parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");

    ContentHandler handler = new SheetHandler(sharedStringsTable, callback);
    parser.setContentHandler(handler);

    List<InputStream> streams = Lists.newArrayList(reader.getSheetsData());
    InputSource inputSource = new InputSource(streams.get(0));

    parser.parse(inputSource);
  }

  /**
   * See org.xml.sax.helpers.DefaultHandler javadocs
   */
  private static class SheetHandler extends DefaultHandler {
    private final Function<CsvRow, ?> callback;
    private SharedStringsTable sst;
    private String lastContents;
    private int lastColumnIndex = 0;
    private ArrayList<String> parsedCells;

    private ImmutableBiMap<String, Integer> headerColumnMap = null;

    private enum CellType {
      DATE,
      INLINE_STRING,
      NUMBER,
      SHARED_STRING,
      ;

      static CellType getCellType(Attributes attributes) {
        CellType localCellType = null;

        String cellTypeAttribute = attributes.getValue("t");

        if ("2".equals(attributes.getValue("s")) && cellTypeAttribute == null) {
          localCellType = CellType.DATE;
        } else if ("n".equals(cellTypeAttribute)) {
          localCellType = CellType.NUMBER;
        } else if ("inlineStr".equals(cellTypeAttribute)) {
          localCellType = CellType.INLINE_STRING;
        } else if ("s".equals(cellTypeAttribute)) {
          localCellType = CellType.SHARED_STRING;
        }
        return localCellType;
      }
    }

    private CellType cellType;

    private SheetHandler(SharedStringsTable sst, Function<CsvRow, ?> callback) {
      this.sst = sst;
      this.callback = callback;
    }

    public void startElement(String uri, String localName, String name,
                             Attributes attributes) throws SAXException {
      // c => cell
      if(name.equals("c")) {
        cellType = CellType.getCellType(attributes);
        String cellReference = attributes.getValue("r");
        int columnIndex = CellReference.convertColStringToIndex(cellReference.replaceAll("\\d", ""));

        for (; lastColumnIndex < columnIndex; lastColumnIndex++) {
          parsedCells.add("");
        }
      } else if ("row".equals(name)) {
        parsedCells = new ArrayList<>();
        lastColumnIndex = 0;
      }
      // Clear contents cache
      lastContents = "";
    }

    public void endElement(String uri, String localName, String name)
        throws SAXException {
      // Process the last contents as required.
      // Do now instead of in characters() as that may be called more than once

      if ("row".equals(name)) {
        if (headerColumnMap == null) {
          ImmutableBiMap.Builder<String, Integer> builder = ImmutableBiMap.builder();
          for (int i = 0; i < parsedCells.size(); i++) {
            builder.put(parsedCells.get(i), i);
          }
          headerColumnMap = builder.build();
        } else {
          callback.apply(new CsvRow(headerColumnMap,
              parsedCells.toArray(new String[parsedCells.size()])));
        }
        return;
      } else if (cellType != null && "c".equals(name)) {
        lastColumnIndex++;
        switch(cellType) {
          case SHARED_STRING: {
            parsedCells.add(
                new XSSFRichTextString(sst.getEntryAt(Integer.parseInt(lastContents)))
                    .toString());
          } break;
          case NUMBER:
            parsedCells.add(new BigDecimal(lastContents).toPlainString());
            break;
          case INLINE_STRING:
            parsedCells.add(lastContents);
            break;
          case DATE:
            parsedCells.add(
                new SimpleDateFormat("MM/dd/yyyy hh:mm a").format(
                    DateUtil.getJavaDate(Double.parseDouble(lastContents))));
            break;
        }

        cellType = null;
      }
    }

    public void characters(char[] ch, int start, int length)
        throws SAXException {
      lastContents += new String(ch, start, length);
    }
  }
}
