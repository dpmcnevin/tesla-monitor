package net.corrupt.stats;

import net.corrupt.stats.graphite.GraphiteClient;
import net.corrupt.stats.graphite.GraphiteClientException;
import net.corrupt.stats.tesla.TeslaClient;
import net.corrupt.stats.tesla.TeslaClientException;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Application {
    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static void main(String[] args) throws InterruptedException {
        String vehicleId = System.getenv("vehicle_id");
        String username = System.getenv("tesla_username");
        String password = System.getenv("tesla_password");
        String graphiteHostname = "graphite";

        TeslaClient teslaClient = new TeslaClient(vehicleId, username, password);

        while(true) {
            JSONObject vehicleData = null;

            // Get data from the API
            try {
                vehicleData = teslaClient.getVehicleData();
            } catch (TeslaClientException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
            }

            // Send the data to statsd
            try {
                if (vehicleData != null) GraphiteClient.sendStats(graphiteHostname, vehicleData);
            } catch (GraphiteClientException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
            }

            Thread.sleep(30 * 1000);
        }
    }
}
