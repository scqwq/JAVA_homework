package ui;

import java.sql.SQLException;

import model.Student;
import model.StudentDormView;
import service.DormService;
import service.StudentService;

public final class StudentMenu {
    private StudentMenu() {
    }

    public static void start(StudentService studentService, DormService dormService) throws SQLException {
        while (true) {
            ConsoleUtils.printHeader("学生管理");
            System.out.println("1. 新增学生");
            System.out.println("2. 查看学生列表");
            System.out.println("3. 根据学生查找宿舍");
            System.out.println("0. 返回上级菜单");

            switch (ConsoleUtils.readInt("请选择操作")) {
                case 1 -> createStudent(studentService);
                case 2 -> listStudents(studentService);
                case 3 -> findDormByStudent(dormService);
                case 0 -> {
                    return;
                }
                default -> System.out.println("无效的选项，请重新输入。");
            }
        }
    }

    private static void createStudent(StudentService studentService) throws SQLException {
        try {
            String studentId = ConsoleUtils.readLine("输入学号");
            String name = ConsoleUtils.readLine("输入姓名");
            String className = ConsoleUtils.readLine("输入班级");
            String grade = ConsoleUtils.readLine("输入年级");
            String gender = ConsoleUtils.readLine("输入性别（男/女）");
            Student student = studentService.createStudent(studentId, name, className, grade, gender);
            System.out.println("新增成功: " + student.studentName());
        } catch (IllegalArgumentException exception) {
            System.out.println("新增失败: " + exception.getMessage());
        }
        ConsoleUtils.waitForEnter();
    }

    private static void listStudents(StudentService studentService) throws SQLException {
        for (Student student : studentService.listStudents()) {
            System.out.printf("学号=%s, 姓名=%s, 班级=%s, 年级=%s, 性别=%s%n",
                    student.studentId(), student.studentName(), student.className(), student.grade(), student.gender().label());
        }
        ConsoleUtils.waitForEnter();
    }

    private static void findDormByStudent(DormService dormService) throws SQLException {
        try {
            String studentId = ConsoleUtils.readLine("输入学号");
            StudentDormView view = dormService.findDormByStudent(studentId);
            if (view.bedNumber() == null) {
                System.out.println("该学生暂未分配宿舍。");
            } else {
                System.out.printf("学号=%s, 姓名=%s, 宿舍楼=%s(%s), 房间=%s, 床号=%d%n",
                        view.studentId(), view.studentName(), view.buildingCode(), view.buildingName(),
                        view.roomNumber(), view.bedNumber());
            }
        } catch (IllegalArgumentException exception) {
            System.out.println("查询失败: " + exception.getMessage());
        }
        ConsoleUtils.waitForEnter();
    }
}

