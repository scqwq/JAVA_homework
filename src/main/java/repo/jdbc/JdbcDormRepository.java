package repo.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import model.DormAssignment;
import repo.interfaces.DormRepository;

//管理宿舍分配，包括新生入住、学生调换宿舍
public class JdbcDormRepository implements DormRepository {
    private final Connection connection;

    public JdbcDormRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public DormAssignment save(DormAssignment dormAssignment) throws SQLException {
        String sql = "INSERT INTO dorm_assignments (student_id, building_id, room_id, bed_number) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, dormAssignment.studentId());
            statement.setLong(2, dormAssignment.buildingId());
            statement.setLong(3, dormAssignment.roomId());
            statement.setInt(4, dormAssignment.bedNumber());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return new DormAssignment(
                        keys.getLong(1),
                        dormAssignment.studentId(),
                        dormAssignment.buildingId(),
                        dormAssignment.roomId(),
                        dormAssignment.bedNumber()
                );
            }
        }
    }

    @Override
    public DormAssignment update(DormAssignment dormAssignment) throws SQLException {
        String sql = "UPDATE dorm_assignments SET building_id = ?, room_id = ?, bed_number = ? WHERE student_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, dormAssignment.buildingId());
            statement.setLong(2, dormAssignment.roomId());
            statement.setInt(3, dormAssignment.bedNumber());
            statement.setString(4, dormAssignment.studentId());
            statement.executeUpdate();
        }
        return dormAssignment;
    }

    @Override
    public Optional<DormAssignment> findByStudentId(String studentId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT assignment_id, student_id, building_id, room_id, bed_number FROM dorm_assignments WHERE student_id = ?")) {
            statement.setString(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public List<DormAssignment> findByRoomId(long roomId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT assignment_id, student_id, building_id, room_id, bed_number FROM dorm_assignments WHERE room_id = ? ORDER BY bed_number")) {
            statement.setLong(1, roomId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<DormAssignment> dormAssignments = new ArrayList<>();
                while (resultSet.next()) {
                    dormAssignments.add(map(resultSet));
                }
                return dormAssignments;
            }
        }
    }

    @Override
    public boolean existsByRoomIdAndBedNumber(long roomId, int bedNumber, String excludeStudentId) throws SQLException {
        String sql = """
                SELECT 1
                FROM dorm_assignments
                WHERE room_id = ? AND bed_number = ? AND (? IS NULL OR student_id <> ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId);
            statement.setInt(2, bedNumber);
            statement.setString(3, excludeStudentId);
            statement.setString(4, excludeStudentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private DormAssignment map(ResultSet resultSet) throws SQLException {
        return new DormAssignment(
                resultSet.getLong("assignment_id"),
                resultSet.getString("student_id"),
                resultSet.getLong("building_id"),
                resultSet.getLong("room_id"),
                resultSet.getInt("bed_number")
        );
    }
}

