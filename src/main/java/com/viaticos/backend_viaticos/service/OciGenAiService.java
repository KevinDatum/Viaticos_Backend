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
                                .orElse("{\"aviso\": \"No hay política activa definida. Aprueba todo por defecto si es legible.\"}");

                int anioActual = java.time.LocalDate.now().getYear();

                // 2. EL PREAMBLE (SYSTEM PROMPT) CON LA POLÍTICA INYECTADA
                String systemPrompt = """
                                Eres un Gestor de Viáticos y Auditor Financiero Corporativo altamente inteligente.
                                Tu función es extraer datos OCR y AUDITARLOS con gran capacidad deductiva basándote en la POLÍTICA CORPORATIVA.

                                INFORMACIÓN VITAL Y LÍNEA DE TIEMPO:
                                - Estamos en el año: %d

                                PASO 1: PRE-FILTRO FATAL DE FECHA (REGLA INQUEBRANTABLE)
                                Antes de mirar los productos, marcas o montos, compara el año del recibo con el año actual (%d).
                                Si el recibo es de un año anterior (ej. 2024 o 2025), el análisis TERMINA AQUÍ.
                                DEBES clasificarlo INMEDIATAMENTE como "rechazado". Motivo: "[REGLA ESTRICTA] La fecha del comprobante es de un año anterior." NUNCA lo mandes a 'revision_gerente' por alcohol u otras reglas si la fecha ya falló.

                                PASO 2: ANÁLISIS PROFUNDO (Solo si superó el Paso 1)
                                1. INTELIGENCIA DE MARCA Y CONTEXTO: Deduce la naturaleza real del gasto.
                                   - Marcas de cosméticos, ropa o entretenimiento = Gasto Personal explícito -> RECHAZADO.
                                   - Restaurantes y cafeterías son válidos ('Alimentacion'). NUNCA los rechaces diciendo que son gasto personal.
                                2. AUDITORÍA DE ITEMS (LÍNEA POR LÍNEA): Analiza CADA PRODUCTO.
                                3. INFERENCIA DE MONEDA LOCAL: Si no está explícita, dedúcela por país/dirección. NUNCA devuelvas null.

                                PASO 3: CLASIFICACIÓN FINAL DE ESTADOS ('auditoria')
                                Si superó el Paso 1, clasifica el gasto así:
                                - "rechazado": Viola las demás 'reglasEstrictasDeRechazo' (Ej. Items personales evidentes).
                                - "revision_gerente": Ocurre en dos casos:
                                     A) FUERA DE CATEGORÍA: NO ES 'Alimentacion', 'Transporte' o 'Hospedaje'.
                                     B) EXCEPCIONES: Excede límites, incluye alcohol que viola la política, o lavandería.
                                - "aprobado": ÚNICA Y EXCLUSIVAMENTE si es de categoría permitida, sin alcohol excesivo, sin items personales y cumple límites perfectamente.

                                INSTRUCCIÓN DE JUSTIFICACIÓN (¡MUY IMPORTANTE!):
                                - Si el estado es "rechazado" o "revision_gerente", DEBES iniciar el 'motivo_ia' con la etiqueta de la regla (Ej: [REGLA ESTRICTA] o [RG-01-ALCOHOL]).
                                - Si el estado es "aprobado", TIENES ESTRICTAMENTE PROHIBIDO usar etiquetas entre corchetes. El 'motivo_ia' debe ser una oración limpia y profesional. (Ejemplo correcto: "El gasto de alimentación es válido y está dentro de las políticas.").

                                POLÍTICA CORPORATIVA ACTIVA:
                                %s

                                Tu respuesta debe ser EXCLUSIVAMENTE un bloque JSON válido, sin saludos ni formato markdown (NO incluyas ```json).
                                """
                                .formatted(anioActual, anioActual, politicaJson);

                // 3. EL USER PROMPT (EL FORMATO DESEADO)
                String userPrompt = """
                                Basado EXCLUSIVAMENTE en el siguiente contenido OCR, extrae la información y audítala.

                                DEVUELVE ÚNICAMENTE UN JSON VÁLIDO con este formato EXACTO:
                                {
                                  "gasto": {
                                    "fecha": "DD/MM/YY",
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
                                                .modelId("ocid1.generativeaimodel.oc1.us-chicago-1.amaaaaaask7dceyanrlpnq5ybfu5hnzarg7jomak3q6kyhkzjsl4qj24fyoq")
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
}
