package com.taskflow.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NOTE on {@link #restTemplateBuilder()}: the JDK's default HttpURLConnection-based HTTP client
 * cannot replay a POST request body once it finds out the response is a 401, which otherwise
 * surfaces as "cannot retry due to server authentication, in streaming mode" instead of the
 * actual 401 response. Using Apache HttpClient as the request factory avoids that JDK limitation
 * in these tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AuthControllerTest.TestClientConfig.class)
class AuthControllerTest {

    @TestConfiguration
    static class TestClientConfig {
        @Bean
        RestTemplateBuilder restTemplateBuilder() {
            return new RestTemplateBuilder()
                    .requestFactory((java.util.function.Supplier<org.springframework.http.client.ClientHttpRequestFactory>)
                            HttpComponentsClientHttpRequestFactory::new);
        }
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private Map<String, Object> registerBody(String name, String email, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("password", password);
        return body;
    }

    @Test
    void registerWithValidDataReturns201AndUserBody() {
        String email = uniqueEmail();
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/auth/register", registerBody("Alice", email, "password123"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKeys("id", "name", "email", "createdAt");
        assertThat(response.getBody().get("email")).isEqualTo(email);
        assertThat(response.getBody()).doesNotContainKey("password");
        assertThat(response.getBody()).doesNotContainKey("passwordHash");
    }

    @Test
    void registerWithDuplicateEmailReturns409() {
        String email = uniqueEmail();
        restTemplate.postForEntity("/api/auth/register", registerBody("Alice", email, "password123"), Map.class);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/auth/register", registerBody("Alice Clone", email, "anotherPassword1"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("error")).isEqualTo("Email already registered");
    }

    @Test
    void registerWithMalformedEmailReturns400() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/auth/register", registerBody("Bob", "not-an-email", "password123"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("error");
    }

    @Test
    void registerWithWeakPasswordReturns400() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/auth/register", registerBody("Carol", uniqueEmail(), "short"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("error");
    }

    @Test
    void loginWithValidCredentialsReturns200AndToken() {
        String email = uniqueEmail();
        restTemplate.postForEntity("/api/auth/register", registerBody("Dana", email, "password123"), Map.class);

        Map<String, Object> loginBody = Map.of("email", email, "password", "password123");
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/login", loginBody, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKeys("token", "user");
        Map<?, ?> user = (Map<?, ?>) response.getBody().get("user");
        assertThat(user.get("email")).isEqualTo(email);
    }

    @Test
    void loginWithWrongPasswordReturns401() {
        String email = uniqueEmail();
        restTemplate.postForEntity("/api/auth/register", registerBody("Erin", email, "password123"), Map.class);

        Map<String, Object> loginBody = Map.of("email", email, "password", "wrongPassword1");
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/login", loginBody, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("error")).isEqualTo("Invalid email or password");
    }

    @Test
    void loginWithUnknownUserReturns401WithSameMessage() {
        Map<String, Object> loginBody = Map.of("email", uniqueEmail(), "password", "password123");
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/login", loginBody, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("error")).isEqualTo("Invalid email or password");
    }
}
