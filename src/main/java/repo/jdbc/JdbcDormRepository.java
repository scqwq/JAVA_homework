package repo.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import model.DormAssignment;
import repo.interfaces.DormRepository;

/**
 * {@link DormRepository} 的 JDBC 实现。
 * <p>
 * 给 DormService 持久化入住和调宿结果，并提供床位占用查询。学生性别、楼房归属、
 * 床号范围等业务规则由 DormService 先验证，本类只执行分配表读写。
 */
public class JdbcDormRepository implements DormRepository {
    private final Connection connection;

    public JdbcDormRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public DormAssignment save(DormAssignment dormAssignment) throws SQLException {
        // DormService 已确认学生、楼、房间和床位关系有效；这里写入最终分配记录。
        String sql = "INSERT INTO dorm_assignments (student_id, building_id, room_id, bed_number) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, dormAssignment.studentId());
            statement.setLong(2, dormAssignment.buildingId());
            statement.setLong(3, dormAssignment.roomId());
            statement.setInt(4, dormAssignment.bedNumber());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                // 返回数据库生成的 assignmentId，保持业务层拿到的是完整领域对象。
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
        // 调宿按学生学号更新既有记录；是否存在旧记录由 DormService 在调用前确认。
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
                // 空结果是“尚未分配”的正常状态，DormService 据此选择分配或调宿流程。
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public List<DormAssignment> findByRoomId(long roomId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                // 床号排序使调用方能按实际床位顺序理解该房间的占用记录。
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
        // 调宿时排除当前学生自己的记录，避免同一床位被错误判定为冲突。
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

    @Override
    public long countAllAssignments() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM dorm_assignments")) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    @Override
    public Map<Long, Long> countAssignmentsByBuilding() throws SQLException {
        Map<Long, Long> counts = new HashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT building_id, COUNT(*) FROM dorm_assignments GROUP BY building_id")) {
            while (resultSet.next()) {
                counts.put(resultSet.getLong(1), resultSet.getLong(2));
            }
        }
        return counts;
    }

    private DormAssignment map(ResultSet resultSet) throws SQLException {
        // 数据库字段到领域分配对象的转换集中在 Repository，DormService 不依赖列名。
        return new DormAssignment(
                resultSet.getLong("assignment_id"),
                resultSet.getString("student_id"),
                resultSet.getLong("building_id"),
                resultSet.getLong("room_id"),
                resultSet.getInt("bed_number")
        );
    }
}

