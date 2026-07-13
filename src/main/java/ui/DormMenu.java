package ui;

import model.StudentDormView;
import service.DormService;
import service.RoomService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 调换宿舍管理菜单，负责与学生入住、调宿及宿舍查询相关的终端交互。
 * <p>
 * 接收用户输入并调用 {@link DormService} 完成业务操作，再将结果输出到控制台。
 */
public final class DormMenu {

    // 私有构造器，防止实例化；所有方法均为静态工具方法。
    private DormMenu() {
    }

    // 显示调换宿舍管理子菜单，并根据用户选择分发到对应操作。
    public static void start(DormService dormService, RoomService roomService) throws SQLException {
        while (true) {
            ConsoleUtils.printHeader("调换宿舍管理");
            String[] headers = {"编号", "操作"};
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"[1]", "分配宿舍"});
            rows.add(new String[]{"[2]", "调换宿舍"});
            rows.add(new String[]{"[3]", "根据宿舍查找学生"});
            rows.add(new String[]{"[0]", "返回上级菜单"});
            ConsoleUtils.printTable(headers, rows);
            ConsoleUtils.printSeparator();

            switch (ConsoleUtils.readInt("请选择操作")) {
                case 1 -> assignDorm(dormService);
                case 2 -> changeDorm(dormService);
                case 3 -> findStudentsByRoom(dormService, roomService);
                case 0 -> {
                    return;
                }
                default -> ConsoleUtils.printError("无效的选项，请重新输入。");
            }
        }
    }

    // 读取分配宿舍所需信息并调用服务；失败时输出错误提示。
    private static void assignDorm(DormService dormService) throws SQLException {
        ConsoleUtils.printSubHeader("分配宿舍");
        try {
            String studentId = ConsoleUtils.readLine("输入学生学号");
            long buildingId = ConsoleUtils.readLong("输入宿舍楼ID");
            long roomId = ConsoleUtils.readLong("输入房间ID");
            int bedNumber = ConsoleUtils.readInt("输入床号（1-4）");
            dormService.assignDorm(studentId, buildingId, roomId, bedNumber);
            ConsoleUtils.printSuccess("分配成功。");
        } catch (IllegalArgumentException exception) {
            ConsoleUtils.printError("分配失败: " + exception.getMessage());
        }
        ConsoleUtils.waitForEnter();
    }

    // 读取调换宿舍所需信息并调用服务；失败时输出错误提示。
    private static void changeDorm(DormService dormService) throws SQLException {
        ConsoleUtils.printSubHeader("调换宿舍");
        try {
            String studentId = ConsoleUtils.readLine("输入学生学号");
            long buildingId = ConsoleUtils.readLong("输入新宿舍楼ID");
            long roomId = ConsoleUtils.readLong("输入新房间ID");
            int bedNumber = ConsoleUtils.readInt("输入新床号（1-4）");
            dormService.changeDorm(studentId, buildingId, roomId, bedNumber);
            ConsoleUtils.printSuccess("调宿成功。");
        } catch (IllegalArgumentException exception) {
            ConsoleUtils.printError("调宿失败: " + exception.getMessage());
        }
        ConsoleUtils.waitForEnter();
    }

    // 根据房间 ID 查询入住学生并以表格形式输出；无入住时给出空提示。
    private static void findStudentsByRoom(DormService dormService, RoomService roomService) throws SQLException {
        ConsoleUtils.printSubHeader("宿舍入住学生");
        try {
            long roomId = ConsoleUtils.readLong("输入房间ID");
            roomService.getRoom(roomId);
            List<StudentDormView> students = dormService.findStudentsByRoom(roomId);
            if (students.isEmpty()) {
                ConsoleUtils.printInfo("该宿舍当前没有学生入住。");
            } else {
                String[] headers = {"学号", "姓名", "班级", "床号", "宿舍楼", "房间"};
                List<String[]> rows = new ArrayList<>();
                for (StudentDormView student : students) {
                    rows.add(new String[]{
                            student.studentId(),
                            student.studentName(),
                            student.className(),
                            String.valueOf(student.bedNumber()),
                            student.buildingCode(),
                            student.roomNumber()
                    });
                }
                ConsoleUtils.printTable(headers, rows);
            }
        } catch (IllegalArgumentException exception) {
            ConsoleUtils.printError("查询失败: " + exception.getMessage());
        }
        ConsoleUtils.waitForEnter();
    }
}
