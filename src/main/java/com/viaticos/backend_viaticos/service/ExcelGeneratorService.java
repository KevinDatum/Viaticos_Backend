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
            Map<String, Object> data
    ) throws Exception {

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
                
                // La llave del JSON que envía el frontend (ej: "titularDelViaje" o "destinoDelViaje")
                String jsonField = entry.getKey(); 
                Object mappingValue = entry.getValue(); // Puede ser String ("H5") o Map (checkbox_group)

                // El valor real que viene en el JSON de tu Frontend
                Object payloadValue = data.get(jsonField); 

                if (payloadValue == null || payloadValue.toString().trim().isEmpty()) {
                    continue; // Si el frontend no mandó dato, lo saltamos
                }

                // CASO A: Es una celda simple (String)
                if (mappingValue instanceof String) {
                    String cellRef = (String) mappingValue;
                    log.info("HEADER SIMPLE {} -> {} = {}", cellRef, jsonField, payloadValue);
                    writeToCell(sheet, cellRef, payloadValue);
                } 
                // CASO B: Es un grupo de opciones (Checkbox Group)
                else if (mappingValue instanceof Map) {
                    Map<String, Object> complexMapping = (Map<String, Object>) mappingValue;
                    
                    if ("checkbox_group".equals(complexMapping.get("type"))) {
                        Map<String, String> options = (Map<String, String>) complexMapping.get("options");
                        String selectedValue = payloadValue.toString().toUpperCase().trim();
                        
                        log.info("HEADER CHECKBOX -> {} = {}", jsonField, selectedValue);

                        // Buscamos cuál opción coincide con el valor del frontend
                        for (Map.Entry<String, String> option : options.entrySet()) {
                            String optionText = option.getKey().toUpperCase().trim();
                            
                            // Usamos contains() por si hay espacios extra (ej: "OTRO:" vs "OTRO")
                            if (selectedValue.contains(optionText) || optionText.contains(selectedValue)) {
                                String cellRef = option.getValue();
                                log.info("  [X] Marcando casilla {} para opción '{}'", cellRef, optionText);
                                writeToCell(sheet, cellRef, "X"); // Escribimos la 'X' en el checkbox
                                break; 
                            }
                        }
                    }
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

            if (rows == null) {
                log.warn("No se encontró el array '{}' en el JSON", arrayPath);
            } else {
                for (int i = 0; i < rows.size(); i++) {
                    Map<String, Object> rowData = rows.get(i);
                    Row excelRow = sheet.getRow(startRow + i);
                    if (excelRow == null) {
                        excelRow = sheet.createRow(startRow + i);
                    }

                    for (Map.Entry<String, String> column : columns.entrySet()) {
                        String columnLetter = column.getKey();
                        String fieldName = column.getValue(); // ej: "montoUsd"

                        Object value = rowData.get(fieldName);
                        if (value == null) continue;

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
         * 3️⃣ EXPORTAR EXCEL
         * ============================================================
         */
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
}