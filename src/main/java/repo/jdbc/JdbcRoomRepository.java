package repo.jdbc;

import model.Room;
import repo.interfaces.RoomRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link RoomRepository} 的 JDBC 实现。
 * <p>
 * 给 RoomService 提供 rooms 表的读写能力，并让 DormService 能通过 RoomService
 * 获取房间归属。所属楼是否存在、楼层是否合法由业务层在调用本类前确认。
 */
public class JdbcRoomRepository implements RoomRepository {
    private final Connection connection;

    public JdbcRoomRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Room save(Room room) throws SQLException {
        // RoomService 已将房间与有效宿舍楼关联，本类只负责持久化字段映射。
        String sql = "INSERT INTO rooms (room_number, building_id, floor_number) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, room.roomNumber());
            statement.setLong(2, room.buildingId());
            statement.setInt(3, room.floorNumber());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                // 将自增房间 ID 返回给业务层，供 UI 或后续宿舍分配流程使用。
                return new Room(keys.getLong(1), room.roomNumber(), room.buildingId(), room.floorNumber());
            }
        }
    }

    @Override
    public List<Room> findAll() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                // 以楼、楼层、房间号排序，使 RoomService 的全量列表可直接展示。
                "SELECT room_id, room_number, building_id, floor_number FROM rooms ORDER BY building_id, floor_number, room_number");
             ResultSet resultSet = statement.executeQuery()) {
            List<Room> rooms = new ArrayList<>();
            while (resultSet.next()) {
                rooms.add(map(resultSet));
            }
            return rooms;
        }
    }

    @Override
    public List<Room> findByBuildingId(long buildingId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                // 给“按楼查房间”页面返回同一栋楼内自然的楼层/房间号顺序。
                "SELECT room_id, room_number, building_id, floor_number FROM rooms WHERE building_id = ? ORDER BY floor_number, room_number")) {
            statement.setLong(1, buildingId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Room> rooms = new ArrayList<>();
                while (resultSet.next()) {
                    rooms.add(map(resultSet));
                }
                return rooms;
            }
        }
    }

    @Override
    public Optional<Room> findById(long roomId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT room_id, room_number, building_id, floor_number FROM rooms WHERE room_id = ?")) {
            statement.setLong(1, roomId);
            try (ResultSet resultSet = statement.executeQuery()) {
                // 空结果交给 RoomService 或 DormService 转换为业务错误。
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<Room> findByBuildingFloorAndRoomNumber(long buildingId, int floorNumber, String roomNumber) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT room_id, room_number, building_id, floor_number FROM rooms WHERE building_id = ? AND floor_number = ? AND room_number = ?")) {
            statement.setLong(1, buildingId);
            statement.setInt(2, floorNumber);
            statement.setString(3, roomNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    private Room map(ResultSet resultSet) throws SQLException {
        // 集中完成 records 与表字段的对应，调用方始终只接触 Room 领域对象。
        return new Room(
                resultSet.getLong("room_id"),
                resultSet.getString("room_number"),
                resultSet.getLong("building_id"),
                resultSet.getInt("floor_number")
        );
    }
}

