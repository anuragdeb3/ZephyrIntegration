# Zephyr Integration

- Parses Allure JSON reports
- Creates a test cycle in Zephyr Squad
- Adds test cases to the cycle
- Updates their execution status
- Optionally uploads Allure HTML reports


##1. Fetch Test Case IDs from Allure Reports
Allure JSON files (allure-results) contain metadata such as:

-name
-status
-labels (where you can include testCaseId if you tag it in code)

###Sample Allure JSON (*.json) content:
```
{
  "name": "Login API Test",
  "status": "passed",
  "labels": [
    {"name": "testCaseId", "value": "JIRA-123"},
    {"name": "severity", "value": "critical"}
  ]
}
```
You need to:

- Parse all JSON files under allure-results/
- Extract "testCaseId" from labels

Tip: Tag your test cases with @Allure.label("testCaseId", "JIRA-123") in test code.

##2. Create Test Cycle in JIRA Zephyr Squad
API: Use Zephyr Squad API
(usually accessible under https://<your-domain>.atlassian.net/rest/zephyr/latest/...)

Sample Request:
```
POST /rest/zapi/latest/cycle
Authorization: Bearer <token>

{
  "name": "Automation Cycle - May 16",
  "projectId": <PROJECT_ID>,
  "versionId": -1
}
```
Response will include "id" of the created cycle.

##3. Add Test Cases to Cycle
Use the Zephyr Squad API to add test cases by issue keys (JIRA-123, etc.) into the cycle.

```
POST /rest/zapi/latest/execution
Authorization: Bearer <token>

{
  "issues": ["JIRA-123", "JIRA-124"],
  "cycleId": "<cycleId>",
  "projectId": "<projectId>",
  "versionId": -1
}
```

This will create executions for each test case in the cycle.

Save the response to map issue keys to execution IDs.

4. Update Status of Test Executions
Allure provides statuses like passed, failed, etc. You’ll need to map them to Zephyr statuses:
```
Allure Status	Zephyr Status
passed	1 (Pass)
failed	2 (Fail)
broken	4 (Blocked)
```

Update Execution:
```
PUT /rest/zapi/latest/execution/<executionId>/execute
Authorization: Bearer <token>

{
  "status": 1
}
```
You can also attach Allure reports (PDF/HTML) or URLs as attachments/comments.

## Suggested Automation Pipeline Integration

1. Post-Test Stage in CI:

- Allure report is generated.
- Trigger your utility script.

2. Run Utility Script:

Step 1: Parse JSON results and collect IDs.
Step 2: Create JIRA cycle.
Step 3: Push test cases to cycle.
Step 4: Update execution results.

## Sample File/Script Structure
'''
/test-sync
  ├── parseAllure.ts
  ├── jiraClient.ts
  ├── zephyrClient.ts
  ├── syncToJira.ts
  ├── config.json (tokens, projectId, etc.)
'''

## Authentication with Zephyr Squad

Instead of JIRA API tokens, use Zephyr Squad Access Key + Secret Key (used for generating JWTs for each request).

```
public void uploadAttachment(String executionId, File file) throws IOException {
    String uri = "/public/rest/api/1.0/attachment?entityId=" + executionId + "&entityType=EXECUTION";

    HttpPost request = new HttpPost(baseUrl + uri);
    signRequest("POST", uri, "", request); // Auth headers

    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    builder.addBinaryBody("file", file, ContentType.APPLICATION_OCTET_STREAM, file.getName());

    request.setEntity(builder.build());

    HttpResponse response = httpClient.execute(request);
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode != 200) {
        System.err.println("❌ Failed to upload attachment for " + executionId + ". HTTP " + statusCode);
    } else {
        System.out.println("📎 Attachment uploaded for execution: " + executionId);
    }
}
```
