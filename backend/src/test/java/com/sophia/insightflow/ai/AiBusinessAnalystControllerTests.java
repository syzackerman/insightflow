package com.sophia.insightflow.ai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AiBusinessAnalystControllerTests {

  @Autowired private MockMvc mockMvc;

  @Test
  void reportGeneratesExecutiveBriefFromCurrentAnalytics() throws Exception {
    mockMvc
        .perform(
            post("/api/ai/report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "filters": {
                        "startDate": "2018-01-01",
                        "endDate": "2018-08-31",
                        "state": "SP"
                      }
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mode").value("local"))
        .andExpect(jsonPath("$.title").value("InsightFlow Executive Brief"))
        .andExpect(jsonPath("$.executiveSummary", Matchers.containsString("Revenue")))
        .andExpect(jsonPath("$.keyFindings", Matchers.hasSize(Matchers.greaterThanOrEqualTo(3))))
        .andExpect(jsonPath("$.recommendations", Matchers.hasSize(3)));
  }

  @Test
  void queryAnswersNaturalLanguageQuestion() throws Exception {
    mockMvc
        .perform(
            post("/api/ai/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "question": "Which product category is performing best?",
                      "filters": {
                        "startDate": "2018-01-01",
                        "endDate": "2018-08-31"
                      }
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mode").value("local"))
        .andExpect(jsonPath("$.question").value("Which product category is performing best?"))
        .andExpect(jsonPath("$.answer", Matchers.containsString("leads product performance")))
        .andExpect(jsonPath("$.supportingMetrics", Matchers.hasSize(Matchers.greaterThanOrEqualTo(5))));
  }

  @Test
  void queryRejectsBlankQuestion() throws Exception {
    mockMvc
        .perform(
            post("/api/ai/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "question": " "
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("question is required"));
  }
}
