package com.viaticos.backend_viaticos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.util.CellReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelTemplateParserService {

    private final ObjectMapper objectMapper;

    /**
     * Extrae una radiografía de la plantilla Excel para enviarla al LLM
     */
    public String extractTemplateStructure(InputStream excelInputStream) throws Exception {

        log.info("Iniciando escaneo inteligente de la plantilla Excel...");

        Workbook workbook = WorkbookFactory.create(excelInputStream);
        Sheet sheet = workbook.getSheetAt(0);

        Set<String> cellsToSkip = new HashSet<>();
        Set<String> masterMergedCells = new HashSet<>();
        Map<String, CellRangeAddress> mergedMap = new HashMap<>();

        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {

            CellRangeAddress merged = sheet.getMergedRegion(i);

            String masterCell = getCellAddress(
                    merged.getFirstRow(),
                    merged.getFirstColumn());

            masterMergedCells.add(masterCell);
            mergedMap.put(masterCell, merged);

            for (int r = merged.getFirstRow(); r <= merged.getLastRow(); r++) {
                for (int c = merged.getFirstColumn(); c <= merged.getLastColumn(); c++) {

                    if (r == merged.getFirstRow() && c == merged.getFirstColumn())
                        continue;

                    cellsToSkip.add(getCellAddress(r, c));
                }
            }
        }

        ArrayNode rootArray = objectMapper.createArrayNode();

        int maxRows = Math.min(sheet.getLastRowNum(), 150);

        for (int r = 0; r <= maxRows; r++) {

            Row row = sheet.getRow(r);
            if (row == null)
                continue;

            ArrayNode rowArray = objectMapper.createArrayNode();
            boolean hasMeaningfulData = false;

            for (Cell cell : row) {

                String address = cell.getAddress().formatAsString();

                if (cellsToSkip.contains(address))
                    continue;

                String value = getCellValueAsString(cell);

                boolean isMerged = masterMergedCells.contains(address);

                boolean hasBorderOrFill = hasVisualFormatting(cell) || isMerged;

                if (!value.isEmpty() || hasBorderOrFill || isMerged) {

                    ObjectNode cellNode = objectMapper.createObjectNode();

                    cellNode.put("cell", address);
                    cellNode.put("row", cell.getRowIndex() + 1);
                    cellNode.put("column", cell.getColumnIndex() + 1);
                    cellNode.put("value", value.trim());

                    if (isMerged) {

                        cellNode.put("isMerged", true);

                        CellRangeAddress merged = mergedMap.get(address);

                        if (merged != null) {

                            cellNode.put(
                                    "mergeWidth",
                                    merged.getLastColumn() - merged.getFirstColumn() + 1);

                            cellNode.put(
                                    "mergeHeight",
                                    merged.getLastRow() - merged.getFirstRow() + 1);
                        }
                    }

                    cellNode.put(
                            "type",
                            value.isEmpty() ? "input_area" : "label_or_data");

                    boolean looksLikeTableHeader = value.matches(
                            "(?i).*(fecha|factura|concepto|descripcion|monto|valor|total).*");

                    if (looksLikeTableHeader) {
                        cellNode.put("possibleTableHeader", true);
                    }

                    rowArray.add(cellNode);
                    hasMeaningfulData = true;
                }
            }

            if (hasMeaningfulData && !rowArray.isEmpty()) {
                rootArray.add(rowArray);
            }
        }

        workbook.close();

        log.info("Escaneo finalizado. Filas útiles encontradas: {}", rootArray.size());

        return objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(rootArray);
    }


    // -------- MÉTODOS AUXILIARES --------

    private String getCellAddress(int row, int col) {
        return CellReference.convertNumToColString(col) + (row + 1);
    }

    private String getCellValueAsString(Cell cell) {

        if (cell == null)
            return "";

        return switch (cell.getCellType()) {

            case STRING -> cell.getStringCellValue();

            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toString()
                    : String.valueOf(cell.getNumericCellValue());

            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());

            case FORMULA -> "FORMULA_DETECTED";

            default -> "";
        };
    }

    private boolean hasVisualFormatting(Cell cell) {

        CellStyle style = cell.getCellStyle();

        if (style == null)
            return false;

        return style.getBorderBottom() != BorderStyle.NONE
                || style.getFillPattern() != FillPatternType.NO_FILL;
    }
}
