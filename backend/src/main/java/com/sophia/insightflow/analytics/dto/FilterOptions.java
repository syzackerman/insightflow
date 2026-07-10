package com.sophia.insightflow.analytics.dto;

import java.time.LocalDate;
import java.util.List;

public record FilterOptions(
    LocalDate minDate,
    LocalDate maxDate,
    List<String> states,
    List<String> categories,
    List<String> paymentTypes) {}
