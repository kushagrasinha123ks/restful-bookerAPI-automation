package com.kushagra.booker.tests;

import com.kushagra.booker.support.BaseApiTest;
import io.qameta.allure.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.util.Map;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

@Epic("Restful Booker")
@Feature("Authentication")
@Test(groups = "regression")
public class AuthTest extends BaseApiTest {
    @Test(groups = {"smoke", "contract"})
    @Story("Obtain a token")
    public void validCredentialsReturnNonEmptyToken() {
        auth.createToken().then().statusCode(200).contentType("application/json")
                .body(matchesJsonSchemaInClasspath("schemas/token.json"))
                .body("token", not(emptyOrNullString()))
                .body("reason", nullValue());
    }

    @DataProvider(name = "invalidCredentials")
    public Object[][] invalidCredentials() {
        return new Object[][] {
            {"wrong username and password", Map.of("username", "not-a-booker-user", "password", "incorrect")},
            {"missing username", Map.of("password", "incorrect")},
            {"missing password", Map.of("username", "not-a-booker-user")},
            {"empty credentials", Map.of("username", "", "password", "")},
            {"empty object", Map.of()}
        };
    }

    @Test(dataProvider = "invalidCredentials", groups = {"negative", "contract"})
    @Story("Reject invalid credentials")
    @Description("The application returns HTTP 200 with an error reason for rejected credentials; a token must never be issued.")
    public void invalidCredentialsReturnErrorWithoutToken(String scenario, Map<String, String> payload) {
        auth.createToken(payload).then().statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/auth-error.json"))
                .body("reason", equalTo("Bad credentials"))
                .body("token", nullValue());
    }

    @Test(groups = {"negative", "contract"})
    @Description("A valid username alone must not authenticate when the password is wrong; configured credentials are not DataProvider/report parameters.")
    public void validUsernameWithWrongPasswordDoesNotIssueToken() {
        auth.createToken(Map.of("username", config.username(), "password", "incorrect-password"))
                .then().statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/auth-error.json"))
                .body("reason", equalTo("Bad credentials")).body("token", nullValue());
    }

    @Test(groups = {"negative", "contract"})
    @Description("A valid password alone must not authenticate when the username is wrong; configured credentials are not DataProvider/report parameters.")
    public void wrongUsernameWithValidPasswordDoesNotIssueToken() {
        auth.createToken(Map.of("username", "not-a-booker-user", "password", config.password()))
                .then().statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/auth-error.json"))
                .body("reason", equalTo("Bad credentials")).body("token", nullValue());
    }

    @Test(groups = "negative")
    public void malformedAuthenticationJsonReturnsBadRequest() {
        auth.createToken("{\"username\":").then().statusCode(400);
    }
}
