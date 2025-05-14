package steps;

import io.cucumber.java.en.*;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class ApiStepDefinitions {

    private String baseUrl = "https://jsonplaceholder.typicode.com";
    private io.restassured.response.Response response;

    @Given("I create a new user with name {string}")
    public void i_create_a_new_user(String name) {
        String payload = "{ \"name\": \"" + name + "\" }";
        response = given()
                        .header("Content-Type", "application/json")
                        .body(payload)
                   .when()
                        .post(baseUrl + "/users");
    }

    @Then("the response status should be {int}")
    public void the_response_status_should_be(int statusCode) {
        response.then().statusCode(statusCode);
    }
}
