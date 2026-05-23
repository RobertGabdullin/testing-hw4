package ru.qa.blogapi.tests.ui;

import io.restassured.http.ContentType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import ru.qa.blogapi.base.BaseUiTest;
import ru.qa.blogapi.pages.LoginPage;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

class LoginUiTest extends BaseUiTest {

    private static final Log log = LogFactory.getLog(LoginUiTest.class);

    @Test
    @Tag("regression")
    @DisplayName("UI /login -> should login with existing user credentials")
    void shouldLoginWithExistingUserCredentials() {
        String email = randomEmail();
        String password = "SecurePass123!";

        // подготовка юзера через API
        registerUserViaApi(email, password);

        LoginPage loginPage = new LoginPage(driver);
        loginPage.open(uiBaseUrl);

        // шаги
        //ввести почту
        loginPage.fillEmail(email);
        //ввести пароль
        loginPage.fillPassword(password);
        //клик логин
        loginPage.clickLogin();

        // проверка, что мы ушли со страницы логина
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlToBe(uiBaseUrl + "/"));
    }

    private void registerUserViaApi(String email, String password) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("firstName", "Ronam");
        body.put("lastName", "Doe");
        body.put("nickname", "roman_" + suffix(5));
        body.put("birthDate", "1990-01-02");
        body.put("phone", randomPhone());

        //вызов /api/auth/register
        given()
                .contentType(ContentType.JSON)
                .baseUri(apiBaseUrl)
                .body(body)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200);
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