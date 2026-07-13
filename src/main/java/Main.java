import com.sun.net.httpserver.HttpServer;
import database.DatabaseConnection;
import service.BuildingService;
import service.DormService;
import service.RoomService;
import service.StudentService;

public class Main {
    public static void main(String[] args) {
        try {
            DatabaseConnection databaseConnection = DatabaseConnection.fromEnv();
            databaseConnection.initializeSchema();

            BuildingService buildingService = new BuildingService(databaseConnection);
            RoomService roomService = new RoomService(databaseConnection, buildingService);
            StudentService studentService = new StudentService(databaseConnection);
            DormService dormService = new DormService(databaseConnection, studentService, buildingService, roomService);

            HttpServer server = Router.start(buildingService, roomService, studentService, dormService);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                server.stop(0);
                try {
                    databaseConnection.close();
                } catch (Exception ignored) {
                }
            }));

            System.out.println("Web 服务已启动: http://localhost:" + server.getAddress().getPort());
            Thread.currentThread().join();
        } catch (Exception exception) {
            System.out.println("系统启动失败: " + exception.getMessage());
        }
    }
}

