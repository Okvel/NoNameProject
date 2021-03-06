package by.bsuir.spp.autoservice.service;

import by.bsuir.spp.autoservice.entity.*;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class DocumentService {
    private static final String FONT_FILE = "font/arial.ttf";
    private static final String DATE_FORMAT = "dd.MM.yyyy";
    private static final String PASSING_ACT_DOCUMENT = DocumentType.PASSING_ACT.getPath() + DocumentFormat.XLSX.getFormat();
    private static final String ACCEPTANCE_ACT_DOCUMENT = DocumentType.ACCEPTANCE_ACT.getPath() + DocumentFormat.XLSX.getFormat();
    private static final String WORK_PERFORMED_SHEET_DOCUMENT = DocumentType.WORK_PERFORMED_SHEET.getPath() +
            DocumentFormat.XLSX.getFormat();
    private static final String ORDER_DETAILS_APPLICATION = DocumentType.ORDER_DETAILS_APPLICATION.getPath() +
            DocumentFormat.XLSX.getFormat();
    private static final String PARTS_INVOICE_DOCUMENT = DocumentType.PARTS_INVOICE.getPath() + DocumentFormat.XLSX.getFormat();

    private static DocumentService instance = new DocumentService();
    private static CellStyle defaultStyle;
    private static CellStyle headerStyle;
    private static CellStyle dateStyle;

    private DocumentService() {}

    public static DocumentService getInstance() {
        return instance;
    }

    private void createCellStyle(Workbook workbook) {
        defaultStyle = workbook.createCellStyle();
        defaultStyle.setBorderBottom(CellStyle.BORDER_THIN);
        defaultStyle.setBorderLeft(CellStyle.BORDER_THIN);
        defaultStyle.setBorderTop(CellStyle.BORDER_THIN);
        defaultStyle.setBorderRight(CellStyle.BORDER_THIN);
        defaultStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        defaultStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);

        headerStyle = workbook.createCellStyle();
        headerStyle.setBorderBottom(CellStyle.BORDER_THIN);
        headerStyle.setBorderLeft(CellStyle.BORDER_THIN);
        headerStyle.setBorderTop(CellStyle.BORDER_THIN);
        headerStyle.setBorderRight(CellStyle.BORDER_THIN);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        headerStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);

        dateStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        dateStyle.setDataFormat(format.getFormat("dd.MM.yyyy"));
        dateStyle.setBorderBottom(CellStyle.BORDER_THIN);
        dateStyle.setBorderLeft(CellStyle.BORDER_THIN);
        dateStyle.setBorderTop(CellStyle.BORDER_THIN);
        dateStyle.setBorderRight(CellStyle.BORDER_THIN);
        dateStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        dateStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
    }

    private String saveGeneratedDocument(DocumentType type, DocumentFormat format) throws ServiceException {
        String result = null;
        switch (format) {
            case XLSX:
                result = type.getPath() + format.getFormat();
                break;
            case CSV:
                saveCsv(type.getPath());
                result = type.getPath() + format.getFormat();
                break;
            case PDF:
                savePdf(type.getPath());
                result = type.getPath() + format.getFormat();
                break;
        }

        return result;
    }

    private void saveCsv(String fileName) throws ServiceException {
        File outFile = new File(fileName + DocumentFormat.CSV.getFormat());
        File inputFile = new File(fileName + DocumentFormat.XLSX.getFormat());
        StringBuilder data = new StringBuilder();

        try (
                FileOutputStream fos = new FileOutputStream(outFile);
                XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(inputFile));
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fos, "Windows-1251")
                ) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            Cell cell;
            for (Row row : sheet) {
                Iterator<Cell> cellIterator = row.cellIterator();
                while (cellIterator.hasNext()) {
                    cell = cellIterator.next();
                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_BOOLEAN:
                            data.append(cell.getBooleanCellValue()).append(";");
                            break;
                        case Cell.CELL_TYPE_NUMERIC:
                            data.append(cell.getNumericCellValue()).append(";");
                            break;
                        case Cell.CELL_TYPE_STRING:
                            data.append(cell.getStringCellValue()).append(";");
                            break;
                        default:
                            data.append(cell).append(";");
                    }
                }
                data.append("\r\n");
            }
            outputStreamWriter.write(data.toString());
            outputStreamWriter.flush();
        } catch (IOException ex) {
            throw new ServiceException(ex);
        }
    }

    private void savePdf(String fileName) throws ServiceException {
        File outFile = new File(fileName + DocumentFormat.PDF.getFormat());
        File inputFile = new File(fileName + DocumentFormat.XLSX.getFormat());

        try (
                FileInputStream fileInputStream = new FileInputStream(inputFile);
                XSSFWorkbook workbook = new XSSFWorkbook(fileInputStream);
                FileOutputStream fos = new FileOutputStream(outFile)
                ) {
            BaseFont baseFont = BaseFont.createFont(FONT_FILE, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            Font font = new Font(baseFont);
            XSSFSheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            Document pdfDocument = new Document();
            PdfWriter.getInstance(pdfDocument, fos);
            pdfDocument.open();
            PdfPTable table = null;

            PdfPCell tableCell;
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Iterator<Cell> cellIterator = row.iterator();
                while (cellIterator.hasNext()) {
                    if (table == null) {
                        table = new PdfPTable(iteratorSize(cellIterator));
                        cellIterator = row.iterator();
                    }
                    Cell cell = cellIterator.next();
                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_BOOLEAN:
                            tableCell = new PdfPCell( new Phrase(String.valueOf(cell.getBooleanCellValue())));
                            table.addCell(tableCell);
                        case Cell.CELL_TYPE_STRING:
                            table.addCell(new Paragraph(cell.getStringCellValue(), font));
                            break;
                        case Cell.CELL_TYPE_NUMERIC:
                            if (cell.getNumericCellValue() > 42000) {
                                SimpleDateFormat sf = new SimpleDateFormat(DATE_FORMAT);
                                String formattedDate = sf.format(cell.getDateCellValue());
                                tableCell = new PdfPCell(new Phrase(formattedDate));
                                table.addCell(tableCell);
                            } else {
                                table.addCell(String.valueOf(cell.getNumericCellValue()));
                            }
                            break;
                        default:
                            tableCell = new PdfPCell(new Phrase(" "));
                            table.addCell(tableCell);
                    }
                }
            }
            pdfDocument.add(table);
            pdfDocument.close();
        } catch (IOException | DocumentException ex) {
            throw new ServiceException(ex);
        }
    }

    private int iteratorSize(Iterator iterator) {
        int size = 0;
        for (; iterator.hasNext();) {
            iterator.next();
            size++;
        }

        return size;
    }

    private void createPassingActDocument() throws ServiceException {
        try (
                FileOutputStream outputStream = new FileOutputStream(PASSING_ACT_DOCUMENT);
                XSSFWorkbook workbook = new XSSFWorkbook()
                ) {
            int rowCount = 0;
            createCellStyle(workbook);
            Sheet sheet = workbook.createSheet("Car passing act");
            String[] headers = {"Номер акта",
                    "ФИО администратора",
                    "ФИО клиента",
                    "Марка и модель автомобиля",
                    "Дата",
                    "Описание"};
            Row row = sheet.createRow(rowCount++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = row.createCell(i);
                cell.setCellStyle(headerStyle);
                cell.setCellValue(headers[i]);
            }

            List<Act> acts = ActService.getInstance().findAllPassingActs();
            fillActDocument(acts, sheet, rowCount, headers.length);
            workbook.write(outputStream);
        } catch (IOException ex) {
            throw new ServiceException(ex);
        }
    }

    private void createAcceptanceActDocument() throws ServiceException {
        try (
                FileOutputStream outputStream = new FileOutputStream(ACCEPTANCE_ACT_DOCUMENT);
                XSSFWorkbook workbook = new XSSFWorkbook()
        ) {
            int rowCount = 0;
            createCellStyle(workbook);
            Sheet sheet = workbook.createSheet("Acceptance car act");
            String[] headers = {"Номер акта",
                    "ФИО администратора",
                    "ФИО клиента",
                    "Марка и модель автомобиля",
                    "Дата",
                    "Описание"};
            Row row = sheet.createRow(rowCount++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = row.createCell(i);
                cell.setCellStyle(headerStyle);
                cell.setCellValue(headers[i]);
            }

            List<Act> acts = ActService.getInstance().findAllAcceptanceActs();
            fillActDocument(acts, sheet, rowCount, headers.length);
            workbook.write(outputStream);
        } catch (IOException ex) {
            throw new ServiceException(ex);
        }
    }

    private void fillActDocument(List<Act> acts, Sheet sheet, int rowCount, int tableSize) {
        for (Act act : acts) {
            Row row = sheet.createRow(rowCount++);
            for (int i = 0; i < tableSize; i++) {
                Cell cell = row.createCell(i);
                cell.setCellStyle(defaultStyle);
                StringBuilder cellValue = new StringBuilder();
                switch (i) {
                    case 0:
                        cell.setCellValue(act.getId());
                        break;
                    case 1:
                        cellValue.append(act.getManager().getPersonInfo().getFirstName())
                                .append(" ")
                                .append(act.getManager().getPersonInfo().getLastName())
                                .append(" ")
                                .append(act.getManager().getPersonInfo().getPatronymic());
                        cell.setCellValue(cellValue.toString());
                        break;
                    case 2:
                        cellValue.append(act.getClient().getPersonInformation().getFirstName())
                                .append(" ")
                                .append(act.getClient().getPersonInformation().getLastName())
                                .append(" ")
                                .append(act.getClient().getPersonInformation().getPatronymic());
                        cell.setCellValue(cellValue.toString());
                        break;
                    case 3:
                        cellValue.append(act.getCar().getModel().getVendor())
                                .append(" ")
                                .append(act.getCar().getModel().getName());
                        cell.setCellValue(cellValue.toString());
                        break;
                    case 4:
                        cell.setCellStyle(dateStyle);
                        cell.setCellValue(act.getDate());
                        break;
                    case 5:
                        cell.setCellValue(act.getDescription());
                        break;
                }
                sheet.autoSizeColumn(i);
            }
        }
    }

    private void createWorkPerformedSheetDocument() throws ServiceException {
        try (
                FileOutputStream outputStream = new FileOutputStream(WORK_PERFORMED_SHEET_DOCUMENT);
                XSSFWorkbook workbook = new XSSFWorkbook()
        ) {
            int rowCount = 0;
            createCellStyle(workbook);
            Sheet sheet = workbook.createSheet("Sheet of the work performed");
            String[] headers = {"Номер отчета",
                    "ФИО механика",
                    "Марка и модель автомобиля",
                    "Дата начала работ",
                    "Дата окончания работ",
                    "Описание"};
            Row row = sheet.createRow(rowCount++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = row.createCell(i);
                cell.setCellStyle(headerStyle);
                cell.setCellValue(headers[i]);
            }

            List<RepairReport> repairReports = RepairReportService.getInstance().findAll();
            for (RepairReport report : repairReports) {
                row = sheet.createRow(rowCount++);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellStyle(defaultStyle);
                    StringBuilder cellValue = new StringBuilder();
                    switch (i) {
                        case 0:
                            cell.setCellValue(report.getId());
                            break;
                        case 1:
                            cellValue.append(report.getMechanic().getPersonInfo().getFirstName())
                                    .append(" ")
                                    .append(report.getMechanic().getPersonInfo().getLastName())
                                    .append(" ")
                                    .append(report.getMechanic().getPersonInfo().getPatronymic());
                            cell.setCellValue(cellValue.toString());
                            break;
                        case 2:
                            cellValue.append(report.getCar().getModel().getVendor())
                                    .append(" ")
                                    .append(report.getCar().getModel().getName());
                            cell.setCellValue(cellValue.toString());
                            break;
                        case 3:
                            cell.setCellStyle(dateStyle);
                            cell.setCellValue(report.getStartDate());
                            break;
                        case 4:
                            cell.setCellStyle(dateStyle);
                            cell.setCellValue(report.getEndDate());
                            break;
                        case 5:
                            cell.setCellValue(report.getDescription());
                            break;
                    }
                    sheet.autoSizeColumn(i);
                }
            }
            workbook.write(outputStream);
        } catch (IOException ex) {
            throw new ServiceException(ex);
        }
    }

    private void createOrderDetailsApplicationDocument() throws ServiceException {
        try (
                FileOutputStream outputStream = new FileOutputStream(ORDER_DETAILS_APPLICATION);
                XSSFWorkbook workbook = new XSSFWorkbook()
        ) {
            int rowCount = 0;
            createCellStyle(workbook);
            Sheet sheet = workbook.createSheet("The application for the order details");
            String[] headers = {"Номер заявки",
                    "ФИО механика",
                    "Название детали",
                    "Количество"};
            Row row = sheet.createRow(rowCount++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = row.createCell(i);
                cell.setCellStyle(headerStyle);
                cell.setCellValue(headers[i]);
            }

            List<DetailApplication> detailApplications = DetailApplicationService.getInstance().findAll();
            for (DetailApplication application : detailApplications) {
                row = sheet.createRow(rowCount++);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellStyle(defaultStyle);
                    StringBuilder cellValue = new StringBuilder();
                    switch (i) {
                        case 0:
                            cell.setCellValue(application.getId());
                            break;
                        case 1:
                            cellValue.append(application.getMechanic().getPersonInfo().getFirstName())
                                    .append(" ")
                                    .append(application.getMechanic().getPersonInfo().getLastName())
                                    .append(" ")
                                    .append(application.getMechanic().getPersonInfo().getPatronymic());
                            cell.setCellValue(cellValue.toString());
                            break;
                        case 2:
                            cell.setCellValue(application.getDetail().getName());
                            break;
                        case 3:
                            cell.setCellValue(application.getCount());
                            break;
                    }
                    sheet.autoSizeColumn(i);
                }
            }
            workbook.write(outputStream);
        } catch (IOException ex) {
            throw new ServiceException(ex);
        }
    }

    private void createPartsInvoiceDocument() throws ServiceException {
        try (
                FileOutputStream outputStream = new FileOutputStream(PARTS_INVOICE_DOCUMENT);
                XSSFWorkbook workbook = new XSSFWorkbook()
        ) {
            int rowCount = 0;
            createCellStyle(workbook);
            Sheet sheet = workbook.createSheet("Invoice for parts");
            String[] headers = {"Номер заявки",
                    "ФИО менеджера",
                    "Номер заявки",
                    "Название детали",
                    "Количество"};
            Row row = sheet.createRow(rowCount++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = row.createCell(i);
                cell.setCellStyle(headerStyle);
                cell.setCellValue(headers[i]);
            }

            List<Invoice> invoices = InvoiceService.getInstance().findAll();
            for (Invoice invoice : invoices) {
                row = sheet.createRow(rowCount++);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellStyle(defaultStyle);
                    StringBuilder cellValue = new StringBuilder();
                    switch (i) {
                        case 0:
                            cell.setCellValue(invoice.getId());
                            break;
                        case 1:
                            cellValue.append(invoice.getManager().getPersonInfo().getFirstName())
                                    .append(" ")
                                    .append(invoice.getManager().getPersonInfo().getLastName())
                                    .append(" ")
                                    .append(invoice.getManager().getPersonInfo().getPatronymic());
                            cell.setCellValue(cellValue.toString());
                            break;
                        case 2:
                            cell.setCellValue(invoice.getApplication().getId());
                            break;
                        case 3:
                            cell.setCellValue(invoice.getApplication().getDetail().getName());
                            break;
                        case 4:
                            cell.setCellValue(invoice.getApplication().getCount());
                            break;
                    }
                    sheet.autoSizeColumn(i);
                }
            }
            workbook.write(outputStream);
        } catch (IOException ex) {
            throw new ServiceException(ex);
        }
    }

    public String getPassingActDocument(DocumentFormat format) throws ServiceException {
        createPassingActDocument();
        return saveGeneratedDocument(DocumentType.PASSING_ACT, format);
    }

    public String getAcceptanceActDocument(DocumentFormat format) throws ServiceException {
        createAcceptanceActDocument();
        return saveGeneratedDocument(DocumentType.ACCEPTANCE_ACT, format);
    }

    public String getWorkPerformedSheetDocument(DocumentFormat format) throws ServiceException {
        createWorkPerformedSheetDocument();
        return saveGeneratedDocument(DocumentType.WORK_PERFORMED_SHEET, format);
    }

    public String getOrderDetailsApplication(DocumentFormat format) throws ServiceException {
        createOrderDetailsApplicationDocument();
        return saveGeneratedDocument(DocumentType.ORDER_DETAILS_APPLICATION, format);
    }

    public String getPartsInvoiceDocument(DocumentFormat format) throws ServiceException {
        createPartsInvoiceDocument();
        return saveGeneratedDocument(DocumentType.PARTS_INVOICE, format);
    }
}
