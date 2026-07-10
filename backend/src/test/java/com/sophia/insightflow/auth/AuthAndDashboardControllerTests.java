package com.sophia.insightflow.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAndDashboardControllerTests {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void registerLoginAndUseWorkspaceFeatures() throws Exception {
    String email = "user-" + UUID.randomUUID() + "@example.com";
    String password = "portfolio-pass";

    String registerPayload =
        """
        {
          "email": "%s",
          "password": "%s",
          "displayName": "Portfolio User"
        }
        """
            .formatted(email, password);

    String registerResponse =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", Matchers.not(Matchers.blankString())))
            .andExpect(jsonPath("$.user.email").value(email))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String registerToken = objectMapper.readTree(registerResponse).path("token").asText();

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + registerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Portfolio User"));

    String loginResponse =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """
                            .formatted(email, password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", Matchers.not(Matchers.blankString())))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String token = objectMapper.readTree(loginResponse).path("token").asText();

    mockMvc
        .perform(
            post("/api/dashboards")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "SP executive view",
                      "description": "Saved from tests",
                      "filters": {
                        "state": "SP",
                        "paymentType": "Credit Card"
                      }
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("SP executive view"))
        .andExpect(jsonPath("$.filters.state").value("SP"));

    mockMvc
        .perform(get("/api/dashboards").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", Matchers.hasSize(1)))
        .andExpect(jsonPath("$[0].filters.paymentType").value("Credit Card"));

    mockMvc
        .perform(
            put("/api/preferences")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "theme": "focus",
                      "compactView": true,
                      "visibleSections": ["kpis", "ai", "trend"]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.theme").value("focus"))
        .andExpect(jsonPath("$.compactView").value(true));
  }

  @Test
  void savedDashboardsRequireJwt() throws Exception {
    mockMvc.perform(get("/api/dashboards")).andExpect(status().isForbidden());
  }

  @Test
  void demoAccountCanLoginAndLoadSeededWorkspace() throws Exception {
    String loginResponse =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "email": "demo@example.com",
                          "password": "portfolio-pass"
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", Matchers.not(Matchers.blankString())))
            .andExpect(jsonPath("$.user.email").value("demo@example.com"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String token = objectMapper.readTree(loginResponse).path("token").asText();

    mockMvc
        .perform(get("/api/dashboards").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", Matchers.hasSize(1)))
        .andExpect(jsonPath("$[0].name").value("SP credit card performance"))
        .andExpect(jsonPath("$[0].filters.state").value("SP"));

    mockMvc
        .perform(get("/api/preferences").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.defaultDashboardId").value("22222222-2222-4222-8222-222222222222"));
  }

  @Test
  void preferencesRejectDashboardIdsOwnedByNobody() throws Exception {
    String loginResponse =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "email": "demo@example.com",
                          "password": "portfolio-pass"
                        }
                        """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String token = objectMapper.readTree(loginResponse).path("token").asText();

    mockMvc
        .perform(
            put("/api/preferences")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "theme": "light",
                      "compactView": false,
                      "defaultDashboardId": "33333333-3333-4333-8333-333333333333"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("defaultDashboardId must reference one of your saved dashboards"));
  }
}
