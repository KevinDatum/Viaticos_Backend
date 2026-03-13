package com.viaticos.backend_viaticos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.generativeaiinference.GenerativeAiInferenceClient;
import com.oracle.bmc.generativeaiinference.model.*;
import com.oracle.bmc.generativeaiinference.requests.ChatRequest;
import com.oracle.bmc.generativeaiinference.responses.ChatResponse; // Asegúrate de que la ruta sea correcta
import com.viaticos.backend_viaticos.dto.response.FacturaExtractResponse;
import com.viaticos.backend_viaticos.entity.Regla;
import com.viaticos.backend_viaticos.repository.ReglaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OciGenAiService {

    private final GenerativeAiInferenceClient genAiClient;
    private final ObjectMapper objectMapper;

    // ✨ INYECTAMOS EL REPOSITORIO DE POLÍTICAS
    private final ReglaRepository reglaRepository;

    private final CurrencyConversionService currencyConversionService;

    @Value("${app.compartmentId}")
    private String compartmentId;

    public FacturaExtractResponse parseOcrJson(String rawOcrJson) throws Exception {
        log.info("Iniciando extracción y auditoría simultánea con OCI Generative AI...");

        // 1. OBTENER LA POLÍTICA ACTIVA DE LA BASE DE DATOS
        String politicaJson = reglaRepository.findByEstadoActivo(1)
                .map(Regla::getContenidoJson)
                .orElse("{\"aviso\": \"No hay política activa definida. Extrae los datos y marca como APROBADO por defecto.\"}");

        // 2. CONTEXTO DE TIEMPO DINÁMICO
        int anioActual = java.time.LocalDate.now().getYear();

        // 3. EL PREAMBLE (SYSTEM PROMPT) CON LA REGLA FISCAL BLINDADA
        String systemPrompt = """
                Eres un Gestor de Viáticos y Auditor Financiero Corporativo altamente inteligente.
                Tu única función es extraer datos OCR y AUDITARLOS estrictamente.

                CONTEXTO DEL SISTEMA:
                - Año actual en curso: %d

                🛡️ REGLA ANTI-ALUCINACIÓN (CRÍTICA):
                1. Evalúa si el texto OCR realmente pertenece a un ticket, factura, recibo o comprobante de pago válido.
                2. Si el texto está vacío, parece basura, es una foto irrelevante o NO tiene contexto financiero:
                   - NO INVENTES DATOS BAJO NINGUNA CIRCUNSTANCIA.
                   - Deja los campos numéricos (monto) en 0 y los de texto en null.
                   - La lista de "items" DEBE estar completamente vacía ([]).
                   - El 'estado_ia' DEBE ser "RECHAZADO".
                   - El 'motivo_ia' DEBE ser exactamente: "ALERTA: La imagen cargada no se reconoce como un comprobante de gasto válido."
                   - Ignora el resto de las reglas si se cumple esta condición.

                REGLA UNIVERSAL INQUEBRANTABLE (FILTRO FISCAL):
                1. Extrae el año de la fecha del comprobante.
                2. Si el año extraído es ESTRICTAMENTE MENOR a %d, el estado DEBE ser "RECHAZADO" obligatoriamente.
                3. Si el año extraído es IGUAL a %d, IGNORA esta regla fiscal y evalúa el ticket normalmente con la Política Corporativa.
                - Motivo IA en caso de fallo fiscal: "[REGLA FISCAL] El comprobante pertenece a un año fiscal cerrado."

                INSTRUCCIONES PARA TICKETS DEL AÑO ACTUAL:
                1. CERO SUPOSICIONES: No apliques reglas de sentido común. Tu ÚNICA ley es el JSON de la "POLÍTICA CORPORATIVA ACTIVA".
                2. ANÁLISIS Y MONEDA: Deduce qué se compró leyendo los items. INFIERE LA MONEDA LOCAL por país/dirección. NUNCA devuelvas la palabra "null" en moneda, usa "USD" por defecto si no logras deducirla.
                3. APLICACIÓN DE LA POLÍTICA: Cruza la información del OCR con las 'reglas_auditoria_ia' y 'reglasEstrictasDeRechazo'.
                4. ACCIÓN: Si viola una regla, aplica el estado exacto que indique la política ("REVISION_GERENTE" o "RECHAZADO"). Si no viola NADA, el estado es "APROBADO".

                INSTRUCCIÓN DE JUSTIFICACIÓN (motivo_ia):
                - Si es RECHAZADO o REVISION_GERENTE: Inicia citando la etiqueta (Ej: "[REGLA ESTRICTA] "(la causa)"."). NO combines motivos si no es necesario.
                - Si es APROBADO: Escribe: "El gasto cumple con las políticas vigentes."

                POLÍTICA CORPORATIVA ACTIVA:
                %s
                """
                .formatted(anioActual, anioActual, anioActual, politicaJson);

        // 3. EL USER PROMPT (EL FORMATO DESEADO)
        String userPrompt = """
                Basado EXCLUSIVAMENTE en el siguiente contenido OCR, extrae la información y audítala.

                DEVUELVE ÚNICAMENTE UN JSON VÁLIDO con este formato EXACTO:
                {
                  "gasto": {
                    "fecha": "DD/MM/YY",
                    "numeroFactura": "string|null",
                    "categoria": "Alimentacion|Transporte|Hospedaje|Otros",
                    "moneda": "USD|EUR|GTQ|CRC|HNL|NIO|MXN|SVC|null",
                    "monto": number|null,
                    "nombreComercio": "string|null",
                    "descripcion": "frase corta resumida",
                    "metodoPago": "TARJETA|EFECTIVO|Transferencia|null",
                    "ultimos4Tarjeta": "string|null"
                  },
                  "items": [
                    {
                      "descripcion": "string",
                      "cantidad": number,
                      "precioUnitario": number|null,
                      "precioTotal": number|null
                    }
                  ],
                  "auditoria": {
                    "estado_ia": "aprobado|revision_gerente|rechazado",
                    "motivo_ia": "Justificación de máximo 2 líneas sobre la decisión basada en la política."
                  }
                }

                CONTENIDO OCR:
                """
                + rawOcrJson;

        // 4. CONFIGURAR LA INFERENCIA (Cohere Command R)
        ChatDetails chatDetails = ChatDetails.builder()
                .compartmentId(compartmentId)
                .servingMode(OnDemandServingMode.builder()
                        .modelId(
                                "ocid1.generativeaimodel.oc1.us-chicago-1.amaaaaaask7dceyanrlpnq5ybfu5hnzarg7jomak3q6kyhkzjsl4qj24fyoq")
                        .build())
                .chatRequest(CohereChatRequest.builder()
                        .message(userPrompt)
                        .preambleOverride(systemPrompt)
                        .isStream(false)
                        .temperature(0.0) // 0.0 es VITAL para que respete las reglas
                                          // estrictamente
                        .maxTokens(2000)
                        .build())
                .build();

        ChatRequest request = ChatRequest.builder()
                .chatDetails(chatDetails)
                .build();

        // 5. EJECUTAR LLAMADA Y LIMPIAR RESPUESTA
        ChatResponse response = genAiClient.chat(request);

        CohereChatResponse chatResponse = (CohereChatResponse) response.getChatResult().getChatResponse();
        String jsonOutput = chatResponse.getText();

        log.debug("JSON auditado recibido de GenAI: {}", jsonOutput);

        jsonOutput = jsonOutput.replaceAll("```json|```", "").trim();

        // 6. MAPEAR AL POJO
        FacturaExtractResponse extractResponse = objectMapper.readValue(jsonOutput,
                FacturaExtractResponse.class);

        // ✨ 7. INYECTAR CONVERSIÓN DE MONEDA EN TIEMPO REAL
        if (extractResponse.getGasto() != null) {
            // Cambiamos a BigDecimal para respetar el tipo de dato de tu DTO
            java.math.BigDecimal montoOriginal = extractResponse.getGasto().getMonto();
            String monedaLocal = extractResponse.getGasto().getMoneda();

            // Si hay un monto extraído, calculamos su equivalente en USD
            if (montoOriginal != null) {
                double tasa = currencyConversionService.getExchangeRate(monedaLocal);
                // Le pasamos el montoOriginal convertido a double con .doubleValue()
                double usdCalculado = currencyConversionService.convertToUsd(montoOriginal.doubleValue(), monedaLocal);

                // Guardamos los valores calculados en el DTO
                extractResponse.getGasto().setTasaCambio(tasa);
                extractResponse.getGasto().setMontoUsd(usdCalculado);

                log.info("Conversión inyectada con éxito: {} {} -> {} USD (Tasa: {})",
                        montoOriginal, monedaLocal, usdCalculado, tasa);
            }
        }

        return extractResponse;
    }

    // ==========================================
    // 🧠 MOTOR DE POLÍTICAS: EXTRAER REGLAS (NUEVO)
    // ==========================================
    public String extractRulesFromDocument(String documentText) throws Exception {
        log.info("Iniciando extracción de políticas con OCI Generative AI...");

        String systemPrompt = """
        Eres un Consultor Financiero y Auditor de Inteligencia Artificial nivel Senior.
        Tu tarea es leer el texto de un manual de políticas corporativas y estructurarlo EXACTAMENTE en el siguiente formato JSON.
        
        REGLAS DE EXTRACCIÓN (¡CRÍTICAS!):
        1. NO inventes información. Extrae los límites y categorías reales del documento.
        2. ¡MUY IMPORTANTE!: ABSOLUTAMENTE TODAS LAS REGLAS (las que provocan RECHAZO, las de REVISIÓN y las de APROBACIÓN) DEBEN ir detalladas como objetos individuales obligatoriamente dentro del arreglo 'reglas_auditoria_ia'. 
        3. En 'reglas_auditoria_ia', la 'accion' SIEMPRE debe ser una de estas tres: "APROBADO", "REVISION_GERENTE" o "RECHAZADO".
        4. Crea un ID lógico para cada regla en 'id_regla' (ej. RG-01-ALIMENTACION).
        
        FORMATO JSON ESTRICTO REQUERIDO (Usa esto como molde):
        {
          "metadatos": {
            "empresa": "Nombre extraído de la Empresa",
            "politica": "Nombre del documento",
            "version": "Versión o fecha extraída"
          },
          "limites_y_categorias": {
            "limitesDiariosViaticosUSD": {
               "General": 0.00
            },
            "categoriasAprobadasAutomaticamente": ["Ej: Alimentacion"],
            "categoriasQueRequierenAutorizacionPrevia": ["Ej: Vuelos"],
            "gastosDeRepresentacion": {
               "permitidos": ["Ej: Comida clientes"],
               "permitidosEnFinDeSemana": false
            }
          },
          "reglasEstrictasDeRechazo": [
             "Resumen general de rechazos"
          ],
          "reglas_auditoria_ia": [
            {
              "id_regla": "RG-01-CATEGORIA",
              "categoria": "Categoría a la que aplica",
              "descripcion": "Condición o límite específico extraído del texto",
              "accion": "RECHAZADO"
            },
            {
              "id_regla": "RG-02-OTRA",
              "categoria": "Otra categoría",
              "descripcion": "Otra regla extraída del texto",
              "accion": "REVISION_GERENTE"
            }
          ]
        }
        
        Devuelve ÚNICA Y EXCLUSIVAMENTE un bloque JSON válido, sin markdown ni comillas invertidas (```).
        """;

        String userPrompt = "Analiza el siguiente documento de políticas y devuelve el JSON:\n\n" + documentText;

        ChatDetails chatDetails = ChatDetails.builder()
                .compartmentId(compartmentId)
                .servingMode(OnDemandServingMode.builder()
                        .modelId(
                                "ocid1.generativeaimodel.oc1.us-chicago-1.amaaaaaask7dceyanrlpnq5ybfu5hnzarg7jomak3q6kyhkzjsl4qj24fyoq")
                        .build())
                .chatRequest(CohereChatRequest.builder()
                        .message(userPrompt)
                        .preambleOverride(systemPrompt)
                        .isStream(false)
                        .temperature(0.1) // Muy bajo para que no alucine e invente reglas
                        .maxTokens(3500)
                        .build())
                .build();

        ChatResponse response = genAiClient.chat(ChatRequest.builder().chatDetails(chatDetails).build());
        CohereChatResponse chatResponse = (CohereChatResponse) response.getChatResult().getChatResponse();

        String rawOutput = chatResponse.getText();
        log.info("Texto crudo recibido del LLM.");

        // 1. Limpieza básica de Markdown (por si el LLM incluye ```json )
        String cleaned = rawOutput.replaceAll("```json", "").replaceAll("```", "").trim();

        // 2. Extracción de los corchetes principales
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1);
        }

        // 3. 🛡️ EL BLINDAJE: Usar ObjectMapper para purificar el JSON
        try {
            // Lee el texto (si hay un caracter invisible al final, Jackson lo ignora)
            com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(cleaned);

            // Lo vuelve a escribir como un String 100% puro y válido
            String jsonPuro = objectMapper.writeValueAsString(rootNode);

            log.info("JSON purificado exitosamente por Jackson.");
            return jsonPuro;

        } catch (Exception e) {
            log.error("Fallo crítico al intentar parsear el JSON de la IA. Texto problemático:\n{}", cleaned);
            throw new RuntimeException("La IA no generó un formato válido. Intenta subir el documento de nuevo.");
        }
    }

    /**
     * Genera el mapeo lógico de una plantilla Excel basándose en un escaneo de
     * celdas y etiquetas,
     * y cruzándolo dinámicamente con las propiedades conocidas del sistema.
     */
    public String generateTemplateMapping(String rawExcelMap) throws Exception {

        log.info("Iniciando generación de mapeo inteligente (Dynamic Schema Injection)...");

        // 1. DICCIONARIO CON LOS NUEVOS TOTALES (Cero datos quemados del Excel)
        List<String> reportVariables = java.util.Arrays.asList(
                // Datos generales del encabezado
                "fechaLiquidacion", "titularDelViaje", "fechaSalida", "destinoDelViaje",
                "fechaRegreso", "motivoDelViaje", "gastosCubiertosPor", "gastosDelArea",
                "monedaDeLosGastos", "descripcion",

                // Datos de liquidación y totales (NUEVOS)
                "totalMontoLocal", "totalGastos", "viaticosAsignados", "gastosLiquidados", "diferencia",
                "firmaSolicitante", "firmaAutorizada",

                // Datos de la tabla
                "elementosTabla", "item", "fecha", "factura", "concepto",
                "montoLocal", "tasaCambio", "montoUsd");

        String jsonSchemaDisponibles = objectMapper.writeValueAsString(reportVariables);

        log.info("Diccionario de datos inyectado al LLM: {}", jsonSchemaDisponibles);

        // 2. EL PROMPT BLINDADO Y GENÉRICO
        String systemPrompt = """
                You are an expert mapping Excel templates to JSON data models.

                INPUT 1: Excel structure (visible cells, labels, merged areas).
                INPUT 2: Available System Variables: %s

                CRITICAL RULES FOR DYNAMIC MAPPING:
                1. STRICT JSON KEYS: The keys in your JSON MUST be EXACTLY the strings provided in INPUT 2. NEVER use the raw Excel labels as keys.

                2. INPUT CELLS (CRITICAL): When mapping a standard variable, map it to the empty or underlined cell IMMEDIATELY TO THE RIGHT or BELOW the label. NEVER map it to the cell containing the label text itself.

                3. CHECKBOX GROUPS (VITAL): If you detect multiple adjacent options for a single concept, map them as a "checkbox_group".
                   CRITICAL: Variables like "destinoDelViaje", "gastosCubiertosPor", "gastosDelArea", and "monedaDeLosGastos" almost ALWAYS have checkboxes. Ensure they are mapped as checkbox_groups, not simple strings. Record the cell where the option text is located.

                4. FOOTER & TOTALS: Scan the bottom of the document. You MUST map ALL 5 of these variables: "totalMontoLocal" (the sum under the local amount column), "totalGastos" (the sum under the USD column), "viaticosAsignados", "gastosLiquidados", and "diferencia". Put ALL 5 in the "footer" object.

                5. TABLES (CRITICAL): You MUST include a "table" object. Set "startRow" to the row IMMEDIATELY AFTER the headers.
                   CRITICAL FOR COLUMNS: The keys in the "columns" object are the Excel column letters. The VALUES MUST BE THE SYSTEM VARIABLES (e.g., "item", "montoLocal", "montoUsd"), NOT cell coordinates. The arrayPath MUST BE "elementosTabla".

                OUTPUT JSON FORMAT:
                {
                  "header": {
                     "systemVariable1": "CellCoordinate",
                     "systemVariable2": {
                        "type": "checkbox_group",
                        "options": {
                           "OptionTextA": "CellCoordinateA"
                        }
                     }
                  },
                  "table": {
                    "startRow": [IntegerRowNumber],
                    "arrayPath": "elementosTabla",
                    "columns": {
                       "[ColumnLetterA]": "systemVariableForItem",
                       "[ColumnLetterB]": "systemVariableForLocalAmount"
                    }
                  },
                  "footer": {
                     "systemVariableForTotal": "CellCoordinate"
                  }
                }
                """
                .formatted(jsonSchemaDisponibles);

        String userPrompt = """
                Analyze the following Excel structure JSON and generate the strict template mapping JSON.
                Excel structure:
                """ + rawExcelMap;

        ChatDetails chatDetails = ChatDetails.builder()
                .compartmentId(compartmentId)
                .servingMode(OnDemandServingMode.builder()
                        .modelId(
                                "ocid1.generativeaimodel.oc1.us-chicago-1.amaaaaaask7dceyanrlpnq5ybfu5hnzarg7jomak3q6kyhkzjsl4qj24fyoq")
                        .build())
                .chatRequest(CohereChatRequest.builder()
                        .message(userPrompt)
                        .preambleOverride(systemPrompt)
                        .isStream(false)
                        .temperature(0.0) // Vital para que sea determinista y obedezca el esquema
                        .maxTokens(3000)
                        .build())
                .build();

        ChatRequest request = ChatRequest.builder()
                .chatDetails(chatDetails)
                .build();

        ChatResponse response = genAiClient.chat(request);
        CohereChatResponse chatResponse = (CohereChatResponse) response.getChatResult().getChatResponse();

        String jsonOutput = chatResponse.getText();
        jsonOutput = jsonOutput.replaceAll("```json|```", "").trim();

        log.info("Mapeo generado exitosamente por la IA.");

        return jsonOutput;
    }

    // ==========================================
    // 🧾 MOTOR DE EXTRACCIÓN DTE (JSON ESTRUCTURADO)
    // ==========================================
    public FacturaExtractResponse parseDteJson(String dteJsonContent) throws Exception {
        log.info("Iniciando auditoría directa de DTE (Factura Electrónica)...");

        String cleanJson = cleanBloatedDteJson(dteJsonContent);
        log.info("DTE purificado. Tamaño reducido para el LLM.");

        // 1. OBTENER LA POLÍTICA ACTIVA DE LA BASE DE DATOS
        String politicaJson = reglaRepository.findByEstadoActivo(1)
                .map(Regla::getContenidoJson)
                .orElse("{\"aviso\": \"No hay política activa definida. Extrae los datos y marca como APROBADO por defecto.\"}");

        int anioActual = java.time.LocalDate.now().getYear();

        // 2. SYSTEM PROMPT (Idéntico al del OCR)
        String systemPrompt = """
                Eres un Gestor de Viáticos y Auditor Financiero Corporativo altamente inteligente.
                Tu única función es extraer datos y AUDITARLOS estrictamente.

                CONTEXTO DEL SISTEMA:
                - Año actual en curso: %d

                REGLA UNIVERSAL INQUEBRANTABLE (FILTRO FISCAL):
                1. Extrae el año de la fecha del comprobante.
                2. Si el año extraído es ESTRICTAMENTE MENOR a %d, el estado DEBE ser "RECHAZADO" obligatoriamente.
                3. Si el año extraído es IGUAL a %d, IGNORA esta regla fiscal y evalúa el ticket normalmente con la Política Corporativa.
                - Motivo IA en caso de fallo fiscal: "[REGLA FISCAL] El comprobante pertenece a un año fiscal cerrado."

                INSTRUCCIONES PARA TICKETS DEL AÑO ACTUAL:
                1. CERO SUPOSICIONES: No apliques reglas de sentido común. Tu ÚNICA ley es el JSON de la "POLÍTICA CORPORATIVA ACTIVA".
                2. ANÁLISIS Y MONEDA: Deduce qué se compró leyendo los items. INFIERE LA MONEDA LOCAL por país/dirección. NUNCA devuelvas la palabra "null" en moneda, usa "USD" por defecto si no logras deducirla.
                3. APLICACIÓN DE LA POLÍTICA: Cruza la información extraída con las 'reglas_auditoria_ia' y 'reglasEstrictasDeRechazo'.
                4. ACCIÓN: Si viola una regla, aplica el estado exacto que indique la política ("REVISION_GERENTE" o "RECHAZADO"). Si no viola NADA, el estado es "APROBADO".

                POLÍTICA CORPORATIVA ACTIVA:
                %s
                """
                .formatted(anioActual, anioActual, anioActual, politicaJson);

        // 3. USER PROMPT ADAPTADO PARA JSON
        String userPrompt = """
                Basado EXCLUSIVAMENTE en el siguiente JSON de Factura Electrónica (DTE), extrae la información y audítala.
                El documento ya viene estructurado, busca los campos de "emisor", "receptor", "cuerpoDocumento" (o similares) para encontrar el comercio, el número de resolución/factura y los items.

                DEVUELVE ÚNICAMENTE UN JSON VÁLIDO con este formato EXACTO:
                {
                  "gasto": {
                    "fecha": "DD/MM/YY",
                    "numeroFactura": "string|null",
                    "categoria": "Alimentacion|Transporte|Hospedaje|Otros",
                    "moneda": "USD|EUR|GTQ|CRC|HNL|NIO|MXN|SVC|null",
                    "monto": number|null,
                    "nombreComercio": "string|null",
                    "descripcion": "frase corta resumida",
                    "metodoPago": "TARJETA|EFECTIVO|Transferencia|null",
                    "ultimos4Tarjeta": "string|null"
                  },
                  "items": [
                    {
                      "descripcion": "string",
                      "cantidad": number,
                      "precioUnitario": number|null,
                      "precioTotal": number|null
                    }
                  ],
                  "auditoria": {
                    "estado_ia": "aprobado|revision_gerente|rechazado",
                    "motivo_ia": "Justificación de máximo 2 líneas sobre la decisión basada en la política."
                  }
                }

                CONTENIDO DTE JSON:
                """
                + cleanJson;

        ChatDetails chatDetails = ChatDetails.builder()
                .compartmentId(compartmentId)
                .servingMode(OnDemandServingMode.builder()
                        .modelId("ocid1.generativeaimodel.oc1.us-chicago-1.amaaaaaask7dceyanrlpnq5ybfu5hnzarg7jomak3q6kyhkzjsl4qj24fyoq")
                        .build())
                .chatRequest(CohereChatRequest.builder()
                        .message(userPrompt)
                        .preambleOverride(systemPrompt)
                        .isStream(false)
                        .temperature(0.0) 
                        .maxTokens(2000)
                        .build())
                .build();

        ChatResponse response = genAiClient.chat(ChatRequest.builder().chatDetails(chatDetails).build());
        CohereChatResponse chatResponse = (CohereChatResponse) response.getChatResult().getChatResponse();
        
        String jsonOutput = chatResponse.getText().replaceAll("```json|```", "").trim();
        log.debug("JSON auditado (DTE) recibido de GenAI: {}", jsonOutput);

        FacturaExtractResponse extractResponse = objectMapper.readValue(jsonOutput, FacturaExtractResponse.class);

        // 4. INYECTAR CONVERSIÓN DE MONEDA
        if (extractResponse.getGasto() != null && extractResponse.getGasto().getMonto() != null) {
            java.math.BigDecimal montoOriginal = extractResponse.getGasto().getMonto();
            String monedaLocal = extractResponse.getGasto().getMoneda();
            double tasa = currencyConversionService.getExchangeRate(monedaLocal);
            double usdCalculado = currencyConversionService.convertToUsd(montoOriginal.doubleValue(), monedaLocal);

            extractResponse.getGasto().setTasaCambio(tasa);
            extractResponse.getGasto().setMontoUsd(usdCalculado);
        }

        return extractResponse;
    }

    // ==========================================
    // 🧹 PURIFICADOR DE DTE (EL ESCUDO DEFINITIVO)
    // ==========================================
    private String cleanBloatedDteJson(String rawJson) {
        try {
            com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(rawJson);
            removeHeavyFields(rootNode);
            String jsonLimpio = objectMapper.writeValueAsString(rootNode);
            
            log.info("DTE Purificado. Tamaño original: {}, Tamaño final: {}", rawJson.length(), jsonLimpio.length());
            
            // Fallback extremo por si la estructura sigue siendo absurdamente grande
            if (jsonLimpio.length() > 30000) {
                log.warn("El JSON sigue siendo muy grande. Aplicando recorte de emergencia.");
                return jsonLimpio.substring(0, 30000) + "}}";
            }
            
            return jsonLimpio;
        } catch (Exception e) {
            log.warn("Fallo en purificador DTE: {}", e.getMessage());
            return rawJson.length() > 30000 ? rawJson.substring(0, 30000) : rawJson;
        }
    }

    private void removeHeavyFields(com.fasterxml.jackson.databind.JsonNode node) {
        if (node.isObject()) {
            com.fasterxml.jackson.databind.node.ObjectNode obj = (com.fasterxml.jackson.databind.node.ObjectNode) node;
            java.util.List<String> keysToRemove = new java.util.ArrayList<>();

            // ✨ SOLUCIÓN: Usamos fieldNames() en lugar del método deprecado fields()
            java.util.Iterator<String> fieldNames = obj.fieldNames();
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                com.fasterxml.jackson.databind.JsonNode child = obj.get(key);
                
                String keyLower = key.toLowerCase();

                // 1. Filtrar por nombre (Lista negra ampliada)
                if (keyLower.contains("firma") || keyLower.contains("sello") || keyLower.contains("certificado")
                        || keyLower.contains("qr") || keyLower.contains("extension") || keyLower.contains("signature")
                        || keyLower.contains("hash") || keyLower.contains("barcode") || keyLower.contains("xml")) {
                    keysToRemove.add(key);
                } 
                // 2. LA MAGIA: Poda por tamaño de caracteres
                // Si un valor de texto tiene más de 250 caracteres, es basura garantizada.
                else if (child.isTextual() && child.asText().length() > 250) {
                    keysToRemove.add(key);
                } 
                // 3. Seguimos escarbando
                else {
                    removeHeavyFields(child);
                }
            }
            
            // Ejecutamos la purga
            obj.remove(keysToRemove);
            
        } else if (node.isArray()) {
            com.fasterxml.jackson.databind.node.ArrayNode array = (com.fasterxml.jackson.databind.node.ArrayNode) node;
            for (int i = 0; i < array.size(); i++) {
                removeHeavyFields(array.get(i));
            }
        }
    }
}
