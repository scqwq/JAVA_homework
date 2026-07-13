import com.sun.net.httpserver.HttpServer;
import handler.BuildingCreateHandler;
import handler.DashboardHandler;
import handler.DormAssignHandler;
import handler.DormChangeHandler;
import handler.RoomCreateHandler;
import handler.StudentCreateHandler;
import service.BuildingService;
import service.DormService;
import service.RoomService;
import service.StudentService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public final class Router {
    private Router() {
    }

    public static HttpServer start(
            BuildingService buildingService,
            RoomService roomService,
            StudentService studentService,
            DormService dormService
    ) throws IOException {
        int port = resolvePort();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new DashboardHandler(buildingService, roomService, studentService, dormService));
        server.createContext("/students/create", new StudentCreateHandler(studentService));
        server.createContext("/buildings/create", new BuildingCreateHandler(buildingService));
        server.createContext("/rooms/create", new RoomCreateHandler(roomService));
        server.createContext("/dorms/assign", new DormAssignHandler(dormService));
        server.createContext("/dorms/change", new DormChangeHandler(dormService));

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        return server;
    }

    private static int resolvePort() {
        String rawPort = readAppPort();
        if (rawPort == null || rawPort.isBlank()) {
            return 8080;
        }
        try {
            return Integer.parseInt(rawPort.trim());
        } catch (NumberFormatException ignored) {
            return 8080;
        }
    }

    private static String readAppPort() {
        Map<String, String> values = new HashMap<>(System.getenv());
        Path envPath = Path.of(".env");
        if (Files.exists(envPath)) {
            try {
                for (String line : Files.readAllLines(envPath)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                        continue;
                    }
                    int separator = trimmed.indexOf('=');
                    String key = trimmed.substring(0, separator).trim();
                    String value = trimmed.substring(separator + 1).trim();
                    values.put(key, stripQuotes(value));
                }
            } catch (IOException ignored) {
            }
        }
        return values.get("APP_PORT");
    }

    private static String stripQuotes(String value) {
        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
