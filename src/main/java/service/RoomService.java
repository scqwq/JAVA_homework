package service;

import model.Room;
import database.DatabaseConnection;

import java.sql.SQLException;
import java.util.List;

public class RoomService {
    private final DatabaseConnection databaseConnection;
    private final BuildingService buildingService;

    public RoomService(DatabaseConnection databaseConnection, BuildingService buildingService) {
        this.databaseConnection = databaseConnection;
        this.buildingService = buildingService;
    }

    public Room createRoom(String roomNumber, long buildingId, int floorNumber) throws SQLException {
        buildingService.getBuilding(buildingId);
        if (floorNumber < 1) {
            throw new IllegalArgumentException("楼层必须大于等于 1");
        }
        return databaseConnection.roomRepository().save(new Room(0L, roomNumber, buildingId, floorNumber));
    }

    public List<Room> listRooms() throws SQLException {
        return databaseConnection.roomRepository().findAll();
    }

    public List<Room> listRoomsByBuilding(long buildingId) throws SQLException {
        buildingService.getBuilding(buildingId);
        return databaseConnection.roomRepository().findByBuildingId(buildingId);
    }

    public Room getRoom(long roomId) throws SQLException {
        return databaseConnection.roomRepository()
                .findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("未找到宿舍房间: " + roomId));
    }
}

