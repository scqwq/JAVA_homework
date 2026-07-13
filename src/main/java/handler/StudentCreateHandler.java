package handler;

import com.sun.net.httpserver.HttpExchange;
import model.Student;
import service.StudentService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class StudentCreateHandler extends BaseHandler {
    private final StudentService studentService;

    public StudentCreateHandler(StudentService studentService) {
        this.studentService = studentService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            ensureMethod(exchange, "POST");
            Map<String, String> params = formParams(exchange);
            Student student = studentService.createStudent(
                    requireText(params, "studentId", "学号"),
                    requireText(params, "name", "姓名"),
                    requireText(params, "className", "班级"),
                    requireText(params, "grade", "年级"),
                    requireText(params, "gender", "性别")
            );
            redirect(exchange, "/?message=" + encode("学生创建成功: " + student.studentName()));
        } catch (IllegalArgumentException exception) {
            redirect(exchange, "/?error=" + encode(exception.getMessage()));
        } catch (SQLException exception) {
            redirect(exchange, "/?error=" + encode("数据库操作失败: " + exception.getMessage()));
        }
    }
}
