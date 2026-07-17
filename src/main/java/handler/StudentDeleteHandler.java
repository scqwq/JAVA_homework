package handler;

import com.sun.net.httpserver.HttpExchange;
import model.Student;
import service.StudentService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class StudentDeleteHandler extends BaseHandler {
    private final StudentService studentService;

    public StudentDeleteHandler(StudentService studentService) {
        this.studentService = studentService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            ensureMethod(exchange, "POST");
            Map<String, String> params = formParams(exchange);
            Student student = studentService.deleteStudent(
                    requireText(params, "studentId", "学号")
            );
            redirect(exchange, "/?tab=students&message=" + encode("学生删除成功: " + student.studentName()));
        } catch (IllegalArgumentException exception) {
            redirect(exchange, "/?tab=students&error=" + encode(exception.getMessage()));
        } catch (SQLException exception) {
            redirect(exchange, "/?tab=students&error=" + encode("数据库操作失败: " + exception.getMessage()));
        }
    }
}
