package com.viaticos.backend_viaticos.service;

import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.oracle.bmc.aidocument.AIServiceDocumentClient;
import com.oracle.bmc.aidocument.model.AnalyzeDocumentDetails;
import com.oracle.bmc.aidocument.model.DocumentTextExtractionFeature;
import com.oracle.bmc.aidocument.model.InlineDocumentDetails;
import com.oracle.bmc.aidocument.model.Line;
import com.oracle.bmc.aidocument.model.Page;
import com.oracle.bmc.aidocument.requests.AnalyzeDocumentRequest;
import com.oracle.bmc.aidocument.responses.AnalyzeDocumentResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OciDocumentUnderstandingService {

    private final AIServiceDocumentClient aiServiceDocumentClient;

    @Value("${app.compartmentId}")
    private String compartmentId;

    public AnalyzeDocumentResponse analyzeDocument(InputStream inputStream) throws Exception {

        byte[] fileBytes = inputStream.readAllBytes();

        // En SDK 3.80.1 InlineDocumentDetails usa data(byte[])
        InlineDocumentDetails documentDetails = InlineDocumentDetails.builder()
                .data(fileBytes)
                .build();

        // Feature OCR (Text Extraction)
        DocumentTextExtractionFeature textFeature = DocumentTextExtractionFeature.builder()
                .build();

        AnalyzeDocumentDetails details = AnalyzeDocumentDetails.builder()
                .compartmentId(compartmentId)
                .document(documentDetails)
                .features(List.of(textFeature))
                .build();

        AnalyzeDocumentRequest request = AnalyzeDocumentRequest.builder()
                .analyzeDocumentDetails(details)
                .build();

        return aiServiceDocumentClient.analyzeDocument(request);
    }

    public String extractText(AnalyzeDocumentResponse response) {

        StringBuilder sb = new StringBuilder();

        var result = response.getAnalyzeDocumentResult();

        if (result == null || result.getPages() == null) {
            return "";
        }

        for (Page page : result.getPages()) {

            if (page.getLines() == null)
                continue;

            for (Line line : page.getLines()) {
                if (line.getText() != null) {
                    sb.append(line.getText()).append("\n");
                }
            }
        }

        return sb.toString();
    }

    public String analyzeAndGetText(InputStream inputStream) throws Exception {

        AnalyzeDocumentResponse response = analyzeDocument(inputStream);

        return extractText(response);
    }
}
