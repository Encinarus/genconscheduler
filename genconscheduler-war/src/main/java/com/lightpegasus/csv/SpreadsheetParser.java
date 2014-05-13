package com.lightpegasus.csv;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Handles parsing xssf spreadsheets
 */
public class SpreadsheetParser implements Closeable, Iterator<CsvRow> {
  private final OPCPackage pkg;
  private final XSSFSheet sheet;
  private final Iterator<Row> sheetIter;
  private final ImmutableBiMap<String, Integer> headerColumnMap;
  private final Set<String> dateColumns;

  public SpreadsheetParser(InputStream input, Collection<String> dateColumns)
      throws IOException, InvalidFormatException {
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
