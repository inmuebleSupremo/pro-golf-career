package com.progolf.app.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.progolf.app.auth.dto.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * authentication: the end-to-end HTTP flow — register, login, refresh — and the security gate on the GraphQL
 * endpoint (a valid access token is required; refresh tokens and missing/invalid tokens are rejected).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    private static final String GRAPHQL_QUERY = "{\"query\":\"{ __typename }\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private void register(String username, String password) throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(username, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    private TokenResponse login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
    }

    private String body(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    /** A unique username so re-running the suite over the persisted test-users dir never collides. */
    private String newUsername() {
        return "user-" + java.util.UUID.randomUUID();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder graphql() {
        return post("/graphql").contentType(MediaType.APPLICATION_JSON).content(GRAPHQL_QUERY);
    }

    @Test
    void registerLoginThenAccessTheApiWithTheToken() throws Exception {
        String user = newUsername();
        register(user, "s3cret-pw");
        TokenResponse tokens = login(user, "s3cret-pw");
        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();

        mockMvc.perform(graphql().header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.__typename").value("Query"));
    }

    @Test
    void theApiRejectsMissingAndInvalidTokens() throws Exception {
        mockMvc.perform(graphql()).andExpect(status().isUnauthorized());
        mockMvc.perform(graphql().header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aRefreshTokenCannotAccessTheApi() throws Exception {
        String user = newUsername();
        register(user, "s3cret-pw");
        TokenResponse tokens = login(user, "s3cret-pw");

        mockMvc.perform(graphql().header("Authorization", "Bearer " + tokens.refreshToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshIssuesANewAccessToken() throws Exception {
        String user = newUsername();
        register(user, "s3cret-pw");
        TokenResponse tokens = login(user, "s3cret-pw");

        MvcResult result = mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.refreshToken() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        // The new access token works against the API.
        String newAccess = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        mockMvc.perform(graphql().header("Authorization", "Bearer " + newAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.__typename").value("Query"));
    }

    @Test
    void duplicateRegistrationIsRejected() throws Exception {
        String user = newUsername();
        register(user, "s3cret-pw");
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(user, "another-pw")))
                .andExpect(status().isConflict());
    }

    @Test
    void badCredentialsAreRejected() throws Exception {
        String user = newUsername();
        register(user, "s3cret-pw");
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(user, "wrong-pw")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(newUsername(), "whatever")))
                .andExpect(status().isUnauthorized());
    }
}
