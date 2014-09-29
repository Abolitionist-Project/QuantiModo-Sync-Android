package com.quantimodo.etl.readers;

import com.quantimodo.etl.ArrayDatabaseView;
import com.quantimodo.etl.ArrayTable;
import com.quantimodo.etl.DatabaseView;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.IOException;

public class SpreadsheetReader implements Reader {
    public static final SpreadsheetReader instance = new SpreadsheetReader();

    // Disable default constructor.
    private SpreadsheetReader() {
    }

    public DatabaseView getDatabaseView(final CharSequence filename) throws IOException {
        return getDatabaseView(new File(filename.toString()));
    }

    public DatabaseView getDatabaseView(final File file) throws IOException {
        try {
            final Workbook workbook = WorkbookFactory.create(file);
            try {
                workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            } catch (final NotImplementedException e) {
            }

            final int tableCount = workbook.getNumberOfSheets();

            final String[] tableNames = new String[tableCount];
            final ArrayTable[] tables = new ArrayTable[tableCount];
            for (int tableNumber = 0; tableNumber < tableCount; tableNumber++) {
                final String tableName = workbook.getSheetName(tableNumber);
                tableNames[tableNumber] = tableName;
                final Sheet table = workbook.getSheetAt(tableNumber);

                // Find bounds information.
                int minRow = -1;
                int rowCount = 0;

                int minColumn = Integer.MAX_VALUE;
                int maxColumn = Integer.MIN_VALUE;

                for (final Row row : table) {
                    final int rowNumber = row.getRowNum();

                    if (minRow == -1) {
                        minRow = rowNumber;
                    }

                    boolean emptyRow = true;
                    for (final Cell cell : row) {
                        final int cellType = cell.getCellType();
                        if ((cellType == Cell.CELL_TYPE_BOOLEAN) || (cellType == Cell.CELL_TYPE_NUMERIC) || ((cellType == Cell.CELL_TYPE_STRING) && (cell.getStringCellValue().trim().length() != 0))) {
                            emptyRow = false;
                            break;
                        }
                    }

                    if (!emptyRow) {
                        for (final Cell cell : row) {
                            final int columnNumber = cell.getColumnIndex();
                            minColumn = Math.min(minColumn, columnNumber);
                            maxColumn = Math.max(maxColumn, columnNumber);
                        }

                        rowCount++;
                    }
                }

                if (maxColumn == Integer.MIN_VALUE) {
                    tables[tableNumber] = ArrayTable.makeEmptyTable(tableName);
                    continue;
                }

                int columnCount = maxColumn - minColumn + 1;

                // Get header information.
                final String[] headerRow = new String[columnCount];
                boolean hasHeaderRow = true;
                {
                    final Row topRow = table.getRow(minRow);

                    for (int column = minColumn; column <= maxColumn; column++) {
                        Cell cell = topRow.getCell(column);
                        if ((cell == null) || (cell.getCellType() != Cell.CELL_TYPE_STRING)) {
                            hasHeaderRow = false;
                            break;
                        }
                    }

                    if (hasHeaderRow) {
                        for (int column = minColumn; column <= maxColumn; column++) {
                            headerRow[column - minColumn] = topRow.getCell(column).getStringCellValue();
                        }
                        rowCount--;
                    } else {
                        for (int column = 0; column < columnCount; column++) {
                            headerRow[column] = "";
                        }
                    }
                }

                final Object[][] dataRows = new Object[rowCount][columnCount];
                {
                    boolean headerRowNotSeen = hasHeaderRow;
                    int rowNumber = 0;
                    for (final Row row : table) {
                        if (rowNumber >= rowCount) {
                            break;
                        }
                        if (!headerRowNotSeen) {
                            for (final Cell cell : row) {
                                final int columnNumber = cell.getColumnIndex() - minColumn;
                                int cellType = cell.getCellType();
                                if (cellType == Cell.CELL_TYPE_FORMULA) {
                                    cellType = cell.getCachedFormulaResultType();
                                }
                                switch (cellType) {
                                    case Cell.CELL_TYPE_BOOLEAN:
                                        dataRows[rowNumber][columnNumber] = cell.getBooleanCellValue();
                                        break;
                                    case Cell.CELL_TYPE_NUMERIC:
                                        dataRows[rowNumber][columnNumber] = DateUtil.isCellDateFormatted(cell) ? cell.getDateCellValue() : cell.getNumericCellValue();
                                        break;
                                    case Cell.CELL_TYPE_STRING:
                                        String value = cell.getStringCellValue();
                                        if (value.trim().length() != 0) {
                                            dataRows[rowNumber][columnNumber] = value;
                                        }
                                        break;
                                }
                            }
                            boolean emptyRow = true;
                            for (int columnNumber = 0; columnNumber < columnCount; columnNumber++) {
                                if (dataRows[rowNumber][columnNumber] != null) {
                                    emptyRow = false;
                                    break;
                                }
                            }
                            if (!emptyRow) {
                                rowNumber++;
                            }
                        }
                        headerRowNotSeen = false;
                    }
                }

                tables[tableNumber] = new ArrayTable(tableName, headerRow, dataRows);
            }

            return new ArrayDatabaseView(tableNames, tables);
        } catch (final InvalidFormatException e) {
            throw new IOException(e);
        }
    }
}
