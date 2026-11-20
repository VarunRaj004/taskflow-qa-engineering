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

/**
 * Verifies that a task owned by one user is completely invisible to another authenticated user:
 * GET/PUT/DELETE on someone else's task id must behave exactly like the id not existing (404).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TaskOwnershipIsolationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String registerAndLogin(String name) {
        String email = "owner-" + UUID.randomUUID() + "@example.com";
        Map<String, Object> registerBody = Map.of("name", name, "email", email, "password", "password123");
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

    @Test
    void userCannotReadUpdateOrDeleteAnotherUsersTask() {
        String tokenA = registerAndLogin("User A");
        String tokenB = registerAndLogin("User B");

        Map<String, Object> body = Map.of("title", "User A's private task");
        ResponseEntity<Map> createResponse = restTemplate.exchange("/api/tasks", HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(tokenA)), Map.class);
        Object taskId = createResponse.getBody().get("id");

        ResponseEntity<Map> getAsB = restTemplate.exchange("/api/tasks/" + taskId, HttpMethod.GET,
                new HttpEntity<>(authHeaders(tokenB)), Map.class);
        assertThat(getAsB.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        Map<String, Object> update = Map.of("title", "Hijacked title");
        ResponseEntity<Map> updateAsB = restTemplate.exchange("/api/tasks/" + taskId, HttpMethod.PUT,
                new HttpEntity<>(update, authHeaders(tokenB)), Map.class);
        assertThat(updateAsB.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<Map> deleteAsB = restTemplate.exchange("/api/tasks/" + taskId, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(tokenB)), Map.class);
        assertThat(deleteAsB.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // The task is still intact and visible to its real owner.
        ResponseEntity<Map> getAsA = restTemplate.exchange("/api/tasks/" + taskId, HttpMethod.GET,
                new HttpEntity<>(authHeaders(tokenA)), Map.class);
        assertThat(getAsA.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getAsA.getBody().get("title")).isEqualTo("User A's private task");
    }

    @Test
    void listTasksDoesNotLeakOtherUsersTasks() {
        String tokenA = registerAndLogin("User C");
        String tokenB = registerAndLogin("User D");

        restTemplate.exchange("/api/tasks", HttpMethod.POST,
                new HttpEntity<>(Map.of("title", "C's task"), authHeaders(tokenA)), Map.class);

        ResponseEntity<Map> listAsB = restTemplate.exchange("/api/tasks", HttpMethod.GET,
                new HttpEntity<>(authHeaders(tokenB)), Map.class);

        assertThat(listAsB.getBody().get("count")).isEqualTo(0);
    }
}
