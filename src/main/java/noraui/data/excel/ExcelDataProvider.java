package noraui.data.excel;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import noraui.data.CommonDataProvider;
import noraui.data.DataInputProvider;
import noraui.data.DataOutputProvider;
import noraui.exception.TechnicalException;
import noraui.exception.data.EmptyDataFileContentException;
import noraui.exception.data.WrongDataFileFormatException;
import noraui.utils.Constants;
import noraui.utils.Messages;

public abstract class ExcelDataProvider extends CommonDataProvider implements DataInputProvider, DataOutputProvider {

    public static final String EXCEL_TYPE = "xlsx";

    private Workbook workbook;

    private CellStyle styleSuccess;
    private CellStyle styleFailed;

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNbLines() throws TechnicalException {
        int count = 0;
        Iterator<Row> rowsIterator = workbook.getSheetAt(0).iterator();
        Row row;
        while (rowsIterator.hasNext()) {
            row = rowsIterator.next();
            if (!"".equals(readCell(row.getCell(0)))) {
                count++;
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeFailedResult(int line, String value) throws TechnicalException {
        logger.debug("writeFailedResult => line:" + line + " value:" + value);
        writeValue(NAME_OF_RESULT_COLUMN, line, value, styleFailed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeSuccessResult(int line) throws TechnicalException {
        logger.debug("writeSuccessResult => line:" + line);
        writeValue(NAME_OF_RESULT_COLUMN, line, Messages.SUCCESS_MESSAGE, styleSuccess);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeDataResult(String column, int line, String value) throws TechnicalException {
        logger.debug("writeDataResult => column: " + column + " line:" + line + " value:" + value);
        writeValue(column, line, value, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String readValue(String column, int line) throws TechnicalException {
        int colIndex = columns.indexOf(column);
        Sheet sheet = workbook.getSheetAt(0);
        Row row = sheet.getRow(line);
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return "";
        } else {
            return readCell(cell);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] readLine(int line, boolean readResult) throws TechnicalException {
        Sheet sheet = workbook.getSheetAt(0);
        Row row = sheet.getRow(line);
        if (row == null || "".equals(readCell(row.getCell(0)))) {
            return null;
        } else {
            String[] ret = readResult ? new String[columns.size()] : new String[columns.size() - 1];
            Cell cell;
            for (int i = 0; i < ret.length; i++) {
                if ((cell = row.getCell(i)) == null) {
                    ret[i] = "";
                } else {
                    ret[i] = readCell(cell);
                }
            }
            return ret;
        }
    }

    /**
     * @throws EmptyDataFileContentException
     *             if data is empty
     * @throws WrongDataFileFormatException
     *             if data is wrong
     */
    protected void initColumns() throws EmptyDataFileContentException, WrongDataFileFormatException {
        columns = new ArrayList<>();
        Sheet sheet = workbook.getSheetAt(0);
        Row row = sheet.getRow(0);
        Cell cell;
        for (int i = 0; (cell = row.getCell(i)) != null; i++) {
            columns.add(cell.getStringCellValue());
        }
        if (columns.size() < 2) {
            throw new EmptyDataFileContentException("Input data file is empty or only result column is provided.");
        }
        if (!columns.get(columns.size() - 1).equals(NAME_OF_RESULT_COLUMN)) {
            throw new WrongDataFileFormatException("The last column of the data file must be '" + NAME_OF_RESULT_COLUMN + "'.");
        }
    }

    /**
     * @throws TechnicalException
     *             is throws if you have a technical error (format, configuration, datas, ...) in NoraUi.
     */
    protected void openInputData() throws TechnicalException {
        try (FileInputStream fileIn = new FileInputStream(dataInPath + scenarioName + "." + EXCEL_TYPE);) {
            // Check extension
            switch (EXCEL_TYPE) {
                case "xlsx":
                    workbook = new XSSFWorkbook(fileIn);
                    XSSFFormulaEvaluator.evaluateAllFormulaCells((XSSFWorkbook) workbook);
                    break;
                case "xls":
                    workbook = new HSSFWorkbook(fileIn);
                    HSSFFormulaEvaluator.evaluateAllFormulaCells(workbook);
                    break;
                default:
                    logger.error("Wrong data file type input: " + EXCEL_TYPE);
            }
        } catch (IOException e) {
            logger.error(TechnicalException.TECHNICAL_ERROR_MESSAGE_DATA_IOEXCEPTION, e);
            throw new TechnicalException(TechnicalException.TECHNICAL_ERROR_MESSAGE_DATA_IOEXCEPTION, e);
        }
    }

    /**
     * @throws TechnicalException
     *             is throws if you have a technical error (format, configuration, datas, ...) in NoraUi.
     */
    void openOutputData() throws TechnicalException {
        try (FileInputStream fileOut = new FileInputStream(dataOutPath + scenarioName + "." + EXCEL_TYPE);) {
            switch (EXCEL_TYPE) {
                case "xlsx":
                    workbook = new XSSFWorkbook(fileOut);
                    break;
                case "xls":
                    workbook = new HSSFWorkbook(fileOut);
                    break;
                default:
                    logger.error("Wrong data file type input: " + EXCEL_TYPE);
            }
        } catch (IOException e) {
            logger.error(TechnicalException.TECHNICAL_ERROR_MESSAGE_DATA_IOEXCEPTION, e);
            throw new TechnicalException(TechnicalException.TECHNICAL_ERROR_MESSAGE_DATA_IOEXCEPTION, e);
        }

        styleSuccess = workbook.createCellStyle();
        Font fontSuccess = workbook.createFont();
        fontSuccess.setColor(HSSFColor.GREEN.index);
        styleSuccess.setFont(fontSuccess);

        styleFailed = workbook.createCellStyle();
        Font fontFailed = workbook.createFont();
        fontFailed.setColor(HSSFColor.RED.index);
        styleFailed.setFont(fontFailed);
    }

    /**
     * @param column
     * @param line
     * @param value
     * @param style
     */
    private void writeValue(String column, int line, String value, CellStyle style) {
        logger.debug("Writing: " + value + " at line " + line + " in column '" + column + "'");
        int colIndex = columns.indexOf(column);
        Sheet sheet = workbook.getSheetAt(0);
        Row row = sheet.getRow(line);
        Cell cell = row.getCell(colIndex);
        if (cell != null) {
            row.removeCell(cell);
        }
        cell = row.createCell(colIndex);
        cell.setCellStyle(style);
        cell.setCellValue(value);
        saveOpenExcelFile();
    }

    /**
     * @param cell
     * @return
     */
    private String readCell(Cell cell) {
        String txt = "";
        if (cell != null) {
            logger.debug("readCellByType with type: " + cell.getCellType());
            txt = readCellByType(cell, cell.getCellType());
        }
        return txt;
    }

    /**
     * readCellByType is use because CELL_TYPE_FORMULA can be CELL_TYPE_NUMERIC, CELL_TYPE_NUMERIC(date) or CELL_TYPE_STRING.
     *
     * @param cellcell
     *            read (line and column)
     * @param type
     *            CELL_TYPE_FORMULA can be CELL_TYPE_NUMERIC, CELL_TYPE_NUMERIC(date) or CELL_TYPE_STRING
     * @return a string with the data (evalued if CELL_TYPE_FORMULA)
     */
    private String readCellByType(Cell cell, int type) {
        String txt = "";
        if (cell != null) {
            switch (type) {
                case Cell.CELL_TYPE_NUMERIC:
                    txt = dateOrNumberProcessing(cell);
                    break;
                case Cell.CELL_TYPE_STRING:
                    txt = String.valueOf(cell.getRichStringCellValue());
                    logger.debug("CELL_TYPE_STRING: " + txt);
                    break;
                case Cell.CELL_TYPE_FORMULA:
                    txt = readCellByType(cell, cell.getCachedFormulaResultType());
                    logger.debug("CELL_TYPE_FORMULA: " + txt);
                    break;
                case Cell.CELL_TYPE_BLANK:
                    logger.debug("CELL_TYPE_BLANK (we do nothing)");
                    break;
                default:
                    logger.error(String.format("The cell type \"%s\" is not supported in readCellByType methode (0, 1, 2 and 3 only).", type));
                    break;
            }
        }
        return txt;
    }

    /**
     * @param cell
     * @return
     */
    private String dateOrNumberProcessing(Cell cell) {
        String txt;
        if (DateUtil.isCellDateFormatted(cell)) {
            DateFormat formatter = new SimpleDateFormat(Constants.DATE_FORMAT);
            txt = String.valueOf(formatter.format(cell.getDateCellValue()));
            logger.debug("CELL_TYPE_NUMERIC (date): " + txt);
        } else {
            txt = String.valueOf(cell.getNumericCellValue());
            logger.debug("CELL_TYPE_NUMERIC: " + txt);
        }
        return txt;
    }

    private void saveOpenExcelFile() {
        try (FileOutputStream fileOut = new FileOutputStream(dataOutPath + scenarioName + "." + EXCEL_TYPE);) {
            workbook.write(fileOut);
        } catch (IOException e) {
            logger.error("ERROR in saveOpenExcelFile: " + e);
        }
    }

}