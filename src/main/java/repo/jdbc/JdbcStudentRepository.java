package repo.jdbc;

import model.Student;
import model.StudentDormView;
import model.Gender;
import repo.interfaces.StudentRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link StudentRepository} 的 JDBC 实现。
 * <p>
 * 给 StudentService 提供 students 表访问，并为 DormService 的查询页面组装学生与宿舍的视图。
 * Service 层只处理 Student 或 StudentDormView，不需要知道关联表和枚举的存储格式。
 */
public class JdbcStudentRepository implements StudentRepository {
    private final Connection connection;

    public JdbcStudentRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Student save(Student student) throws SQLException {
        // StudentService 已处理学号非空和性别输入，本类只把学生资料映射到 students 表。
        String sql = "INSERT INTO students (student_id, student_name, class_name, grade, gender) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, student.studentId());
            statement.setString(2, student.studentName());
            statement.setString(3, student.className());
            statement.setString(4, student.grade());
            statement.setString(5, student.gender().name());
            statement.executeUpdate();
            return student;
        }
    }

    @Override
    public List<Student> findAll() throws SQLException {
        List<Student> students = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                // 列表按学号排序，便于 StudentService 在 UI 中稳定展示学生资料。
                "SELECT student_id, student_name, class_name, grade, gender FROM students ORDER BY student_id");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                students.add(mapStudent(resultSet));
            }
        }
        return students;
    }

    @Override
    public Optional<Student> findById(String studentId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT student_id, student_name, class_name, grade, gender FROM students WHERE student_id = ?")) {
            statement.setString(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                // Optional.empty() 表示学号不存在，具体业务提示由调用 Service 统一生成。
                return resultSet.next() ? Optional.of(mapStudent(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public List<StudentDormView> findStudentsByRoom(long roomId) throws SQLException {
        // DormService 需要一次取到学生、楼、房间和床号，避免业务层自行拼接多次查询；结果按床号返回给 UI。
        String sql = """
                SELECT s.student_id, s.student_name, s.class_name, s.grade, s.gender,
                       b.building_code, b.building_name, r.room_number, d.bed_number
                FROM dorm_assignments d
                JOIN students s ON s.student_id = d.student_id
                JOIN buildings b ON b.building_id = d.building_id
                JOIN rooms r ON r.room_id = d.room_id
                WHERE d.room_id = ?
                ORDER BY d.bed_number
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<StudentDormView> students = new ArrayList<>();
                while (resultSet.next()) {
                    students.add(mapView(resultSet));
                }
                return students;
            }
        }
    }

    @Override
    public Optional<StudentDormView> findDormByStudentId(String studentId) throws SQLException {
        // LEFT JOIN 保留未入住学生，让 StudentService 能区分“学生存在但暂未分配”和“学号不存在”。
        String sql = """
                SELECT s.student_id, s.student_name, s.class_name, s.grade, s.gender,
                       b.building_code, b.building_name, r.room_number, d.bed_number
                FROM students s
                LEFT JOIN dorm_assignments d ON d.student_id = s.student_id
                LEFT JOIN buildings b ON b.building_id = d.building_id
                LEFT JOIN rooms r ON r.room_id = d.room_id
                WHERE s.student_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapView(resultSet)) : Optional.empty();
            }
        }
    }

    private Student mapStudent(ResultSet resultSet) throws SQLException {
        // 统一把数据库保存的枚举常量还原为领域枚举，避免类型转换散落到 Service 层。
        return new Student(
                resultSet.getString("student_id"),
                resultSet.getString("student_name"),
                resultSet.getString("class_name"),
                resultSet.getString("grade"),
                Gender.valueOf(resultSet.getString("gender"))
        );
    }

    private StudentDormView mapView(ResultSet resultSet) throws SQLException {
        // getInt 会把 SQL NULL 读成 0，因此需结合 wasNull() 保留“暂未入住”的业务语义。
        int bedNumber = resultSet.getInt("bed_number");
        return new StudentDormView(
                resultSet.getString("student_id"),
                resultSet.getString("student_name"),
                resultSet.getString("class_name"),
                resultSet.getString("grade"),
                Gender.valueOf(resultSet.getString("gender")),
                resultSet.getString("building_code"),
                resultSet.getString("building_name"),
                resultSet.getString("room_number"),
                resultSet.wasNull() ? null : bedNumber
        );
    }
}

