package com.taskflow.demo;

import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String token;

    @BeforeEach
    void registerAndLogin() {
        String email = "task-user-" + UUID.randomUUID() + "@example.com";
        Map<String, Object> registerBody = Map.of("name", "Task User", "email", email, "password", "password123");
        restTemplate.postForEntity("/api/auth/register", registerBody, Map.class);

        Map<String, Object> loginBody = Map.of("email", email, "password", "password123");
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity("/api/auth/login", loginBody, Map.class);
        this.token = (String) loginResponse.getBody().get("token");
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private ResponseEntity<Map> createTask(String title) {
        Map<String, Object> body = Map.of("title", title, "description", "demo task");
        return restTemplate.exchange("/api/tasks", HttpMethod.POST,
                new HttpEntity<>(body, authHeaders()), Map.class);
    }

    @Test
    void createTaskReturns201WithOwnerAndDefaults() {
        ResponseEntity<Map> response = createTask("Buy milk");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("title")).isEqualTo("Buy milk");
        assertThat(response.getBody().get("status")).isEqualTo("TODO");
        assertThat(response.getBody().get("priority")).isEqualTo("MEDIUM");
        assertThat(response.getBody().get("ownerId")).isNotNull();
    }

    @Test
    void createTaskWithBlankTitleReturns400() {
        Map<String, Object> body = Map.of("title", "   ");
        ResponseEntity<Map> response = restTemplate.exchange("/api/tasks", HttpMethod.POST,
                new HttpEntity<>(body, authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("error");
    }

    @Test
    void getTaskByIdReturnsOwnedTask() {
        Map<?, ?> created = createTask("Read a book").getBody();
        Object id = created.get("id");

        ResponseEntity<Map> response = restTemplate.exchange("/api/tasks/" + id, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("title")).isEqualTo("Read a book");
    }

    @Test
    void getNonExistentTaskReturns404() {
        ResponseEntity<Map> response = restTemplate.exchange("/api/tasks/999999999", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("error");
    }

    @Test
    void updateTaskChangesFieldsAndReturns200() {
        Map<?, ?> created = createTask("Old title").getBody();
        Object id = created.get("id");

        Map<String, Object> update = Map.of("title", "New title", "status", "DONE");
        ResponseEntity<Map> response = restTemplate.exchange("/api/tasks/" + id, HttpMethod.PUT,
                new HttpEntity<>(update, authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("title")).isEqualTo("New title");
        assertThat(response.getBody().get("status")).isEqualTo("DONE");
    }

    @Test
    void updateTaskWithInvalidEnumReturns400() {
        Map<?, ?> created = createTask("Needs status").getBody();
        Object id = created.get("id");

        Map<String, Object> update = Map.of("status", "NOT_A_STATUS");
        ResponseEntity<Map> response = restTemplate.exchange("/api/tasks/" + id, HttpMethod.PUT,
                new HttpEntity<>(update, authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateNonExistentTaskReturns404() {
        Map<String, Object> update = Map.of("title", "Doesn't matter");
        ResponseEntity<Map> response = restTemplate.exchange("/api/tasks/999999999", HttpMethod.PUT,
                new HttpEntity<>(update, authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteTaskReturns204ThenRepeatDeleteReturns404() {
        Map<?, ?> created = createTask("Disposable task").getBody();
        Object id = created.get("id");

        ResponseEntity<Void> firstDelete = restTemplate.exchange("/api/tasks/" + id, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()), Void.class);
        assertThat(firstDelete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> secondDelete = restTemplate.exchange("/api/tasks/" + id, HttpMethod.DELETE,
                new HttpEntity<>(authHeaders()), Map.class);
        assertThat(secondDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listTasksOnlyReturnsOwnTasksAndSupportsStatusFilter() {
        createTask("Task A");
        createTask("Task B");

        ResponseEntity<Map> response = restTemplate.exchange("/api/tasks?status=TODO", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> tasks = (List<?>) response.getBody().get("tasks");
        assertThat(tasks.size()).isGreaterThanOrEqualTo(2);
        assertThat(response.getBody().get("count")).isEqualTo(tasks.size());
    }

    @Test
    void listTasksWithInvalidStatusReturns400() {
        ResponseEntity<Map> response = restTemplate.exchange("/api/tasks?status=NOPE", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void endpointsRequireAuthentication() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/tasks", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("error")).isEqualTo("Unauthorized");
    }
}
