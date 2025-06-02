# Zephyr Integration

- Parses Allure JSON reports
- Creates a test cycle in Zephyr Squad
- Adds test cases to the cycle
- Updates their execution status
- Optionally uploads Allure HTML reports


## 1. Fetch Test Case IDs from Allure Reports
Allure JSON files (allure-results) contain metadata such as:

-name
-status
-labels (where you can include testCaseId if you tag it in code)

### Sample Allure JSON (*.json) content:
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

## 2. Create Test Cycle in JIRA Zephyr Squad
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

## 3. Add Test Cases to Cycle
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

## 4. Update Status of Test Executions
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
```
/test-sync
  ├── parseAllure.ts
  ├── jiraClient.ts
  ├── zephyrClient.ts
  ├── syncToJira.ts
  ├── config.json (tokens, projectId, etc.)
```

## Authentication with Zephyr Squad

Instead of JIRA API tokens, use Zephyr Squad Access Key + Secret Key (used for generating JWTs for each request).

```
<dependency>
  <groupId>com.auth0</groupId>
  <artifactId>java-jwt</artifactId>
  <version>3.18.2</version>
</dependency>

```
**JWT Generation Code**
```
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Date;

public class ZephyrJwtGenerator {

    public static String generateJWT(String accessKey, String secretKey, String accountId,
                                     String httpMethod, String uriPath, String queryString) {

        // Canonical path format: METHOD&URI&QUERY
        String canonicalPath = httpMethod.toUpperCase() + "&" + uriPath + "&" + queryString;
        String qsh = DigestUtils.sha256Hex(canonicalPath);

        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + 3600_000; // 1 hour expiration

        Algorithm algorithm = Algorithm.HMAC256(secretKey);

        return JWT.create()
                .withIssuer(accessKey)        // 'iss'
                .withSubject(accountId)       // 'sub'
                .withClaim("qsh", qsh)        // 'qsh' = Query String Hash
                .withIssuedAt(new Date(nowMillis))
                .withExpiresAt(new Date(expMillis))
                .sign(algorithm);
    }
}
```
**NTLM Proxy Usage**
```
import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.auth.params.*;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.*;
import org.apache.http.impl.auth.NTCredentials;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.conn.routing.*;

import java.io.IOException;
import java.util.Map;

public class NTLMHttpClient {

    public static String sendNTLMRequest(
            String method,
            String url,
            String proxyHost,
            int proxyPort,
            String username,
            String password,
            String domain,
            String workstation,
            String payloadJson, // Optional for POST/PUT
            Map<String, String> headers
    ) throws IOException {

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(proxyHost, proxyPort),
                new NTCredentials(username, password, workstation, domain)
        );

        HttpHost proxy = new HttpHost(proxyHost, proxyPort);
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);

        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .setRoutePlanner(routePlanner)
                .build()) {

            HttpRequestBase request;

            switch (method.toUpperCase()) {
                case "POST":
                    HttpPost post = new HttpPost(url);
                    if (payloadJson != null) {
                        post.setEntity(new StringEntity(payloadJson, ContentType.APPLICATION_JSON));
                    }
                    request = post;
                    break;
                case "PUT":
                    HttpPut put = new HttpPut(url);
                    if (payloadJson != null) {
                        put.setEntity(new StringEntity(payloadJson, ContentType.APPLICATION_JSON));
                    }
                    request = put;
                    break;
                case "DELETE":
                    request = new HttpDelete(url);
                    break;
                case "GET":
                default:
                    request = new HttpGet(url);
            }

            // Add headers
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-Type", "application/json");
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    request.setHeader(entry.getKey(), entry.getValue());
                }
            }

            try (CloseableHttpResponse response = client.execute(request)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }
}

'''
String method = "get";
String path = "/executions/search/cycle/38373-7373-7373";

Map<String, String> queryParams = new TreeMap<>(); // TreeMap = auto-sorts keys
queryParams.put("project", "67688");
queryParams.put("versionid", "-1");

StringBuilder queryBuilder = new StringBuilder();
for (Map.Entry<String, String> entry : queryParams.entrySet()) {
    if (queryBuilder.length() > 0) queryBuilder.append("&");
    queryBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                .append("=")
                .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
}

String canonicalRequest = method + "&" + path + "&" + queryBuilder.toString();

MessageDigest digest = MessageDigest.getInstance("SHA-256");
byte[] hash = digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));
StringBuilder hexString = new StringBuilder();
for (byte b : hash) {
    String hex = Integer.toHexString(0xff & b);
    if (hex.length() == 1) hexString.append('0');
    hexString.append(hex);
}
String qsh = hexString.toString();
