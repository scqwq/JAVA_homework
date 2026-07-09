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

//管理学生信息 
//赋值跨表查询，把学生和宿舍楼、房间、床位信息拼在一起查出来
public class JdbcStudentRepository implements StudentRepository {
    private final Connection connection;

    public JdbcStudentRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Student save(Student student) throws SQLException {
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
                return resultSet.next() ? Optional.of(mapStudent(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public List<StudentDormView> findStudentsByRoom(long roomId) throws SQLException {
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
        return new Student(
                resultSet.getString("student_id"),
                resultSet.getString("student_name"),
                resultSet.getString("class_name"),
                resultSet.getString("grade"),
                Gender.valueOf(resultSet.getString("gender"))
        );
    }

    private StudentDormView mapView(ResultSet resultSet) throws SQLException {
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

