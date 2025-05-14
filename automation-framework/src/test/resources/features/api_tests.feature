Feature: API Testing with Jira Integration

  @JIRA-TEST-123
  Scenario: Verify user creation API
    Given I create a new user with name "John Doe"
    Then the response status should be 201
