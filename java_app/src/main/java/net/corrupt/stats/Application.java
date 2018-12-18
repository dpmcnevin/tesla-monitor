package net.corrupt.stats;

import org.json.JSONObject;

import java.io.IOException;

public class Application {
    public static void main(String[] args) throws IOException, InterruptedException {
        String vehicleId = System.getenv("vehicle_id");
        String username = System.getenv("tesla_username");
        String password = System.getenv("tesla_password");

        TeslaClient teslaClient = new TeslaClient(vehicleId, username, password);

        while(true) {
            JSONObject vehicleData = teslaClient.getVehicleData();
            GraphiteClient.sendStats(vehicleData);
            Thread.sleep(30 * 1000);
        }
    }
}
