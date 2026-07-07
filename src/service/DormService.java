package service;

import model.Building;
import model.DormAssignment;
import model.Room;
import model.Student;
import model.StudentDormView;
import database.DatabaseConnection;

import java.sql.SQLException;
import java.util.List;

public class DormService {
    private final DatabaseConnection databaseConnection;
    private final StudentService studentService;
    private final BuildingService buildingService;
    private final RoomService roomService;

    public DormService(
            DatabaseConnection databaseConnection,
            StudentService studentService,
            BuildingService buildingService,
            RoomService roomService
    ) {
        this.databaseConnection = databaseConnection;
        this.studentService = studentService;
        this.buildingService = buildingService;
        this.roomService = roomService;
    }

    public DormAssignment assignDorm(String studentId, long buildingId, long roomId, int bedNumber) throws SQLException {
        Student student = studentService.getStudent(studentId);
        Building building = buildingService.getBuilding(buildingId);
        Room room = roomService.getRoom(roomId);
        validateAssignment(student, building, room, bedNumber);

        if (databaseConnection.dormRepository().findByStudentId(studentId).isPresent()) {
            throw new IllegalArgumentException("该学生已分配宿舍，请使用调宿功能");
        }
        if (databaseConnection.dormRepository().existsByRoomIdAndBedNumber(roomId, bedNumber, null)) {
            throw new IllegalArgumentException("该床位已被占用");
        }

        return databaseConnection.dormRepository().save(new DormAssignment(0L, studentId, buildingId, roomId, bedNumber));
    }

    public DormAssignment changeDorm(String studentId, long buildingId, long roomId, int bedNumber) throws SQLException {
        Student student = studentService.getStudent(studentId);
        Building building = buildingService.getBuilding(buildingId);
        Room room = roomService.getRoom(roomId);
        validateAssignment(student, building, room, bedNumber);

        DormAssignment current = databaseConnection.dormRepository()
                .findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("该学生尚未分配宿舍"));
        if (databaseConnection.dormRepository().existsByRoomIdAndBedNumber(roomId, bedNumber, studentId)) {
            throw new IllegalArgumentException("目标床位已被占用");
        }

        return databaseConnection.dormRepository().update(
                new DormAssignment(current.assignmentId(), studentId, buildingId, roomId, bedNumber)
        );
    }

    public List<StudentDormView> findStudentsByRoom(long roomId) throws SQLException {
        roomService.getRoom(roomId);
        return studentService.getStudentsByRoomId(roomId);
    }

    public StudentDormView findDormByStudent(String studentId) throws SQLException {
        return studentService.getDormByStudentId(studentId);
    }

    private void validateAssignment(Student student, Building building, Room room, int bedNumber) {
        if (room.buildingId() != building.buildingId()) {
            throw new IllegalArgumentException("房间不属于该宿舍楼");
        }
        if (bedNumber < 1 || bedNumber > 4) {
            throw new IllegalArgumentException("床号只能是 1 到 4");
        }
        if (!building.genderPolicy().supports(student.gender())) {
            throw new IllegalArgumentException("该宿舍楼不允许当前学生性别入住");
        }
    }
}

