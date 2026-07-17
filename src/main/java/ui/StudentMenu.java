package ui;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import model.Student;
import model.StudentDormView;
import service.DormService;
import service.StudentService;

/**
 * 学生管理菜单，负责与学生资料及宿舍查询相关的终端交互。
 * <p>
 * 接收用户输入并调用 {@link StudentService}、{@link DormService} 完成业务操作，再将结果输出到控制台。
 */
public final class StudentMenu {

    // 私有构造器，防止实例化；所有方法均为静态工具方法。
    private StudentMenu() {
    }

    // 显示学生管理子菜单，并根据用户选择分发到对应操作。
    public static void start(StudentService studentService, DormService dormService) throws SQLException {
        while (true) {
            ConsoleUtils.printHeader("学生管理");
            String[] headers = {"编号", "操作"};
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"[1]", "新增学生"});
            rows.add(new String[]{"[2]", "查看学生列表"});
            rows.add(new String[]{"[3]", "删除学生"});
            rows.add(new String[]{"[4]", "根据学生查找宿舍"});
            rows.add(new String[]{"[0]", "返回上级菜单"});
            ConsoleUtils.printTable(headers, rows);
            ConsoleUtils.printSeparator();

            switch (ConsoleUtils.readInt("请选择操作")) {
                case 1 -> createStudent(studentService);
                case 2 -> listStudents(studentService);
                case 3 -> deleteStudent(studentService);
                case 4 -> findDormByStudent(dormService);
                case 0 -> {
                    return;
                }
                default -> ConsoleUtils.printError("无效的选项，请重新输入。");
            }
        }
    }

    // 读取新增学生所需信息并调用服务；失败时输出错误提示。
    private static void createStudent(StudentService studentService) throws SQLException {
        ConsoleUtils.printSubHeader("新增学生");
        try {
            String studentId = ConsoleUtils.readLine("输入学号");
            String name = ConsoleUtils.readLine("输入姓名");
            String className = ConsoleUtils.readLine("输入班级");
            String grade = ConsoleUtils.readLine("输入年级");
            String gender = ConsoleUtils.readLine("输入性别（男/女）");
            Student student = studentService.createStudent(studentId, name, className, grade, gender);
            ConsoleUtils.printSuccess("新增成功: " + student.studentName());
        } catch (IllegalArgumentException exception) {
            ConsoleUtils.printError("新增失败: " + exception.getMessage());
        }
        ConsoleUtils.waitForEnter();
    }

    // 列出全部学生并以表格形式输出。
    private static void listStudents(StudentService studentService) throws SQLException {
        ConsoleUtils.printSubHeader("学生列表");
        List<Student> students = studentService.listStudents();
        String[] headers = {"学号", "姓名", "班级", "年级", "性别"};
        List<String[]> rows = new ArrayList<>();
        for (Student student : students) {
            rows.add(new String[]{
                    student.studentId(),
                    student.studentName(),
                    student.className(),
                    student.grade(),
                    student.gender().label()
            });
        }
        ConsoleUtils.printTable(headers, rows);
        ConsoleUtils.waitForEnter();
    }

    // 根据学号删除学生；若学生已有住宿分配，将依赖数据库外键级联一并删除。
    private static void deleteStudent(StudentService studentService) throws SQLException {
        ConsoleUtils.printSubHeader("删除学生");
        try {
            String studentId = ConsoleUtils.readLine("输入要删除的学号");
            Student student = studentService.deleteStudent(studentId);
            ConsoleUtils.printSuccess("删除成功: " + student.studentName() + "（" + student.studentId() + "）");
        } catch (IllegalArgumentException exception) {
            ConsoleUtils.printError("删除失败: " + exception.getMessage());
        }
        ConsoleUtils.waitForEnter();
    }

    // 根据学号查询学生宿舍信息；未分配宿舍时给出对应提示。
    private static void findDormByStudent(DormService dormService) throws SQLException {
        ConsoleUtils.printSubHeader("学生宿舍信息");
        try {
            String studentId = ConsoleUtils.readLine("输入学号");
            StudentDormView view = dormService.findDormByStudent(studentId);
            if (view.bedNumber() == null) {
                ConsoleUtils.printInfo("该学生暂未分配宿舍。");
            } else {
                String[] headers = {"字段", "值"};
                List<String[]> rows = new ArrayList<>();
                rows.add(new String[]{"学号", view.studentId()});
                rows.add(new String[]{"姓名", view.studentName()});
                rows.add(new String[]{"宿舍楼", view.buildingCode() + "(" + view.buildingName() + ")"});
                rows.add(new String[]{"房间", view.roomNumber()});
                rows.add(new String[]{"床号", String.valueOf(view.bedNumber())});
                ConsoleUtils.printTable(headers, rows);
            }
        } catch (IllegalArgumentException exception) {
            ConsoleUtils.printError("查询失败: " + exception.getMessage());
        }
        ConsoleUtils.waitForEnter();
    }
}
