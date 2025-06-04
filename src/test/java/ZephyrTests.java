import org.testng.annotations.Test;
import zephyr.ZephyrAPI;

public class ZephyrTests {
    ZephyrAPI api = new ZephyrAPI();

    @Test
    public void testCreateCycle() {
        String body = "{\"name\":\"Cycle 1\", \"projectId\":10000, \"versionId\":-1}";
        api.createCycle(body).then().statusCode(200);
    }

    @Test
    public void testAddTestToCycle() {
        String body = "{\"issueId\":\"10001\", \"cycleId\":\"10002\", \"projectId\":\"10000\"}";
        api.addTestToCycle(body).then().statusCode(200);
    }
}