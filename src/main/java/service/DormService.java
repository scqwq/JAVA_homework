package service;

import model.Building;
import model.DormAssignment;
import model.Room;
import model.Student;
import model.StudentDormView;
import database.DatabaseConnection;

import java.sql.SQLException;
import java.util.List;

/**
 * 宿舍分配服务类，负责处理学生入住、调宿及宿舍查询相关业务逻辑。
 * <p>
 * 通过组合 {@link StudentService}、{@link BuildingService}、{@link RoomService} 完成跨实体校验，
 * 再调用 {@link database.DatabaseConnection#dormRepository()} 持久化分配结果。
 */
public class DormService {
    // 数据库连接入口，用于访问 dorm_assignments 等仓储。
    private final DatabaseConnection databaseConnection;
    // 学生服务，用于校验学生存在及性别信息。
    private final StudentService studentService;
    // 宿舍楼服务，用于校验目标宿舍楼存在及性别策略。
    private final BuildingService buildingService;
    // 房间服务，用于校验目标房间存在及归属关系。
    private final RoomService roomService;

    // 通过构造器注入所需服务，便于上层统一组装依赖。
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

    // 给学生分配宿舍：校验学生、楼、房间及床位关系后保存新分配记录。
    public DormAssignment assignDorm(String studentId, long buildingId, long roomId, int bedNumber) throws SQLException {
        Student student = studentService.getStudent(studentId);
        Building building = buildingService.getBuilding(buildingId);
        Room room = roomService.getRoom(roomId);
        validateAssignment(student, building, room, bedNumber);

        // 每个学生只能有一条分配记录；已分配时应走调宿流程。
        if (databaseConnection.dormRepository().findByStudentId(studentId).isPresent()) {
            throw new IllegalArgumentException("该学生已分配宿舍，请使用调宿功能");
        }
        // 排除当前学生（新分配无原记录，传 null）后检查床位是否被占用。
        if (databaseConnection.dormRepository().existsByRoomIdAndBedNumber(roomId, bedNumber, null)) {
            throw new IllegalArgumentException("该床位已被占用");
        }

        return databaseConnection.dormRepository().save(new DormAssignment(0L, studentId, buildingId, roomId, bedNumber));
    }

    // 调换宿舍：校验目标宿舍关系后更新学生现有的分配记录。
    public DormAssignment changeDorm(String studentId, long buildingId, long roomId, int bedNumber) throws SQLException {
        Student student = studentService.getStudent(studentId);
        Building building = buildingService.getBuilding(buildingId);
        Room room = roomService.getRoom(roomId);
        validateAssignment(student, building, room, bedNumber);

        // 先确认学生已有分配记录；不存在则无法调宿。
        DormAssignment current = databaseConnection.dormRepository()
                .findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("该学生尚未分配宿舍"));
        // 排除学生自身原床位，避免被自己的旧记录误判为冲突。
        if (databaseConnection.dormRepository().existsByRoomIdAndBedNumber(roomId, bedNumber, studentId)) {
            throw new IllegalArgumentException("目标床位已被占用");
        }

        return databaseConnection.dormRepository().update(
                new DormAssignment(current.assignmentId(), studentId, buildingId, roomId, bedNumber)
        );
    }

    // 根据房间 ID 查询当前入住的学生列表；会先确认房间存在。
    public List<StudentDormView> findStudentsByRoom(long roomId) throws SQLException {
        roomService.getRoom(roomId);
        return studentService.getStudentsByRoomId(roomId);
    }

    // 根据学生学号查询其宿舍信息。
    public StudentDormView findDormByStudent(String studentId) throws SQLException {
        return studentService.getDormByStudentId(studentId);
    }

    // 校验房间是否属于目标楼、床号是否在 1-4 范围、以及学生性别是否符合楼的性别策略。
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
