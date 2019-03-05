package net.corrupt.stats.graphite;

import org.json.JSONObject;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GraphiteClient implements AutoCloseable {
    private static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final JSONObject vehicleData;

    private DatagramSocket socket;
    private InetAddress address;

    public GraphiteClient(String hostname, JSONObject vehicleData) throws SocketException, UnknownHostException {
        this.vehicleData = vehicleData;

        this.socket = new DatagramSocket();
        this.address = InetAddress.getByName(hostname);
    }

    public static void sendStats(String hostname, JSONObject data) throws GraphiteClientException {
        try (GraphiteClient client = new GraphiteClient(hostname, data)) {
            client.flattenVehicleData().forEach((s, o) -> {
                String statsdData = String.format("tesla.%s:%s|%s", s, o, "g");
                System.out.println("statsdData = " + statsdData);
                try {
                    client.sendStat(statsdData);
                    LOGGER.log(Level.FINER, statsdData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (SocketException | UnknownHostException e) {
            throw new GraphiteClientException(e.toString());
        }
    }

    public void sendStat(String msg) throws IOException {
        byte[] buf;

        buf = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 8125);
        socket.send(packet);
    }

    public Map<String, Object> flattenVehicleData() {
        return flattenVehicleData("", vehicleData.toMap());
    }

    private Map<String, Object> flattenVehicleData(String prefix, Map<String, Object> data) {
        Map<String, Object> map = new HashMap<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() != null && entry.getValue() instanceof HashMap) {
                map.putAll(flattenVehicleData(getKeyWithPrefix(prefix, entry.getKey()), (HashMap) entry.getValue()));
            } else {
                map.put(getKeyWithPrefix(prefix, entry.getKey()), entry.getValue());
            }
        }

        return map;
    }

    private String getKeyWithPrefix(String prefix, String e) {
        return prefix.equals("") ? e : String.join(".", prefix, e);
    }

    public void close() {
        socket.close();
    }
}
