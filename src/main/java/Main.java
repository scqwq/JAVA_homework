import database.DatabaseConnection;
import service.BuildingService;
import service.DormService;
import service.RoomService;
import service.StudentService;
import ui.BuildingMenu;
import ui.ConsoleUtils;
import ui.DormMenu;
import ui.RoomMenu;
import ui.StudentMenu;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        try (DatabaseConnection databaseConnection = DatabaseConnection.fromEnv()) {
            databaseConnection.initializeSchema();

            BuildingService buildingService = new BuildingService(databaseConnection);
            RoomService roomService = new RoomService(databaseConnection, buildingService);
            StudentService studentService = new StudentService(databaseConnection);
            DormService dormService = new DormService(databaseConnection, studentService, buildingService, roomService);

            runConsole(buildingService, roomService, studentService, dormService);
        } catch (Exception exception) {
            System.out.println("系统启动失败: " + exception.getMessage());
        }
    }

    private static void runConsole(
            BuildingService buildingService,
            RoomService roomService,
            StudentService studentService,
            DormService dormService
    ) throws SQLException {
        while (true) {
            ConsoleUtils.printHeader("学生宿舍管理系统");
            System.out.println("1. 学生管理");
            System.out.println("2. 宿舍楼管理");
            System.out.println("3. 宿舍房间管理");
            System.out.println("4. 调换宿舍管理");
            System.out.println("0. 退出");

            int choice = ConsoleUtils.readInt("请选择功能");
            switch (choice) {
                case 1 -> StudentMenu.start(studentService, dormService);
                case 2 -> BuildingMenu.start(buildingService);
                case 3 -> RoomMenu.start(roomService, buildingService);
                case 4 -> DormMenu.start(dormService, roomService);
                case 0 -> {
                    System.out.println("已退出系统。");
                    return;
                }
                default -> System.out.println("无效的选项，请重新输入。");
            }
        }
    }
}

