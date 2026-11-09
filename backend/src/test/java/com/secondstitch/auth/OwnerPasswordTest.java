package com.secondstitch.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Starts the app with an owner password set, like the live site. */
@SpringBootTest(properties = "app.admin-password=correct-horse-battery")
@AutoConfigureMockMvc
class OwnerPasswordTest {

    @Autowired
    MockMvc mvc;

    private static final String SCARF = """
            {"name": "Silk scarf", "category": "ACCESSORY", "pricePaid": 60}
            """;

    /** Each test pretends to be a different visitor, so failed-attempt counts don't mix. */
    private static MockHttpServletRequestBuilder from(String address, MockHttpServletRequestBuilder request) {
        return request.header("X-Forwarded-For", address);
    }

    private static MockHttpServletRequestBuilder addScarf() {
        return post("/api/items").contentType(MediaType.APPLICATION_JSON).content(SCARF);
    }

    @Test
    void anyoneCanLookAround() throws Exception {
        mvc.perform(get("/api/items")).andExpect(status().isOk());
        mvc.perform(get("/api/stats")).andExpect(status().isOk());
    }

    @Test
    void tellsTheFrontendThatChangesNeedAPassword() throws Exception {
        mvc.perform(get("/api/auth")).andExpect(jsonPath("$.locked").value(true));
    }

    @Test
    void changesWithoutThePasswordAreRefused() throws Exception {
        mvc.perform(from("10.0.0.1", addScarf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unlock editing to make changes"));
    }

    @Test
    void aWrongPasswordIsRefused() throws Exception {
        mvc.perform(from("10.0.0.2", addScarf()).header(OwnerPasswordGuard.HEADER, "guess"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("That password isn't right"));
    }

    @Test
    void theRightPasswordAllowsChanges() throws Exception {
        mvc.perform(from("10.0.0.3", addScarf()).header(OwnerPasswordGuard.HEADER, "correct-horse-battery"))
                .andExpect(status().isCreated());
    }

    @Test
    void checkEndpointConfirmsThePassword() throws Exception {
        mvc.perform(from("10.0.0.4", post("/api/auth/check")).header(OwnerPasswordGuard.HEADER, "correct-horse-battery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
        mvc.perform(from("10.0.0.4", post("/api/auth/check")).header(OwnerPasswordGuard.HEADER, "nope"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tooManyWrongPasswordsMeansWaiting() throws Exception {
        for (int i = 0; i < FailedAttempts.MAX_FAILURES; i++) {
            mvc.perform(from("10.0.0.5", addScarf()).header(OwnerPasswordGuard.HEADER, "guess" + i))
                    .andExpect(status().isUnauthorized());
        }
        // Even the right password is refused until the 15 minutes are up
        mvc.perform(from("10.0.0.5", addScarf()).header(OwnerPasswordGuard.HEADER, "correct-horse-battery"))
                .andExpect(status().isTooManyRequests());
        // Other visitors aren't affected
        mvc.perform(from("10.0.0.6", addScarf()).header(OwnerPasswordGuard.HEADER, "correct-horse-battery"))
                .andExpect(status().isCreated());
    }
}
