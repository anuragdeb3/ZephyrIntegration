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
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.*;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;

import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class ZephyrClient {

    private static final String PROXY_HOST = "proxy.corporate.com";
    private static final int PROXY_PORT = 8080;
    private static final String PROXY_USER = "your_proxy_username";
    private static final String PROXY_PASS = "your_proxy_password";

    private static final String BASE_URL = "https://prod-play.zephyr4jiracloud.com";
    private static final String ACCESS_KEY = "your-zephyr-access-key";
    private static final String SECRET_KEY = "your-zephyr-secret-key";
    private static final String ACCOUNT_ID = "your-jira-account-id";

    public static void main(String[] args) throws Exception {
        String method = "GET";
        String endpoint = "/public/rest/api/1.0/cycles/search?projectId=1234&versionId=-1";
        String jwt = generateJwtToken(method, endpoint, ACCESS_KEY, SECRET_KEY, ACCOUNT_ID);

        RequestSpecification request = buildRestAssuredClient(jwt);
        String response = request.get(endpoint)
                .then()
                .statusCode(200)
                .extract()
                .asString();

        System.out.println("Zephyr API response: " + response);
    }

    private static RequestSpecification buildRestAssuredClient(String jwt) {
        HttpHost proxy = new HttpHost(PROXY_HOST, PROXY_PORT);

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(PROXY_HOST, PROXY_PORT),
                new UsernamePasswordCredentials(PROXY_USER, PROXY_PASS)
        );

        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setRoutePlanner(routePlanner)
                .setDefaultCredentialsProvider(credsProvider)
                .build();

        RestAssured.useRelaxedHTTPSValidation(); // Trust proxy cert (assumes already in JVM)

        RestAssured.config = RestAssured.config().httpClient(
                io.restassured.config.HttpClientConfig.httpClientConfig()
                        .httpClientFactory(() -> httpClient)
        );

        return RestAssured.given()
                .baseUri(BASE_URL)
                .header("Authorization", jwt)
                .header("Content-Type", "application/json");
    }

    private static String generateJwtToken(String method, String uri, String accessKey, String secretKey, String accountId) throws Exception {
        long expirationTime = Instant.now().getEpochSecond() + 3600;
        String qsh = getQsh(method, uri);

        String jwtHeader = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());
        String jwtPayload = String.format("{\"sub\":\"%s\",\"qsh\":\"%s\",\"iss\":\"%s\",\"exp\":%d}",
                accountId, qsh, accessKey, expirationTime);
        String jwtPayloadEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(jwtPayload.getBytes());

        String signingInput = jwtHeader + "." + jwtPayloadEncoded;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getBytes(), "HmacSHA256"));
        byte[] signature = mac.doFinal(signingInput.getBytes());

        String jwtSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        return "JWT " + signingInput + "." + jwtSignature;
    }

    private static String getQsh(String method, String uriWithQuery) throws Exception {
        // Canonical format: method & relative URI (without domain) & sorted query string
        String canonicalRequest = method.toUpperCase() + "&" +
                uriWithQuery.split("\\?")[0] + "&" +
                (uriWithQuery.contains("?") ? uriWithQuery.split("\\?")[1] : "");

        // SHA256 hash of the canonical request
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("dummy".getBytes(), "HmacSHA256"));
        byte[] hash = java.security.MessageDigest.getInstance("SHA-256").digest(canonicalRequest.getBytes());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}
```
