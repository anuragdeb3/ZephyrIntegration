import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

public class ProxyConfig {

    public static void setupProxy(String host, int port, String username, String password) {
        BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
            new AuthScope(host, port),
            new UsernamePasswordCredentials(username, password)
        );

        HttpHost proxy = new HttpHost(host, port);

        RestAssured.config = RestAssuredConfig.config().httpClient(
            HttpClientConfig.httpClientConfig().httpClientFactory(() ->
                HttpClientBuilder.create()
                    .setProxy(proxy)
                    .setDefaultCredentialsProvider(credsProvider)
                    .build()
            )
        );
    }
}
