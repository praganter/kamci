package dev.batuhanyetgin.cacheservice.dto;

import java.util.Map;

public record ExchangeRateResponse(String date, String base, Map<String, Double> rates) {}
