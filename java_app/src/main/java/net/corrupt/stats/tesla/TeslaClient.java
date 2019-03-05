package net.corrupt.stats.tesla;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TeslaClient {
    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final String BASE_URL = "https://owner-api.teslamotors.com";
    private final String CLIENT_ID = System.getenv("tesla_client_id");
    private final String CLIENT_SECRET = System.getenv("tesla_client_secret");

    private HttpClient client;
    private String vehicleId;
    private String energyId;
    private String username;
    private String password;

    private Integer maxRetries = 5;

    private String accessToken;

    public TeslaClient(final String vehicleId, final String energyId, final String username, final String password) {
        this.vehicleId = vehicleId;
        this.energyId = energyId;
        this.username = username;
        this.password = password;

        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public JSONObject getSolarData() throws TeslaClientException {
        String energyDataUrl = String.format("%s/api/1/energy_sites/%s/live_status", BASE_URL, this.energyId);

        return getApiData(energyDataUrl);
    }

    public JSONObject getVehicleData() throws TeslaClientException {
        String vehicleDataUrl = String.format("%s/api/1/vehicles/%s/data", BASE_URL, this.vehicleId);

        return getApiData(vehicleDataUrl);
    }

    private JSONObject getApiData(String url) throws TeslaClientException {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> stringHttpResponse = authenticatedCallWithRetries(0, request);

        LOGGER.log(Level.INFO, "stringHttpResponse = " + stringHttpResponse);

        JSONObject jsonResponse = new JSONObject(stringHttpResponse.body());

        // Add response info
        jsonResponse.put("statusCode", stringHttpResponse.statusCode());

        return jsonResponse;
    }

    private Integer getMaxRetries() { return maxRetries; }

    private HttpResponse<String> authenticatedCallWithRetries(Integer retry, HttpRequest.Builder requestBuilder) throws TeslaClientException {
        HttpResponse<String> response = null;

        try {
            if (accessToken == null) {
                requestAccessToken();
            }

            response = null;
            requestBuilder.header("Authorization", "Bearer " + getAccessToken());

            response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (retry < maxRetries) {
                LOGGER.log(Level.INFO, String.format("Retry: %s, Error: %s", retry, e.getMessage()));
                return authenticatedCallWithRetries(retry + 1, requestBuilder);
            } else {
                throw new TeslaClientException(e.toString());
            }
        }

        return response;
    }

    private void requestAccessToken() throws IOException, InterruptedException {
        LOGGER.log(Level.INFO, "!!! Getting new Access Token");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/oauth/token"))
                .timeout(Duration.ofMinutes(2))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(authenticationBody().toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JSONObject jsonResponse = new JSONObject(response.body());

        setAccessToken(jsonResponse.getString("access_token"));
    }

    private JSONObject authenticationBody() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("grant_type", "password");
        jsonObject.put("client_id", CLIENT_ID);
        jsonObject.put("client_secret", CLIENT_SECRET);
        jsonObject.put("email", getUsername());
        jsonObject.put("password", getPassword());

        return jsonObject;
    }

    private String getUsername() {
        return username;
    }

    private String getPassword() {
        return password;
    }

    private String getAccessToken() {
        return accessToken;
    }

    private void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
