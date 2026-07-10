package com.sophia.insightflow.analytics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsControllerTests {

  @Autowired private MockMvc mockMvc;

  @Test
  void summaryReturnsGeneratedBusinessKpis() throws Exception {
    mockMvc
        .perform(get("/api/analytics/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.kpis.totalOrders").value(96478))
        .andExpect(jsonPath("$.kpis.totalRevenue").value(13221498.11))
        .andExpect(jsonPath("$.kpis.averageReviewScore").value(4.16))
        .andExpect(jsonPath("$.revenueByMonth[0].month").value("2016-09"))
        .andExpect(jsonPath("$.topCategories[0].category").value("Health Beauty"));
  }

  @Test
  void summaryAcceptsFilterQueryParams() throws Exception {
    mockMvc
        .perform(
            get("/api/analytics/summary")
                .param("startDate", "2018-01-01")
                .param("endDate", "2018-08-31")
                .param("state", "SP")
                .param("category", "Health Beauty")
                .param("paymentType", "Credit Card"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.filters.state").value("SP"))
        .andExpect(jsonPath("$.filters.category").value("Health Beauty"))
        .andExpect(jsonPath("$.filters.paymentType").value("Credit Card"))
        .andExpect(jsonPath("$.kpis.totalOrders", Matchers.greaterThan(0)))
        .andExpect(jsonPath("$.topCategories[0].category").value("Health Beauty"));
  }

  @Test
  void summaryRejectsInvalidDateRange() throws Exception {
    mockMvc
        .perform(
            get("/api/analytics/summary")
                .param("startDate", "2018-08-31")
                .param("endDate", "2018-01-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("startDate must be on or before endDate"));
  }
}
