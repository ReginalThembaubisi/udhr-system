package com.udhr.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Status codes through a real server, not MockMvc. MockMvc skips the error
 * dispatch a real server does after a refusal, which is how every 403 came out
 * as 401 on Render while the other tests passed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealServerStatusTest {

    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper json;

    @Test
    void wrongRoleIs403AndNoSignInIs401() throws Exception {
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        loginHeaders.set("X-Forwarded-For", "10.0.8.1");
        String body = rest.postForObject("/api/auth/login",
                new HttpEntity<>(Map.of("staffNumber", "ADMIN001", "password", "Admin@123"), loginHeaders), String.class);
        String token = json.readTree(body).get("token").asText();

        HttpHeaders frontDesk = new HttpHeaders();
        frontDesk.setBearerAuth(token);
        ResponseEntity<String> forbidden = rest.exchange("/api/visits/today", HttpMethod.GET, new HttpEntity<>(frontDesk), String.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<String> anonymous = rest.getForEntity("/api/visits/today", String.class);
        assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
