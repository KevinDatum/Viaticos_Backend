package com.viaticos.backend_viaticos.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ExcelGeneratorService {

    @SuppressWarnings("unchecked")
    public byte[] generateExcel(
            InputStream templateStream,
            Map<String, Object> mapping,
            Map<String, Object> data) throws Exception {

        System.out.println("=== GENERATE EXCEL INICIADO (DYNAMIC SCHEMA) ===");

        Workbook workbook = new XSSFWorkbook(templateStream);
        Sheet sheet = workbook.getSheetAt(0);

        /*
         * ============================================================
         * 1️⃣ LLENAR HEADER Y CHECKBOXES DINÁMICOS
         * ============================================================
         */
        Map<String, Object> header = (Map<String, Object>) mapping.get("header");

        if (header != null) {
            for (Map.Entry<String, Object> entry : header.entrySet()) {

                String jsonField = entry.getKey();
                Object mappingValue = entry.getValue();
                Object payloadValue = data.get(jsonField);

                if (payloadValue == null || payloadValue.toString().trim().isEmpty()) {
                    continue;
                }

                // CASO A: Celda Simple
                if (mappingValue instanceof String) {
                    String cellRef = (String) mappingValue;
                    log.info("HEADER SIMPLE {} -> {} = {}", cellRef, jsonField, payloadValue);
                    writeToCell(sheet, cellRef, payloadValue);
                }
                // CASO B: Checkbox Group
                else if (mappingValue instanceof Map) {
                    Map<String, Object> complexMapping = (Map<String, Object>) mappingValue;

                    if ("checkbox_group".equals(complexMapping.get("type"))) {
                        Map<String, String> options = (Map<String, String>) complexMapping.get("options");
                        String selectedValue = payloadValue.toString().toUpperCase().trim();
                        boolean matchFound = false;
                        String cellForOtherLabel = null;

                        // 1. Buscar coincidencia exacta o parcial
                        for (Map.Entry<String, String> option : options.entrySet()) {
                            String optionText = option.getKey().toUpperCase().trim();
                            String textCellRef = option.getValue();

                            // Guardamos dónde está la etiqueta "OTRO", "OTRA", "OTROS" por si no hay match
                            if (optionText.startsWith("OTR")) {
                                cellForOtherLabel = textCellRef;
                            }

                            if (selectedValue.contains(optionText) || optionText.contains(selectedValue)) {
                                String targetCell = findEmptyCheckboxCell(sheet, textCellRef);
                                log.info("  [X] Marcando casilla {} para la opción '{}' (Texto en {})",
                                        targetCell, optionText, textCellRef);
                                writeToCell(sheet, targetCell, "X");
                                matchFound = true;
                                break;
                            }
                        }

                        // 2. MAGIA "OTROS": Si no hubo match y la plantilla tiene opción "OTRO"
                        if (!matchFound && cellForOtherLabel != null) {
                            // Encontrar el cuadrito vacío junto a la etiqueta "OTRO"
                            String targetCellForOther = findEmptyCheckboxCell(sheet, cellForOtherLabel);
                            log.info("  [X] Marcando casilla 'OTRO' en {} para el valor no estándar '{}'",
                                    targetCellForOther, selectedValue);
                            writeToCell(sheet, targetCellForOther, "X");

                            // 3. Escribir el valor real (Ej: "GTQ", "EUROPA") a la derecha de la etiqueta
                            // "OTRO:"
                            org.apache.poi.ss.util.CellReference ref = new org.apache.poi.ss.util.CellReference(
                                    cellForOtherLabel);
                            org.apache.poi.ss.usermodel.Row row = sheet.getRow(ref.getRow());
                            if (row != null) {
                                // Nos movemos 2 celdas a la derecha de la etiqueta "OTRO:" (puedes ajustar el
                                // +2 si hace falta)
                                int targetCol = ref.getCol() + 2;
                                org.apache.poi.ss.usermodel.Cell textCell = row.getCell(targetCol);
                                if (textCell == null) {
                                    textCell = row.createCell(targetCol);
                                }
                                textCell.setCellValue(payloadValue.toString());
                                log.info("  [✎] Escribiendo texto '{}' en la celda adyacente (Columna index {})",
                                        payloadValue, targetCol);
                            }
                        }
                    }
                }
            }
        }

        /*
         * ============================================================
         * 1.5️⃣ LLENAR FOOTER (Si la IA decide crearlo)
         * ============================================================
         */
        Map<String, Object> footer = (Map<String, Object>) mapping.get("footer");

        if (footer != null) {
            for (Map.Entry<String, Object> entry : footer.entrySet()) {
                String jsonField = entry.getKey();
                Object mappingValue = entry.getValue();
                Object payloadValue = data.get(jsonField);

                if (payloadValue == null || payloadValue.toString().trim().isEmpty()) {
                    continue;
                }

                if (mappingValue instanceof String) {
                    String cellRef = (String) mappingValue;
                    log.info("FOOTER SIMPLE {} -> {} = {}", cellRef, jsonField, payloadValue);
                    writeToCell(sheet, cellRef, payloadValue);
                }
            }
        }

        /*
         * ============================================================
         * 2️⃣ LLENAR TABLA DINÁMICA
         * ============================================================
         */
        Map<String, Object> table = (Map<String, Object>) mapping.get("table");

        if (table != null) {
            int startRow = ((Number) table.get("startRow")).intValue();
            String arrayPath = (String) table.get("arrayPath");

            Map<String, String> columns = (Map<String, String>) table.get("columns");
            List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get(arrayPath);

            if (rows != null) {
                for (int i = 0; i < rows.size(); i++) {
                    Map<String, Object> rowData = rows.get(i);
                    Row excelRow = sheet.getRow(startRow + i);
                    if (excelRow == null) {
                        excelRow = sheet.createRow(startRow + i);
                    }

                    for (Map.Entry<String, String> column : columns.entrySet()) {
                        String columnLetter = column.getKey();
                        String fieldName = column.getValue();

                        Object value = rowData.get(fieldName);
                        if (value == null)
                            continue;

                        int columnIndex = CellReference.convertColStringToIndex(columnLetter);
                        Cell cell = excelRow.getCell(columnIndex);
                        if (cell == null) {
                            cell = excelRow.createCell(columnIndex);
                        }
                        setCellValue(cell, value);
                    }
                }
            }
        }

        /*
         * ============================================================
         * 3️⃣ EXPORTAR EXCEL Y FORZAR RECÁLCULO
         * ============================================================
         */
        // Obliga a Excel a recalcular todo (incluyendo XLOOKUP) de forma nativa al
        // abrirse.
        workbook.setForceFormulaRecalculation(true);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    /*
     * ============================================================
     * MÉTODOS AUXILIARES
     * ============================================================
     */

    private void writeToCell(Sheet sheet, String cellRef, Object value) {
        CellReference ref = new CellReference(cellRef);
        Row row = sheet.getRow(ref.getRow());
        if (row == null) {
            row = sheet.createRow(ref.getRow());
        }

        Cell cell = row.getCell(ref.getCol());
        if (cell == null) {
            cell = row.createCell(ref.getCol());
        }

        setCellValue(cell, value);
    }

    private void setCellValue(Cell cell, Object value) {
        if (value == null || value.toString().isEmpty()) {
            cell.setBlank();
            return;
        }

        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value ? "X" : "");
        } else {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * Busca la celda del "cuadrito" vacía más cercana al texto.
     * Si no encuentra una vacía, fuerza a marcar la celda inmediatamente a la
     * izquierda
     * para no sobreescribir el texto original.
     */
    private String findEmptyCheckboxCell(Sheet sheet, String textCellRef) {
        CellReference ref = new CellReference(textCellRef);
        Row row = sheet.getRow(ref.getRow());

        if (row == null)
            return textCellRef;

        int textColIndex = ref.getCol();
        int targetColIndex = textColIndex - 1; // Por defecto asumimos la izquierda

        // 1. Mirar a la izquierda (máximo 2 celdas)
        for (int i = 1; i <= 2; i++) {
            int checkCol = textColIndex - i;
            if (checkCol >= 0) {
                Cell c = row.getCell(checkCol);
                if (c == null || c.getCellType() == CellType.BLANK
                        || (c.getCellType() == CellType.STRING && c.getStringCellValue().trim().isEmpty())) {
                    return new CellReference(ref.getRow(), checkCol).formatAsString();
                }
            }
        }

        // 2. Fallback: Forzar celda izquierda para no borrar el texto
        return new CellReference(ref.getRow(), Math.max(0, targetColIndex)).formatAsString();
    }
}