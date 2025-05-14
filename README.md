Repository Structure

![image](https://github.com/user-attachments/assets/7f544a7a-f30f-4b9b-be1d-fec9cb00b526)

Automation-framework Module

Technologies Used:
Rest Assured: For API testing
Cucumber: For BDD-style test definitions
Allure: For generating HTML reports
Zephyr Squad Cloud integration via a dedicated Java CLI tool
Jira tag parsing to map Cucumber scenarios to Jira test cases



Sample Feature File (api_tests.feature):

Feature: API Testing with Jira Integration

  @CBDIDD-123
  Scenario: Verify user creation API
    Given I create a new user with name "John Doe"
    When I send a POST request to "/users"
    Then the response status should be 201

Step Definitions (ApiStepDefinitions.java):

import io.cucumber.java.en.*;
import static io.restassured.RestAssured.*;

public class ApiStepDefinitions {

    @Given("I create a new user with name {string}")
    public void i_create_a_new_user(String name) {
        // Implementation to create user payload
    }

    @When("I send a POST request to {string}")
    public void i_send_post_request(String endpoint) {
        // Implementation to send POST request
    }

    @Then("the response status should be {int}")
    public void the_response_status_should_be(int statusCode) {
        // Implementation to verify response status
    }
}


---

 Zephyr-updater-cli Module

Purpose:

A standalone Java CLI tool that :
- Parses Cucumber reports to extract Jira test case keys
- Updates test execution status in Zephyr Squad Cloud via GraphQL
- Attaches the Allure HTML report to the corresponding Jira test case

Usage:

java -jar zephyr-updater-cli.jar \
  --report-path=path/to/allure-report.zip \
  --status=Pass \
  --jira-keys=JIRA-TEST-123,JIRA-TEST-456

Key Components:

ExecutionUpdater.java

Parses input arguments
Calls ZephyrGraphQLClient to update test execution status
Calls JiraAttachmentService to attach the Allure report


ZephyrGraphQLClient.java

Handles GraphQL mutations to create/update test executions in Zephyr Squad Cloud


JiraAttachmentService.java

Uses Jira REST API to attach files to test cases


Integration Workflow

1. Test Execution:

Run tests using the automation-framework module.
Generate Allure HTML report.


2. Post-Execution:

Use the zephyr-updater-cli tool to:
Parse the Allure report and extract Jira test case keys.
Update test execution status in Zephyr Squad Cloud.
Attach the Allure report to the corresponding Jira test cases.

