package com.progolf.app.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import java.net.URI;

/**
 * authentication: a non-401 error status from a permitted /auth endpoint survives a real servlet container's
 * ERROR dispatch (which MockMvc does not exercise). Guards the regression where a thrown 409 from a
 * permitAll endpoint was overridden by a 401 on the internal forward to /error. (A 401 case isn't asserted
 * here: it's indistinguishable from the gate and the JDK HTTP client auto-retries 401s; MockMvc covers it.)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthErrorStatusTest {

    @Autowired
    private TestRestTemplate rest;

    private ResponseEntity<String> postJson(String path, String json) {
        RequestEntity<String> request = RequestEntity.post(URI.create(path))
                .contentType(MediaType.APPLICATION_JSON).body(json);
        return rest.exchange(request, String.class);
    }

    private String credentials(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void duplicateRegistrationReturns409ThroughTheContainer() {
        String user = "user-" + UUID.randomUUID();
        assertThat(postJson("/auth/register", credentials(user, "pw-123456")).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        assertThat(postJson("/auth/register", credentials(user, "other-pw")).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }
}
