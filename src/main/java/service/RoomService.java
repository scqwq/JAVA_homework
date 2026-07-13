package service;

import model.Room;
import database.DatabaseConnection;

import java.sql.SQLException;
import java.util.List;

/**
 * 宿舍房间服务类，负责处理宿舍房间相关的业务逻辑。
 * <p>
 * 与 {@link BuildingService} 协作确认宿舍楼存在，并负责楼层、房间号重复等业务校验。
 */
public class RoomService {
    // 数据库连接入口，用于访问房间仓储。
    private final DatabaseConnection databaseConnection;
    // 宿舍楼服务，用于在创建房间前确认所属楼存在。
    private final BuildingService buildingService;

    // 通过构造器注入数据库连接与宿舍楼服务。
    public RoomService(DatabaseConnection databaseConnection, BuildingService buildingService) {
        this.databaseConnection = databaseConnection;
        this.buildingService = buildingService;
    }

    // 新增房间：确认宿舍楼存在、楼层合法且同一楼内房间号不重复后保存。
    public Room createRoom(String roomNumber, long buildingId, int floorNumber) throws SQLException {
        buildingService.getBuilding(buildingId);
        if (floorNumber < 1) {
            throw new IllegalArgumentException("楼层必须大于等于 1");
        }
        boolean roomExists = databaseConnection.roomRepository().findByBuildingId(buildingId).stream()
                .anyMatch(room -> room.roomNumber().equals(roomNumber));
        if (roomExists) {
            throw new IllegalArgumentException("该宿舍楼内房间号已存在: " + roomNumber);
        }
        return databaseConnection.roomRepository().save(new Room(0L, roomNumber, buildingId, floorNumber));
    }

    // 查询全部房间列表，供界面层展示。
    public List<Room> listRooms() throws SQLException {
        return databaseConnection.roomRepository().findAll();
    }

    // 根据宿舍楼 ID 查询该楼所有房间，供界面层展示；会先确认宿舍楼存在。
    public List<Room> listRoomsByBuilding(long buildingId) throws SQLException {
        buildingService.getBuilding(buildingId);
        return databaseConnection.roomRepository().findByBuildingId(buildingId);
    }

    // 根据房间 ID 查询单个房间；若不存在，则抛出业务异常。
    public Room getRoom(long roomId) throws SQLException {
        return databaseConnection.roomRepository()
                .findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("未找到宿舍房间: " + roomId));
    }
}
