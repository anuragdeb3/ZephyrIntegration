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
## ✅ How to Actually See the Proxy-Authorization Header
### ✅ Option 1: Use Apache HttpClient Wire Logging (best for proxy debugging)
Add the following JVM arguments to enable low-level logging:

```
-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog
-Dorg.apache.commons.logging.simplelog.log.org.apache.http=DEBUG
-Dorg.apache.commons.logging.simplelog.log.org.apache.http.wire=DEBUG
```
This will print output like:
```
CONNECT proxyhost:8080 HTTP/1.1
Proxy-Authorization: Basic ZHVtbXk6cGFzc3dvcmQ=
```
👉 This is the only reliable way to see proxy handshake, CONNECT, and proxy headers like Proxy-Authorization.

### Option 2: Enable Full Wire Logs Programmatically (if not using command line)
You can do this in your test code (not recommended for production):

```
System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "DEBUG");
System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "DEBUG");
```

```
HttpRequestInterceptor requestLogger = (request, context) -> {
    System.out.println("=== HTTP Request ===");
    System.out.println(request.getRequestLine());
    for (Header header : request.getAllHeaders()) {
        System.out.println(header.getName() + ": " + header.getValue());
    }
};

HttpResponseInterceptor responseLogger = (response, context) -> {
    System.out.println("=== HTTP Response ===");
    System.out.println(response.getStatusLine());
    for (Header header : response.getAllHeaders()) {
        System.out.println(header.getName() + ": " + header.getValue());
    }
};

try (CloseableHttpClient client = HttpClients.custom()
        .setDefaultCredentialsProvider(credsProvider)
        .setRoutePlanner(routePlanner)
        .addInterceptorFirst(requestLogger)
        .addInterceptorFirst(responseLogger)
        .build()) {


```



```
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.DefaultProxyRoutePlanner;
import org.apache.http.HttpHost;

public RequestSpecification getZephyrClient(String jwt, String baseUrl) {
    HttpHost proxy = new HttpHost("proxy.corporate.com", 8080);
    DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);

    RestAssured.useRelaxedHTTPSValidation(); // Trust self-signed certs, useful in corp networks

    RestAssured.config = RestAssured.config().httpClient(
        new io.restassured.config.HttpClientConfig().httpClientFactory(() -> {
            return HttpClientBuilder.create()
                .setRoutePlanner(routePlanner)
                .build();
        })
    );

    return RestAssured.given()
        .baseUri(baseUrl)
        .header("Authorization", jwt)
        .header("Content-Type", "application/json");
}
```
## Zephyr code with proxy 

```
public static String sendRequest(
        String method,
        String url,
        String jwtToken,
        Map<String, String> headers,
        Map<String, Object> jsonBodyMap, // now a Map
        String proxyHost,
        int proxyPort,
        String proxyUser,
        String proxyPass
) throws IOException {
    ObjectMapper mapper = new ObjectMapper(); // Jackson mapper

    HttpHost proxy = new HttpHost(proxyHost, proxyPort);
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(
            new AuthScope(proxyHost, proxyPort),
            new UsernamePasswordCredentials(proxyUser, proxyPass)
    );

    CloseableHttpClient client = HttpClients.custom()
            .setDefaultCredentialsProvider(credsProvider)
            .setRoutePlanner(new DefaultProxyRoutePlanner(proxy))
            .build();

    HttpRequestBase request;

    switch (method.toUpperCase()) {
        case "POST" -> {
            HttpPost post = new HttpPost(url);
            if (jsonBodyMap != null) {
                String json = mapper.writeValueAsString(jsonBodyMap);
                post.setEntity(new StringEntity(json));
            }
            request = post;
        }
        case "PUT" -> {
            HttpPut put = new HttpPut(url);
            if (jsonBodyMap != null) {
                String json = mapper.writeValueAsString(jsonBodyMap);
                put.setEntity(new StringEntity(json));
            }
            request = put;
        }
        case "DELETE" -> request = new HttpDelete(url);
        case "GET" -> request = new HttpGet(url);
        default -> throw new IllegalArgumentException("Unsupported method: " + method);
    }

    if (jwtToken != null && !jwtToken.isEmpty()) {
        request.setHeader("Authorization", jwtToken);
    }
    if (headers != null) {
        headers.forEach(request::setHeader);
    }

    try (CloseableHttpResponse response = client.execute(request)) {
        return EntityUtils.toString(response.getEntity());
    }
}

```

```
{
  "manifest_version": 2,
  "name": "Auto Auth Cancel",
  "version": "1.0",
  "background": {
    "scripts": ["background.js"]
  },
  "permissions": [
    "webRequest",
    "webRequestBlocking",
    "<all_urls>"
  ]
}


chrome.webRequest.onAuthRequired.addListener(
  function(details, callbackFn) {
    callbackFn({cancel: true});  // CANCEL the popup
  },
  {urls: ["<all_urls>"]},
  ["blocking"]
);


options.addArguments("load-extension=/path/to/auto-auth-extension");


<suite name="ParallelAccountWorkflow" parallel="methods" thread-count="10">
    <test name="MultiUserAccountWorkflow">
        <classes>
            <class name="tests.TestUserWorkflow"/>
        </classes>
    </test>
</suite>



```
