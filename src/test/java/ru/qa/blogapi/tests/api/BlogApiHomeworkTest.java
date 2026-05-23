package ru.qa.blogapi.tests.api;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.qa.blogapi.base.BaseAuthorizedApiTest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class BlogApiHomeworkTest extends BaseAuthorizedApiTest {

    @Test
    @DisplayName("POST /api/auth/register -> should register user with valid required fields")
    void shouldRegisterUserWithValidRequiredFields() {
        String email = randomEmail();
        String password = "SecurePass123!";

        Map<String, Object> body = registrationBody(email, password);

        given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("message", equalTo("User registered successfully"))
                .body("user.id", notNullValue())
                .body("user.email", equalTo(email))
                .body("user.firstName", equalTo(body.get("firstName")))
                .body("user.lastName", equalTo(body.get("lastName")))
                .body("user.nickname", equalTo(body.get("nickname")))
                .body("user.birthDate", equalTo(body.get("birthDate")))
                .body("user.phone", equalTo(body.get("phone")));
    }

    @Test
    @DisplayName("POST /api/auth/register -> should return validation error for invalid email")
    void shouldReturnValidationErrorForInvalidEmailOnRegistration() {
        Map<String, Object> body = registrationBody("invalid-email", "SecurePass123!");

        given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(400)
                .body("error", notNullValue())
                .body("error.code", equalTo(400))
                .body("error.message", notNullValue());
    }

    @Test
    @DisplayName("POST /api/login -> should login with valid credentials")
    void shouldLoginWithValidCredentials() {
        String email = randomEmail();
        String password = "SecurePass123!";

        registerUser(email, password);

        given()
                .spec(requestSpec)
                .body(loginBody(email, password))
                .when()
                .post("/api/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("refresh_token", notNullValue());
    }

    @Test
    @Tag("regression")
    @DisplayName("POST /api/login -> should return 401 with wrong password")
    void shouldReturn401WithWrongPassword() {
        String email = randomEmail();
        String password = "SecurePass123!";
        registerUser(email, password);

        given()
                .spec(requestSpec)
                .body(loginBody(email, "WrongPassword999!"))
                .when()
                .post("/api/login")
                .then()
                .statusCode(401)
                .body("message", notNullValue());
    }

    @Test
    @Tag("regression")
    @DisplayName("POST /api/token/refresh -> should refresh JWT token with valid refresh token")
    void shouldRefreshJwtTokenWithValidRefreshToken() {
        String email = randomEmail();
        String password = "SecurePass123!";
        registerUser(email, password);

        String refreshToken = given()
                .spec(requestSpec)
                .body(loginBody(email, password))
                .when()
                .post("/api/login")
                .then()
                .statusCode(200)
                .extract()
                .path("refresh_token");

        Map<String, Object> refreshBody = new HashMap<>();
        refreshBody.put("refresh_token", refreshToken);

        given()
                .spec(requestSpec)
                .body(refreshBody)
                .when()
                .post("/api/token/refresh")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("refresh_token", notNullValue());
    }

    @Test
    @Tag("smoke")
    @DisplayName("GET /api/profile -> should return current user profile with valid token")
    void shouldReturnCurrentUserProfileWithValidToken() {
        given()
                .spec(authorizedRequestSpec)
                .when()
                .get("/api/profile")
                .then()
                .statusCode(200)
                .body("user.id", notNullValue())
                .body("user.firstName", notNullValue())
                .body("user.lastName", notNullValue())
                .body("user.nickname", notNullValue())
                .body("user.birthDate", notNullValue());
    }

    @Test
    @Tag("e2e")
    @DisplayName("E2E: create post -> get by id -> update -> delete")
    void shouldCreateGetUpdateAndDeletePost() {
        // Создаём
        Map<String, Object> createBody = postCreateBody(
                "E2E Post " + suffix(5),
                "education"
        );
        int postId = given()
                .spec(authorizedRequestSpec)
                .body(createBody)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .extract()
                .path("post.id");

        // Получаем по id
        given()
                .spec(authorizedRequestSpec)
                .when()
                .get("/api/posts/" + postId)
                .then()
                .statusCode(200)
                .body("post.id", equalTo(postId))
                .body("post.category", equalTo("education"));

        // Обновляем
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("title", "Updated E2E Post");
        updateBody.put("category", "business");

        given()
                .spec(authorizedRequestSpec)
                .body(updateBody)
                .when()
                .put("/api/posts/" + postId)
                .then()
                .statusCode(200)
                .body("post.title", equalTo("Updated E2E Post"))
                .body("post.category", equalTo("business"));

        // Удаляем
        given()
                .spec(authorizedRequestSpec)
                .when()
                .delete("/api/posts/" + postId)
                .then()
                .statusCode(200)
                .body("status", equalTo("success"));

        // Проверяем, что пост недоступен
        given()
                .spec(authorizedRequestSpec)
                .when()
                .get("/api/posts/" + postId)
                .then()
                .statusCode(404);
    }

    @Test
    @Tag("smoke")
    @DisplayName("POST /api/feedback -> should submit feedback successfully")
    void shouldSubmitFeedbackSuccessfully() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Test User " + suffix(4));
        body.put("email", randomEmail());
        body.put("content", "This is great feedback from automated test!");

        given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post("/api/feedback")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("message", equalTo("Feedback submitted successfully"))
                .body("feedback.id", notNullValue())
                .body("feedback.email", equalTo(body.get("email")))
                .body("feedback.content", equalTo(body.get("content")));
    }

    @Test
    @Tag("regression")
    @DisplayName("GET /api/posts -> should return paginated list of posts")
    void shouldReturnPaginatedListOfPosts() {
        given()
                .spec(authorizedRequestSpec)
                .queryParam("page", 1)
                .queryParam("limit", 5)
                .when()
                .get("/api/posts")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("page", equalTo(1))
                .body("itemsPerPage", equalTo(5))
                .body("totalItems", greaterThanOrEqualTo(0));
    }

    @Test
    @Tag("e2e")
    @DisplayName("POST /api/posts/{id}/favorite -> should add post to favorites")
    void shouldAddPostToFavorites() {
        // создаём пост
        Map<String, Object> createBody = postCreateBody("Fav Post " + suffix(5), "food");
        int postId = given()
                .spec(authorizedRequestSpec)
                .body(createBody)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .extract()
                .path("post.id");

        // добавляем в избранное
        Map<String, Object> favBody = new HashMap<>();
        favBody.put("isFavorite", true);

        given()
                .spec(authorizedRequestSpec)
                .body(favBody)
                .when()
                .post("/api/posts/" + postId + "/favorite")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("isFavorite", equalTo(true));

        // проверяем, что пост попал в избранное
        given()
                .spec(authorizedRequestSpec)
                .when()
                .get("/api/posts/favorites")
                .then()
                .statusCode(200)
                .body("items.id", hasItem(postId));
    }

    @Test
    @Tag("smoke")
    @DisplayName("GET /api/profile -> should return 401 without authorization token")
    void shouldReturn401WhenGettingProfileWithoutToken() {
        given()
                .spec(requestSpec)
                .when()
                .get("/api/profile")
                .then()
                .statusCode(401)
                .body("error", notNullValue());
    }

    @Test
    @Tag("regression")
    @DisplayName("PUT /api/profile -> should update current user profile")
    void shouldUpdateCurrentUserProfile() {
        String newFirstName = "Updated_" + suffix(5);
        String newLastName = "User_" + suffix(5);

        Map<String, Object> body = new HashMap<>();
        body.put("firstName", newFirstName);
        body.put("lastName", newLastName);

        given()
                .spec(authorizedRequestSpec)
                .body(body)
                .when()
                .put("/api/profile")
                .then()
                .statusCode(200)
                .body("status", equalTo("success"))
                .body("user.firstName", equalTo(newFirstName))
                .body("user.lastName", equalTo(newLastName));
    }

    @Test
    @Tag("smoke")
    @DisplayName("POST /api/posts -> should create a new post with valid data")
    void shouldCreateNewPostWithValidData() {
        Map<String, Object> body = postCreateBody(
                "My Awesome Post " + suffix(5),
                "technology"
        );

        given()
                .spec(authorizedRequestSpec)
                .body(body)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .body("status", equalTo("success"))
                .body("message", equalTo("Post created successfully"))
                .body("post.id", notNullValue())
                .body("post.title", equalTo(body.get("title")))
                .body("post.category", equalTo("technology"))
                .body("post.author.id", notNullValue());
    }

    @Test
    @Tag("regression")
    @DisplayName("POST /api/posts -> should return 401 when creating post without auth")
    void shouldReturn401WhenCreatingPostWithoutAuth() {
        Map<String, Object> body = postCreateBody("Unauthorized post", "travel");

        given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(401)
                .body("error", notNullValue());
    }

    @Test
    @Tag("regression")
    @DisplayName("GET /api/posts/my -> should return only current user's posts")
    void shouldReturnOnlyCurrentUserPosts() {
        // создаём пост
        Map<String, Object> createBody = postCreateBody("My Post " + suffix(5), "fitness");
        int postId = given()
                .spec(authorizedRequestSpec)
                .body(createBody)
                .when()
                .post("/api/posts")
                .then()
                .statusCode(201)
                .extract()
                .path("post.id");

        // запрашиваем "мои" посты — созданный должен быть в списке
        given()
                .spec(authorizedRequestSpec)
                .queryParam("page", 1)
                .queryParam("limit", 50)
                .when()
                .get("/api/posts/my")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("items.id", hasItem(postId))
                .body("page", equalTo(1));
    }

    @Test
    @Tag("regression")
    @DisplayName("GET /api/posts/feed -> should return feed posts from other users")
    void shouldReturnFeedPostsFromOtherUsers() {
        given()
                .spec(authorizedRequestSpec)
                .queryParam("page", 1)
                .queryParam("limit", 10)
                .when()
                .get("/api/posts/feed")
                .then()
                .statusCode(200)
                .body("items", notNullValue())
                .body("itemsPerPage", equalTo(10))
                .body("totalItems", greaterThanOrEqualTo(0));
    }

    @Test
    @Tag("regression")
    @DisplayName("GET /api/profile/{id} -> should return 404 for non-existent user profile")
    void shouldReturn404ForNonExistentUserProfile() {
        int nonExistentId = 99999999;

        given()
                .spec(authorizedRequestSpec)
                .when()
                .get("/api/profile/" + nonExistentId)
                .then()
                .statusCode(404)
                .body("error", notNullValue());
    }

    @Test
    @Tag("e2e")
    @DisplayName("E2E: submit feedback -> get it by id")
    void shouldSubmitAndRetrieveFeedbackById() {
        // отправляем feedback
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Feedback Author " + suffix(4));
        body.put("email", randomEmail());
        body.put("content", "Feedback content " + suffix(8));

        int feedbackId = given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post("/api/feedback")
                .then()
                .statusCode(200)
                .extract()
                .path("feedback.id");

        // получаем его обратно
        given()
                .spec(requestSpec)
                .when()
                .get("/api/feedback/" + feedbackId)
                .then()
                .statusCode(200)
                .body("feedback.id", equalTo(feedbackId))
                .body("feedback.email", equalTo(body.get("email")))
                .body("feedback.content", equalTo(body.get("content")));
    }

    private Response registerUser(String email, String password) {
        return given()
                .spec(requestSpec)
                .body(registrationBody(email, password))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200)
                .extract()
                .response();
    }

    private Map<String, Object> postCreateBody(String title, String category) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("body", "This is the full body text of the test post. " +
                "It contains enough text to be a meaningful post for testing purposes.");
        body.put("description", "Short test description");
        body.put("category", category);
        body.put("isDraft", false);
        return body;
    }

    private Map<String, Object> registrationBody(String email, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("firstName", "Ronam");
        body.put("lastName", "Doe");
        body.put("nickname", "roman_" + suffix(5));
        body.put("birthDate", "1990-01-02");
        body.put("phone", randomPhone());
        return body;
    }

    private Map<String, Object> loginBody(String email, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", email);
        body.put("password", password);
        return body;
    }

    private String randomEmail() {
        return "student_" + suffix(8) + "@example.com";
    }

    private String randomPhone() {
        return "+79" + UUID.randomUUID()
                .toString()
                .replaceAll("[^0-9]", "")
                .substring(0, 9);
    }

    private String suffix(int length) {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, length);
    }

}