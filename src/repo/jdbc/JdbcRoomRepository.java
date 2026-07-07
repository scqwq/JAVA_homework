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

public class JdbcRoomRepository implements RoomRepository {
    private final Connection connection;

    public JdbcRoomRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Room save(Room room) throws SQLException {
        String sql = "INSERT INTO rooms (room_number, building_id, floor_number) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, room.roomNumber());
            statement.setLong(2, room.buildingId());
            statement.setInt(3, room.floorNumber());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return new Room(keys.getLong(1), room.roomNumber(), room.buildingId(), room.floorNumber());
            }
        }
    }

    @Override
    public List<Room> findAll() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
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
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    private Room map(ResultSet resultSet) throws SQLException {
        return new Room(
                resultSet.getLong("room_id"),
                resultSet.getString("room_number"),
                resultSet.getLong("building_id"),
                resultSet.getInt("floor_number")
        );
    }
}

