package com.taskflow.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void meReturnsAuthenticatedUserWithValidToken() {
        String email = "me-" + UUID.randomUUID() + "@example.com";
        restTemplate.postForEntity("/api/auth/register",
                Map.of("name", "Me User", "email", email, "password", "password123"), Map.class);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity("/api/auth/login",
                Map.of("email", email, "password", "password123"), Map.class);
        String token = (String) loginResponse.getBody().get("token");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<Map> response = restTemplate.exchange("/api/users/me", HttpMethod.GET,
                new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("email")).isEqualTo(email);
        assertThat(response.getBody()).doesNotContainKey("passwordHash");
    }

    @Test
    void meWithoutTokenReturns401() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/users/me", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("error")).isEqualTo("Unauthorized");
    }

    @Test
    void meWithMalformedTokenReturns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer not-a-real-token");
        ResponseEntity<Map> response = restTemplate.exchange("/api/users/me", HttpMethod.GET,
                new HttpEntity<>(headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
