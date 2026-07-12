package ui;

import model.Room;
import service.BuildingService;
import service.RoomService;

import java.sql.SQLException;

public final class RoomMenu {
    private RoomMenu() {
    }

    public static void start(RoomService roomService, BuildingService buildingService) throws SQLException {
        while (true) {
            ConsoleUtils.printHeader("宿舍房间管理");
            System.out.println("1. 新增房间");
            System.out.println("2. 查看全部房间");
            System.out.println("3. 按宿舍楼查看房间");
            System.out.println("0. 返回上级菜单");

            switch (ConsoleUtils.readInt("请选择操作")) {
                case 1 -> createRoom(roomService);
                case 2 -> listRooms(roomService);
                case 3 -> listRoomsByBuilding(roomService, buildingService);
                case 0 -> {
                    return;
                }
                default -> System.out.println("无效的选项，请重新输入。");
            }
        }
    }

    private static void createRoom(RoomService roomService) throws SQLException {
        try {
            String roomNumber = ConsoleUtils.readLine("输入房间号");
            long buildingId = ConsoleUtils.readLong("输入所属宿舍楼ID");
            int floorNumber = ConsoleUtils.readInt("输入楼层");
            Room room = roomService.createRoom(roomNumber, buildingId, floorNumber);
            System.out.println("新增成功，房间ID: " + room.roomId());
        } catch (IllegalArgumentException exception) {
            System.out.println("新增失败: " + exception.getMessage());
        }
        ConsoleUtils.waitForEnter();
    }

    private static void listRooms(RoomService roomService) throws SQLException {
        for (Room room : roomService.listRooms()) {
            System.out.printf("ID=%d, 房间号=%s, 宿舍楼ID=%d, 楼层=%d, 床位=1-4%n",
                    room.roomId(), room.roomNumber(), room.buildingId(), room.floorNumber());
        }
        ConsoleUtils.waitForEnter();
    }

    private static void listRoomsByBuilding(RoomService roomService, BuildingService buildingService) throws SQLException {
        try {
            long buildingId = ConsoleUtils.readLong("输入宿舍楼ID");
            buildingService.getBuilding(buildingId);
            for (Room room : roomService.listRoomsByBuilding(buildingId)) {
                System.out.printf("ID=%d, 房间号=%s, 楼层=%d%n", room.roomId(), room.roomNumber(), room.floorNumber());
            }
        } catch (IllegalArgumentException exception) {
            System.out.println("查询失败: " + exception.getMessage());
        }
        ConsoleUtils.waitForEnter();
    }
}

