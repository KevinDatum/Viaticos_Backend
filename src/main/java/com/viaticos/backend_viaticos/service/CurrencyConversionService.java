package com.viaticos.backend_viaticos.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class CurrencyConversionService {

    @Value("${exchangerate.api.key}")
    private String apiKey;

    // --- 🧠 CACHÉ EN MEMORIA ---
    private Map<String, Double> cachedRates = new HashMap<>();
    private LocalDate lastFetchDate;

    private String getApiUrl() {
        return "https://v6.exchangerate-api.com/v6/" + apiKey + "/latest/USD";
    }

    private static class ExchangeResponse {
        public Map<String, Double> conversion_rates;
    }

    // 1. MÉTODO PARA OBTENER LA TASA EXACTA (Para la columna TASA_CAMBIO)
    public double getExchangeRate(String monedaLocal) {
        if (monedaLocal == null || monedaLocal.trim().isEmpty() || monedaLocal.equalsIgnoreCase("USD")) {
            return 1.0;
        }

        // Si el caché está vacío o cambió de día, volvemos a llamar a la API
        if (cachedRates.isEmpty() || !LocalDate.now().equals(lastFetchDate)) {
            refreshCache();
        }

        return cachedRates.getOrDefault(monedaLocal.toUpperCase(), 1.0);
    }

    // 2. MÉTODO PARA CALCULAR LOS DÓLARES (Para la columna MONTO_USD)
    public double convertToUsd(double amountLocal, String monedaLocal) {
        double rate = getExchangeRate(monedaLocal);
        
        if (rate == 1.0 && !monedaLocal.equalsIgnoreCase("USD")) {
            return amountLocal; // Fallback por si la moneda es inválida
        }

        double usdAmount = amountLocal / rate;
        return Math.round(usdAmount * 100.0) / 100.0;
    }

    // --- FUNCIÓN INTERNA PARA ACTUALIZAR EL CACHÉ ---
    private void refreshCache() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ExchangeResponse response = restTemplate.getForObject(getApiUrl(), ExchangeResponse.class);

            if (response != null && response.conversion_rates != null) {
                this.cachedRates = response.conversion_rates;
                this.lastFetchDate = LocalDate.now();
                System.out.println("✅ Exchange Rates cacheados exitosamente para el día: " + lastFetchDate);
            }
        } catch (Exception e) {
            System.err.println("❌ Error al consultar ExchangeRate-API: " + e.getMessage());
        }
    }
}
