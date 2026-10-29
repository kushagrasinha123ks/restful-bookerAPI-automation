package com.kushagra.booker.tests;

import com.kushagra.booker.support.BaseApiTest;
import io.qameta.allure.*;
import org.testng.annotations.Test;
import static org.hamcrest.Matchers.equalTo;

@Epic("Restful Booker")
@Feature("Health check")
@Test(groups = "regression")
public class HealthTest extends BaseApiTest {
    @Test(groups = {"smoke", "contract"})
    @Description("GET /ping intentionally returns 201 Created, rather than the conventional 200.")
    public void serviceHealthCheckReturnsCreated() {
        health.ping().then().statusCode(201).body(equalTo("Created"));
    }

    @Test(groups = "negative")
    @Description("The documented health endpoint supports GET. POST has no route and returns 404.")
    public void unsupportedHealthMethodReturnsNotFound() {
        health.postPing().then().statusCode(404);
    }
}
