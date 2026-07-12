package ui;

import model.StudentDormView;
import service.DormService;
import service.RoomService;

import java.sql.SQLException;
import java.util.List;

public final class DormMenu {
    private DormMenu() {
    }

    public static void start(DormService dormService, RoomService roomService) throws SQLException {
        while (true) {
            ConsoleUtils.printHeader("调换宿舍管理");
            System.out.println("1. 分配宿舍");
            System.out.println("2. 调换宿舍");
            System.out.println("3. 根据宿舍查找学生");
            System.out.println("0. 返回上级菜单");

            switch (ConsoleUtils.readInt("请选择操作")) {
                case 1 -> assignDorm(dormService);
                case 2 -> changeDorm(dormService);
                case 3 -> findStudentsByRoom(dormService, roomService);
                case 0 -> {
                    return;
                }
                default -> System.out.println("无效的选项，请重新输入。");
            }
        }
    }

    private static void assignDorm(DormService dormService) throws SQLException {
        try {
            String studentId = ConsoleUtils.readLine("输入学生学号");
            long buildingId = ConsoleUtils.readLong("输入宿舍楼ID");
            long roomId = ConsoleUtils.readLong("输入房间ID");
            int bedNumber = ConsoleUtils.readInt("输入床号（1-4）");
            dormService.assignDorm(studentId, buildingId, roomId, bedNumber);
            System.out.println("分配成功。");
        } catch (IllegalArgumentException exception) {
            System.out.println("分配失败: " + exception.getMessage());
        }
        ConsoleUtils.waitForEnter();
    }

    private static void changeDorm(DormService dormService) throws SQLException {
        try {
            String studentId = ConsoleUtils.readLine("输入学生学号");
            long buildingId = ConsoleUtils.readLong("输入新宿舍楼ID");
            long roomId = ConsoleUtils.readLong("输入新房间ID");
            int bedNumber = ConsoleUtils.readInt("输入新床号（1-4）");
            dormService.changeDorm(studentId, buildingId, roomId, bedNumber);
            System.out.println("调宿成功。");
        } catch (IllegalArgumentException exception) {
            System.out.println("调宿失败: " + exception.getMessage());
        }
        ConsoleUtils.waitForEnter();
    }

    private static void findStudentsByRoom(DormService dormService, RoomService roomService) throws SQLException {
        try {
            long roomId = ConsoleUtils.readLong("输入房间ID");
            roomService.getRoom(roomId);
            List<StudentDormView> students = dormService.findStudentsByRoom(roomId);
            if (students.isEmpty()) {
                System.out.println("该宿舍当前没有学生入住。");
            } else {
                for (StudentDormView student : students) {
                    System.out.printf("学号=%s, 姓名=%s, 班级=%s, 床号=%d, 宿舍楼=%s, 房间=%s%n",
                            student.studentId(), student.studentName(), student.className(),
                            student.bedNumber(), student.buildingCode(), student.roomNumber());
                }
            }
        } catch (IllegalArgumentException exception) {
            System.out.println("查询失败: " + exception.getMessage());
        }
        ConsoleUtils.waitForEnter();
    }
}

