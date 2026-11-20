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
class DashboardControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String registerAndLogin() {
        String email = "dash-" + UUID.randomUUID() + "@example.com";
        Map<String, Object> registerBody = Map.of("name", "Dash User", "email", email, "password", "password123");
        restTemplate.postForEntity("/api/auth/register", registerBody, Map.class);

        Map<String, Object> loginBody = Map.of("email", email, "password", "password123");
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity("/api/auth/login", loginBody, Map.class);
        return (String) loginResponse.getBody().get("token");
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private void createTaskWithStatus(String token, String title, String status) {
        Map<String, Object> body = Map.of("title", title, "status", status);
        restTemplate.exchange("/api/tasks", HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), Map.class);
    }

    @Test
    void dashboardReturnsCorrectCountsForOwnTasksOnly() {
        String token = registerAndLogin();

        createTaskWithStatus(token, "Task 1", "TODO");
        createTaskWithStatus(token, "Task 2", "TODO");
        createTaskWithStatus(token, "Task 3", "IN_PROGRESS");
        createTaskWithStatus(token, "Task 4", "DONE");

        ResponseEntity<Map> response = restTemplate.exchange("/api/dashboard", HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("total")).isEqualTo(4);
        assertThat(response.getBody().get("todo")).isEqualTo(2);
        assertThat(response.getBody().get("inProgress")).isEqualTo(1);
        assertThat(response.getBody().get("done")).isEqualTo(1);
    }

    @Test
    void dashboardRequiresAuthentication() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/dashboard", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
