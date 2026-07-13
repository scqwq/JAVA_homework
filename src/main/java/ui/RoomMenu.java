package ui;

import model.Room;
import service.BuildingService;
import service.RoomService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 宿舍房间管理菜单，负责与房间相关的终端交互。
 * <p>
 * 接收用户输入并调用 {@link RoomService} 完成业务操作，再将结果输出到控制台。
 */
public final class RoomMenu {

    // 私有构造器，防止实例化；所有方法均为静态工具方法。
    private RoomMenu() {
    }

    // 显示宿舍房间管理子菜单，并根据用户选择分发到对应操作。
    public static void start(RoomService roomService, BuildingService buildingService) throws SQLException {
        while (true) {
            ConsoleUtils.printHeader("宿舍房间管理");
            String[] headers = {"编号", "操作"};
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"[1]", "新增房间"});
            rows.add(new String[]{"[2]", "查看全部房间"});
            rows.add(new String[]{"[3]", "按宿舍楼查看房间"});
            rows.add(new String[]{"[0]", "返回上级菜单"});
            ConsoleUtils.printTable(headers, rows);
            ConsoleUtils.printSeparator();

            switch (ConsoleUtils.readInt("请选择操作")) {
                case 1 -> createRoom(roomService);
                case 2 -> listRooms(roomService);
                case 3 -> listRoomsByBuilding(roomService, buildingService);
                case 0 -> {
                    return;
                }
                default -> ConsoleUtils.printError("无效的选项，请重新输入。");
            }
        }
    }

    // 读取新增房间所需信息并调用服务；失败时输出错误提示。
    private static void createRoom(RoomService roomService) throws SQLException {
        ConsoleUtils.printSubHeader("新增房间");
        try {
            String roomNumber = ConsoleUtils.readLine("输入房间号");
            long buildingId = ConsoleUtils.readLong("输入所属宿舍楼ID");
            int floorNumber = ConsoleUtils.readInt("输入楼层");
            Room room = roomService.createRoom(roomNumber, buildingId, floorNumber);
            ConsoleUtils.printSuccess("新增成功，房间ID: " + room.roomId());
        } catch (IllegalArgumentException exception) {
            ConsoleUtils.printError("新增失败: " + exception.getMessage());
        }
        ConsoleUtils.waitForEnter();
    }

    // 列出全部房间并以表格形式输出。
    private static void listRooms(RoomService roomService) throws SQLException {
        ConsoleUtils.printSubHeader("全部房间");
        List<Room> rooms = roomService.listRooms();
        String[] headers = {"ID", "房间号", "宿舍楼ID", "楼层", "床位"};
        List<String[]> rows = new ArrayList<>();
        for (Room room : rooms) {
            rows.add(new String[]{
                    String.valueOf(room.roomId()),
                    room.roomNumber(),
                    String.valueOf(room.buildingId()),
                    String.valueOf(room.floorNumber()),
                    "1-4"
            });
        }
        ConsoleUtils.printTable(headers, rows);
        ConsoleUtils.waitForEnter();
    }

    // 按宿舍楼列出房间；查询前会先确认宿舍楼存在。
    private static void listRoomsByBuilding(RoomService roomService, BuildingService buildingService) throws SQLException {
        ConsoleUtils.printSubHeader("按宿舍楼查看房间");
        try {
            long buildingId = ConsoleUtils.readLong("输入宿舍楼ID");
            buildingService.getBuilding(buildingId);
            List<Room> rooms = roomService.listRoomsByBuilding(buildingId);
            String[] headers = {"ID", "房间号", "楼层"};
            List<String[]> rows = new ArrayList<>();
            for (Room room : rooms) {
                rows.add(new String[]{
                        String.valueOf(room.roomId()),
                        room.roomNumber(),
                        String.valueOf(room.floorNumber())
                });
            }
            ConsoleUtils.printTable(headers, rows);
        } catch (IllegalArgumentException exception) {
            ConsoleUtils.printError("查询失败: " + exception.getMessage());
        }
        ConsoleUtils.waitForEnter();
    }
}
