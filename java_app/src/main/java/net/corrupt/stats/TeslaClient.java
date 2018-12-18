package net.corrupt.stats;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TeslaClient {
    private final String BASE_URL = "https://owner-api.teslamotors.com";
    private final String CLIENT_ID = System.getenv("tesla_client_id");
    private final String CLIENT_SECRET = System.getenv("tesla_client_secret");

    private HttpClient client;
    private String vehicleId;
    private String username;
    private String password;

    private String accessToken;

    public TeslaClient(final String vehicleId, final String username, final String password) {
        this.vehicleId = vehicleId;
        this.username = username;
        this.password = password;

        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public JSONObject getVehicleData() {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/api/1/vehicles/%s/data", BASE_URL, this.vehicleId)))
                .timeout(Duration.ofMinutes(2))
                .header("Accept", "application/json")
                .GET();

        HttpResponse<String> stringHttpResponse = authenticatedCallWithRetries(0, request);

        System.out.println("stringHttpResponse = " + stringHttpResponse);

        JSONObject jsonResponse = new JSONObject(stringHttpResponse.body());

        return jsonResponse;
    }

    private HttpResponse<String> authenticatedCallWithRetries(Integer retry, HttpRequest.Builder requestBuilder) {
        if (accessToken == null) {
            try {
                requestAccessToken();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        HttpResponse<String> response = null;
        requestBuilder.header("Authorization", "Bearer " + getAccessToken());

        try {
            response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return response;
    }

    private void requestAccessToken() throws IOException, InterruptedException {
        System.out.println("!!! Getting new Access Token");

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
