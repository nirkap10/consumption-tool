package com.nirkap.consumption;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

// Runs the whole app against the Postgres from docker-compose, so
// `docker compose up -d` must be running. Each test creates its own
// organization and user, so tests do not depend on each other or on old data.
@SpringBootTest
@AutoConfigureMockMvc
class UsageFlowTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void newUserStartsWithFullAllowance() throws Exception {
        String userId = createUser(1000);

        mvc.perform(get("/api/v1/users/" + userId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingCredit").value(1000));
    }

    @Test
    void usageLowersBalanceByExactlyTheTokensUsed() throws Exception {
        String userId = createUser(1000);

        mvc.perform(postJson("/api/v1/usage",
                "{\"eventId\":\"" + newEventId() + "\",\"userId\":\"" + userId + "\",\"serviceName\":\"chat\",\"tokensUsed\":100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingCredit").value(900));

        mvc.perform(get("/api/v1/users/" + userId + "/balance"))
                .andExpect(jsonPath("$.remainingCredit").value(900));
    }

    @Test
    void overSpendingIsRecordedAndBalanceGoesNegative() throws Exception {
        String userId = createUser(100);

        mvc.perform(postJson("/api/v1/usage",
                "{\"eventId\":\"" + newEventId() + "\",\"userId\":\"" + userId + "\",\"serviceName\":\"chat\",\"tokensUsed\":250}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingCredit").value(-150));
    }

    @Test
    void usageForUnknownUserIs404() throws Exception {
        mvc.perform(postJson("/api/v1/usage",
                "{\"eventId\":\"" + newEventId() + "\",\"userId\":\"00000000-0000-0000-0000-000000000000\",\"serviceName\":\"chat\",\"tokensUsed\":10}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void usageWithZeroTokensIs400() throws Exception {
        String userId = createUser(1000);

        mvc.perform(postJson("/api/v1/usage",
                "{\"eventId\":\"" + newEventId() + "\",\"userId\":\"" + userId + "\",\"serviceName\":\"chat\",\"tokensUsed\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sameEventIdTwiceIsCountedOnce() throws Exception {
        String userId = createUser(1000);
        String body = "{\"eventId\":\"" + newEventId() + "\",\"userId\":\"" + userId
                + "\",\"serviceName\":\"chat\",\"tokensUsed\":100}";

        mvc.perform(postJson("/api/v1/usage", body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingCredit").value(900));

        // The retry: same eventId, so 200 and the balance does not move again.
        mvc.perform(postJson("/api/v1/usage", body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingCredit").value(900));
    }

    @Test
    void usageWithoutEventIdIs400() throws Exception {
        String userId = createUser(1000);

        mvc.perform(postJson("/api/v1/usage",
                "{\"userId\":\"" + userId + "\",\"serviceName\":\"chat\",\"tokensUsed\":10}"))
                .andExpect(status().isBadRequest());
    }

    // Creates an organization and a user in it, and returns the user's id.
    private String createUser(long monthlyAllowance) throws Exception {
        String orgJson = mvc.perform(postJson("/api/v1/organizations", "{\"name\":\"Test org\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String orgId = JsonPath.read(orgJson, "$.id");

        String userJson = mvc.perform(postJson("/api/v1/users",
                "{\"organizationId\":\"" + orgId + "\",\"monthlyAllowance\":" + monthlyAllowance + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(userJson, "$.id");
    }

    // A fresh id per usage report, the way the company's system would make one.
    private static String newEventId() {
        return UUID.randomUUID().toString();
    }

    private static MockHttpServletRequestBuilder postJson(String url, String body) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
