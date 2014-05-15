package com.lightpegasus.csv;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Handles parsing excel spreadsheets
 */
public class SpreadsheetParser implements Closeable, Iterator<CsvRow> {
  private final OPCPackage pkg;
  private final Sheet sheet;
  private final Iterator<Row> sheetIter;
  private final ImmutableBiMap<String, Integer> headerColumnMap;
  private final Set<String> dateColumns;

  public SpreadsheetParser(InputStream input, Collection<String> dateColumns)
      throws IOException, OpenXML4JException, SAXException {
    this.pkg = OPCPackage.open(input);

    XSSFWorkbook wb = new XSSFWorkbook(pkg);
    this.sheet = wb.getSheetAt(0);
    this.sheetIter = sheet.iterator();
    this.dateColumns = ImmutableSet.copyOf(dateColumns);

    ImmutableBiMap.Builder<String, Integer> headerMapBuilder = ImmutableBiMap.builder();
    if (sheetIter.hasNext()) {
      Row headerRow = sheetIter.next();

      for (Cell cell : headerRow) {
        headerMapBuilder.put(cell.getStringCellValue(), cell.getColumnIndex());
      }
    }
    this.headerColumnMap = headerMapBuilder.build();
  }

  public XMLReader fetchSheetParser(SharedStringsTable sst) throws SAXException {
    XMLReader parser =
        XMLReaderFactory.createXMLReader(
            "org.apache.xerces.parsers.SAXParser"
        );
    ContentHandler handler = new SheetHandler(sst);
    parser.setContentHandler(handler);
    return parser;
  }

  private static class SheetHandler extends DefaultHandler {
    private SharedStringsTable sst;
    private String lastContents;
    private boolean nextIsString;

    private int rowCount = 0;
    private int cellCount = 0;
    private int biggestCellCount = 0;

    private SheetHandler(SharedStringsTable sst) {
      this.sst = sst;
    }

    public void startElement(String uri, String localName, String name,
                             Attributes attributes) throws SAXException {
      if (name.equals("row")) {
        biggestCellCount = Math.max(cellCount, biggestCellCount);
        if (cellCount < biggestCellCount) {
          System.err.println(" cell count for row is off: " + cellCount + " vs " + biggestCellCount);
        }

        System.err.println("Rows: " + rowCount + " Cells: " + cellCount);
        rowCount++;
        cellCount = 0;
      }

      // c => cell
      if(name.equals("c")) {
        System.err.println("  cell " + cellCount);
        cellCount++;
        // Print the cell reference
//        System.out.print(attributes.getValue("r") + " - ");
        // Figure out if the value is an index in the SST
        String cellType = attributes.getValue("t");
        if(cellType != null && cellType.equals("s")) {
          nextIsString = true;
        } else {
          nextIsString = false;
        }
      }
      // Clear contents cache
      lastContents = "";
    }

    public void endElement(String uri, String localName, String name)
        throws SAXException {
      // Process the last contents as required.
      // Do now, as characters() may be called more than once
      if(nextIsString) {
        int idx = Integer.parseInt(lastContents);
        lastContents = new XSSFRichTextString(sst.getEntryAt(idx)).toString();
        nextIsString = false;
      }

      // v => contents of a cell
      // Output after we've seen the string contents
      if(name.equals("v")) {
        System.out.println(lastContents);
      }
    }

    public void characters(char[] ch, int start, int length)
        throws SAXException {
      lastContents += new String(ch, start, length);
    }
  }

  @Override
  public void close() throws IOException {
    pkg.close();
  }

  @Override
  public boolean hasNext() {
    return sheetIter.hasNext();
  }

  @Override
  public CsvRow next() {
    Row nextRow = sheetIter.next();
    String[] parsedRow = new String[headerColumnMap.size()];
    for (Cell cell : nextRow) {
      String columnHeader = headerColumnMap.inverse().get(cell.getColumnIndex());
      String cellValue = "";
      switch (cell.getCellType()) {
        case Cell.CELL_TYPE_STRING:
          cellValue = cell.getStringCellValue();
          break;
        case Cell.CELL_TYPE_BLANK:
          cellValue = "";
          break;
        case Cell.CELL_TYPE_BOOLEAN:
          cellValue = Boolean.toString(cell.getBooleanCellValue());
          break;
        case Cell.CELL_TYPE_NUMERIC:
          if (dateColumns.contains(columnHeader)) {
            cellValue = new SimpleDateFormat("MM/dd/yyyy hh:mm a").format(cell.getDateCellValue());
          } else {
            cellValue = BigDecimal.valueOf((int) cell.getNumericCellValue()).toPlainString();
          }
          break;
        default:
          throw new UnsupportedOperationException(
              "Don't know what to do with cell type " + cell.getCellType() + " cell: " + cell);
      }
      parsedRow[cell.getColumnIndex()] = cellValue;
    }

    return new CsvRow(headerColumnMap, parsedRow);
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Remove not supported for spreadsheet iter");
  }
}
