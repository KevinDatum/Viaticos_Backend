package com.viaticos.backend_viaticos.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.generativeaiinference.GenerativeAiInferenceClient;
import com.oracle.bmc.generativeaiinference.model.*;
import com.oracle.bmc.generativeaiinference.requests.ChatRequest;
import com.oracle.bmc.generativeaiinference.responses.ChatResponse; // Asegúrate de que la ruta sea correcta
import com.viaticos.backend_viaticos.dto.response.FacturaExtractResponse;

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

    @Value("${app.compartmentId}")
    private String compartmentId;

    public FacturaExtractResponse parseOcrJson(String rawOcrJson) throws Exception {
        log.info("Iniciando refinamiento de datos con OCI Generative AI...");

        // 1. Definición del Prompt (Adaptado de tu versión de OpenAI pero para OCI)
        String systemPrompt = "Eres un asistente experto en auditoría financiera y facturación. Tu única función es extraer datos de un JSON de OCR y devolver un nuevo JSON válido.\n" + //
                                "                \n" + //
                                "                REGLAS SUPREMAS INQUEBRANTABLES:\n" + //
                                "                1. NO inventes, no asumas y no adivines ningún dato. Extrae ÚNICAMENTE la información explícita.\n" + //
                                "                2. Si un dato no está claro, falta, o la imagen es borrosa, devuelve 'SIN COMERCIO' para textos y 0 o null para números.\n" + //
                                "                3. NUNCA inventes productos (ej. hamburguesas, sodas) ni comercios si no están en el texto original.\n" + //
                                "                4. El texto original puede provenir de una imagen rotada (90, 180, 270 grados). Interpreta el texto sin importar su orden u orientación original.\n" + //
                                "                5. Tu respuesta debe ser EXCLUSIVAMENTE un bloque JSON válido, sin saludos, ni explicaciones adicionales.\n" + //
                                "                \"\"\";";
        
        String userPrompt = """
                Basado EXCLUSIVAMENTE en el siguiente contenido OCR, extrae la información de la factura respetando las REGLAS SUPREMAS.

                DEVUELVE ÚNICAMENTE UN JSON VÁLIDO con este formato EXACTO:
                {
                  "gasto": {
                    "fecha": "DD/MM/YY",
                    "categoria": "Alimentacion|Transporte|Hospedaje|Otros",
                    "moneda": "USD|EUR|GTQ|CRC|HNL|NIO|MXN|SVC|null (OJO: Si ves un símbolo $, usa USD u otra moneda de la lista, NO devuelvas el símbolo)",
                    "monto": number|null,
                    "nombreComercio": "string|null (OJO: Prioriza el nombre comercial o marca del local, por ejemplo 'Boston', 'McDonalds'. Ignora la razón social que termine en 'S.A. DE C.V.', 'Ltda', etc.)",
                    "descripcion": "frase corta resumida"
                  },
                  "items": [
                    {
                      "descripcion": "string",
                      "cantidad": number,
                      "precioUnitario": number|null,
                      "precioTotal": number|null
                    }
                  ]
                }
                
                CONTENIDO OCR:
                """ + rawOcrJson;

        // 2. Configurar la inferencia del modelo (Cohere Command R)
        // Usamos ChatDetails que es el estándar actual del SDK 3.x para modelos de chat
        ChatDetails chatDetails = ChatDetails.builder()
                .compartmentId(compartmentId)
                .servingMode(OnDemandServingMode.builder()
                        .modelId("ocid1.generativeaimodel.oc1.us-chicago-1.amaaaaaask7dceyanrlpnq5ybfu5hnzarg7jomak3q6kyhkzjsl4qj24fyoq") // El modelo que probamos con éxito
                        .build())
                .chatRequest(CohereChatRequest.builder()
                        .message(userPrompt)
                        .preambleOverride(systemPrompt)
                        .isStream(false)
                        .temperature(0.0) // Vital para evitar alucinaciones y ser determinista
                        .maxTokens(2000)
                        .build())
                .build();

        ChatRequest request = ChatRequest.builder()
                .chatDetails(chatDetails)
                .build();

        // 3. Ejecutar la llamada
        ChatResponse response = genAiClient.chat(request);

        // 4. Extraer el texto de la respuesta
        // El SDK de OCI para Cohere devuelve el contenido en text dentro de chatResponse
        CohereChatResponse chatResponse = (CohereChatResponse) response.getChatResult().getChatResponse();
        String jsonOutput = chatResponse.getText();

        log.debug("JSON recibido de GenAI: {}", jsonOutput);

        // Limpiar posibles etiquetas de markdown si el modelo las incluye (```json ... ```)
        jsonOutput = jsonOutput.replaceAll("```json|```", "").trim();

        // 5. Mapear a tu POJO final
        return objectMapper.readValue(jsonOutput, FacturaExtractResponse.class);
    }
}
