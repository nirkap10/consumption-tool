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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.nirkap.consumption.service.ReservationService;

// Runs the whole app against the Postgres from docker-compose, so
// `docker compose up -d` must be running. Each test creates its own
// organization and user, so tests do not depend on each other or on old data.
@SpringBootTest
@AutoConfigureMockMvc
class UsageFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ReservationService reservationService;

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

    @Test
    void grantRaisesBalanceByTheAmount() throws Exception {
        String userId = createUser(1000);

        mvc.perform(postJson("/api/v1/grants",
                "{\"eventId\":\"" + newEventId() + "\",\"userId\":\"" + userId
                        + "\",\"amount\":500,\"reason\":\"extra budget\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingCredit").value(1500));
    }

    @Test
    void sameGrantEventIdTwiceIsAddedOnce() throws Exception {
        String userId = createUser(1000);
        String body = "{\"eventId\":\"" + newEventId() + "\",\"userId\":\"" + userId + "\",\"amount\":500}";

        mvc.perform(postJson("/api/v1/grants", body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingCredit").value(1500));

        // The retry: same eventId, so 200 and the balance does not move again.
        mvc.perform(postJson("/api/v1/grants", body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingCredit").value(1500));
    }

    @Test
    void grantWithoutEventIdIs400() throws Exception {
        String userId = createUser(1000);

        mvc.perform(postJson("/api/v1/grants", "{\"userId\":\"" + userId + "\",\"amount\":500}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sameOrganizationIdTwiceIs409() throws Exception {
        String body = "{\"id\":\"org-" + UUID.randomUUID() + "\",\"name\":\"Test org\"}";

        mvc.perform(postJson("/api/v1/organizations", body))
                .andExpect(status().isCreated());

        mvc.perform(postJson("/api/v1/organizations", body))
                .andExpect(status().isConflict());
    }

    @Test
    void organizationWithoutIdIs400() throws Exception {
        mvc.perform(postJson("/api/v1/organizations", "{\"name\":\"Test org\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reserveThenConfirmChargesTheRealUseOnce() throws Exception {
        String userId = createUser(1000);
        String eventId = newEventId();

        mvc.perform(postJson("/api/v1/reservations", reservationBody(eventId, userId, 100)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingCredit").value(900));

        // Used 20 more than reserved, so 20 more comes off.
        mvc.perform(postJson("/api/v1/usage", usageBody(eventId, userId, 120)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingCredit").value(880));

        // The retry: the reservation is already confirmed, so nothing changes.
        mvc.perform(postJson("/api/v1/usage", usageBody(eventId, userId, 120)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingCredit").value(880));
    }

    @Test
    void confirmingLessThanReservedGivesTheRestBack() throws Exception {
        String userId = createUser(1000);
        String eventId = newEventId();

        mvc.perform(postJson("/api/v1/reservations", reservationBody(eventId, userId, 100)));

        mvc.perform(postJson("/api/v1/usage", usageBody(eventId, userId, 60)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingCredit").value(940));
    }

    @Test
    void reservingMoreThanTheBalanceIs409() throws Exception {
        String userId = createUser(1000);

        mvc.perform(postJson("/api/v1/reservations", reservationBody(newEventId(), userId, 2000)))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/v1/users/" + userId + "/balance"))
                .andExpect(jsonPath("$.remainingCredit").value(1000));
    }

    @Test
    void sameReservationEventIdTwiceIsReservedOnce() throws Exception {
        String userId = createUser(1000);
        String body = reservationBody(newEventId(), userId, 100);

        mvc.perform(postJson("/api/v1/reservations", body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingCredit").value(900));

        mvc.perform(postJson("/api/v1/reservations", body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingCredit").value(900));
    }

    @Test
    void expiredReservationGivesTokensBackAndLateUsageIsNormalUsage() throws Exception {
        String userId = createUser(1000);
        String eventId = newEventId();

        mvc.perform(postJson("/api/v1/reservations", reservationBody(eventId, userId, 100)));

        // Pretend the reservation time has passed, then run the expiry job.
        jdbc.update("UPDATE usage_events SET reserved_until = now() - interval '1 minute' WHERE event_id = ?",
                eventId);
        reservationService.expireReservations();

        mvc.perform(get("/api/v1/users/" + userId + "/balance"))
                .andExpect(jsonPath("$.remainingCredit").value(1000));

        // The usage call comes too late, so it is charged in full.
        mvc.perform(postJson("/api/v1/usage", usageBody(eventId, userId, 120)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remainingCredit").value(880));
    }

    @Test
    void waitingReservationIsNotInTheMonthlyReport() throws Exception {
        String userId = createUser(1000);
        String eventId = newEventId();

        mvc.perform(postJson("/api/v1/reservations", reservationBody(eventId, userId, 100)));

        mvc.perform(get("/api/v1/reports/monthly").param("userId", userId))
                .andExpect(jsonPath("$.totalTokens").value(0));

        mvc.perform(postJson("/api/v1/usage", usageBody(eventId, userId, 120)));

        mvc.perform(get("/api/v1/reports/monthly").param("userId", userId))
                .andExpect(jsonPath("$.totalTokens").value(120));
    }

    private static String reservationBody(String eventId, String userId, long tokens) {
        return "{\"eventId\":\"" + eventId + "\",\"userId\":\"" + userId
                + "\",\"serviceName\":\"chat\",\"tokens\":" + tokens + "}";
    }

    private static String usageBody(String eventId, String userId, long tokensUsed) {
        return "{\"eventId\":\"" + eventId + "\",\"userId\":\"" + userId
                + "\",\"serviceName\":\"chat\",\"tokensUsed\":" + tokensUsed + "}";
    }

    // Creates an organization and a user in it, and returns the user's id.
    private String createUser(long monthlyAllowance) throws Exception {
        // The client's own id for the organization; any text works.
        String orgId = "org-" + UUID.randomUUID();
        mvc.perform(postJson("/api/v1/organizations", "{\"id\":\"" + orgId + "\",\"name\":\"Test org\"}"))
                .andExpect(status().isCreated());

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
