package handler;

import com.sun.net.httpserver.HttpExchange;
import model.Building;
import model.Room;
import model.Student;
import model.StudentDormView;
import service.BuildingService;
import service.DormService;
import service.RoomService;
import service.StudentService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class DashboardHandler extends BaseHandler {
    private final BuildingService buildingService;
    private final RoomService roomService;
    private final StudentService studentService;
    private final DormService dormService;

    public DashboardHandler(
            BuildingService buildingService,
            RoomService roomService,
            StudentService studentService,
            DormService dormService
    ) {
        this.buildingService = buildingService;
        this.roomService = roomService;
        this.studentService = studentService;
        this.dormService = dormService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            ensureMethod(exchange, "GET");
            Map<String, String> query = queryParams(exchange);
            List<Building> buildings = buildingService.listBuildings();
            List<Room> rooms = roomService.listRooms();
            List<Student> students = studentService.listStudents();

            StudentDormView dormByStudent = null;
            if (!query.getOrDefault("lookupStudentId", "").isBlank()) {
                dormByStudent = dormService.findDormByStudent(query.get("lookupStudentId").trim());
            }

            List<StudentDormView> studentsByRoom = List.of();
            String lookupRoomId = query.getOrDefault("lookupRoomId", "").trim();
            if (!lookupRoomId.isBlank()) {
                studentsByRoom = dormService.findStudentsByRoom(Long.parseLong(lookupRoomId));
            }

            sendHtml(exchange, 200, renderPage(
                    query.getOrDefault("message", ""),
                    query.getOrDefault("error", ""),
                    buildings,
                    rooms,
                    students,
                    dormByStudent,
                    studentsByRoom,
                    query.getOrDefault("lookupStudentId", ""),
                    lookupRoomId
            ));
        } catch (IllegalArgumentException exception) {
            sendHtml(exchange, 400, renderErrorPage(exception.getMessage()));
        } catch (SQLException exception) {
            sendHtml(exchange, 500, renderErrorPage("数据库操作失败: " + exception.getMessage()));
        }
    }

    private String renderPage(
            String message,
            String error,
            List<Building> buildings,
            List<Room> rooms,
            List<Student> students,
            StudentDormView dormByStudent,
            List<StudentDormView> studentsByRoom,
            String lookupStudentId,
            String lookupRoomId
    ) {
        StringBuilder html = new StringBuilder("""
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>学生宿舍管理系统</title>
                    <style>
                        :root {
                            --bg: #f4efe6;
                            --card: #fffaf2;
                            --ink: #1f2937;
                            --subtle: #6b7280;
                            --line: #d6c7b2;
                            --accent: #b45309;
                            --accent-soft: #fde7c2;
                            --danger: #b91c1c;
                            --ok: #166534;
                        }
                        * { box-sizing: border-box; }
                        body {
                            margin: 0;
                            font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
                            color: var(--ink);
                            background:
                                radial-gradient(circle at top left, #fff3d6 0, transparent 28%),
                                linear-gradient(180deg, #f7f1e8 0%, var(--bg) 100%);
                        }
                        .page {
                            max-width: 1400px;
                            margin: 0 auto;
                            padding: 32px 20px 48px;
                        }
                        h1 {
                            margin: 0 0 8px;
                            font-size: 38px;
                        }
                        .subtitle {
                            color: var(--subtle);
                            margin-bottom: 24px;
                        }
                        .notice, .error {
                            padding: 14px 16px;
                            border-radius: 14px;
                            margin-bottom: 18px;
                        }
                        .notice {
                            background: #dcfce7;
                            color: var(--ok);
                        }
                        .error {
                            background: #fee2e2;
                            color: var(--danger);
                        }
                        .grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
                            gap: 18px;
                        }
                        .card {
                            background: var(--card);
                            border: 1px solid rgba(214, 199, 178, 0.9);
                            border-radius: 22px;
                            padding: 20px;
                            box-shadow: 0 10px 30px rgba(120, 92, 42, 0.08);
                        }
                        h2 {
                            margin-top: 0;
                            font-size: 22px;
                        }
                        form {
                            display: grid;
                            gap: 10px;
                        }
                        label {
                            font-size: 14px;
                            color: var(--subtle);
                        }
                        input, select, button {
                            width: 100%;
                            padding: 10px 12px;
                            border-radius: 12px;
                            border: 1px solid var(--line);
                            font-size: 15px;
                            background: white;
                        }
                        button {
                            border: none;
                            background: linear-gradient(135deg, #c2410c, var(--accent));
                            color: white;
                            font-weight: 700;
                            cursor: pointer;
                        }
                        button:hover {
                            filter: brightness(1.05);
                        }
                        table {
                            width: 100%;
                            border-collapse: collapse;
                            margin-top: 12px;
                            font-size: 14px;
                        }
                        th, td {
                            text-align: left;
                            padding: 10px 8px;
                            border-bottom: 1px solid #eadfce;
                            vertical-align: top;
                        }
                        th {
                            color: var(--subtle);
                        }
                        .pill {
                            display: inline-block;
                            padding: 4px 10px;
                            border-radius: 999px;
                            background: var(--accent-soft);
                            color: var(--accent);
                            font-size: 12px;
                            font-weight: 700;
                        }
                        .wide {
                            grid-column: 1 / -1;
                        }
                        .result {
                            margin-top: 14px;
                            padding: 14px;
                            border-radius: 14px;
                            background: #fff;
                            border: 1px dashed var(--line);
                        }
                    </style>
                </head>
                <body>
                <div class="page">
                    <h1>学生宿舍管理系统</h1>
                    <div class="subtitle">当前已切换为浏览器访问方式，所有操作都可以在这个管理页面完成。</div>
                """);

        if (!message.isBlank()) {
            html.append("<div class=\"notice\">").append(escapeHtml(message)).append("</div>");
        }
        if (!error.isBlank()) {
            html.append("<div class=\"error\">").append(escapeHtml(error)).append("</div>");
        }

        html.append("""
                    <div class="grid">
                """);
        html.append(renderStudentForm());
        html.append(renderBuildingForm(buildings));
        html.append(renderRoomForm(buildings));
        html.append(renderAssignForm(buildings, rooms));
        html.append(renderChangeForm(buildings, rooms));
        html.append(renderStudentLookupForm(lookupStudentId, dormByStudent));
        html.append(renderRoomLookupForm(lookupRoomId, studentsByRoom));
        html.append(renderStudentTable(students));
        html.append(renderBuildingTable(buildings));
        html.append(renderRoomTable(rooms, buildings));
        html.append("""
                    </div>
                </div>
                </body>
                </html>
                """);
        return html.toString();
    }

    private String renderStudentForm() {
        return """
                <section class="card">
                    <h2>新增学生</h2>
                    <form method="post" action="/students/create">
                        <label>学号<input name="studentId" placeholder="例如 20240001" required></label>
                        <label>姓名<input name="name" placeholder="例如 张三" required></label>
                        <label>班级<input name="className" placeholder="例如 计科 1 班" required></label>
                        <label>年级<input name="grade" placeholder="例如 2024" required></label>
                        <label>性别
                            <select name="gender">
                                <option value="男">男</option>
                                <option value="女">女</option>
                            </select>
                        </label>
                        <button type="submit">创建学生</button>
                    </form>
                </section>
                """;
    }

    private String renderBuildingForm(List<Building> buildings) {
        return """
                <section class="card">
                    <h2>新增宿舍楼</h2>
                    <form method="post" action="/buildings/create">
                        <label>楼栋编号<input name="code" placeholder="例如 A1" required></label>
                        <label>楼栋名称<input name="name" placeholder="例如 一舍" required></label>
                        <label>入住策略
                            <select name="genderPolicy">
                                <option value="男">男生楼</option>
                                <option value="女">女生楼</option>
                                <option value="男女分层">男女分层</option>
                            </select>
                        </label>
                        <button type="submit">创建宿舍楼</button>
                    </form>
                    <div class="result">当前楼栋数量：<span class="pill">""" + buildings.size() + "</span></div>\n" +
                "                </section>\n";
    }

    private String renderRoomForm(List<Building> buildings) {
        StringBuilder options = new StringBuilder();
        for (Building building : buildings) {
            options.append("<option value=\"")
                    .append(building.buildingId())
                    .append("\">")
                    .append(escapeHtml(building.buildingCode()))
                    .append(" - ")
                    .append(escapeHtml(building.buildingName()))
                    .append("</option>");
        }
        return """
                <section class="card">
                    <h2>新增房间</h2>
                    <form method="post" action="/rooms/create">
                        <label>房间号<input name="roomNumber" placeholder="例如 101" required></label>
                        <label>所属宿舍楼
                            <select name="buildingId">
                """ + options + """
                            </select>
                        </label>
                        <label>楼层<input type="number" name="floorNumber" min="1" placeholder="例如 1" required></label>
                        <button type="submit">创建房间</button>
                    </form>
                </section>
                """;
    }

    private String renderAssignForm(List<Building> buildings, List<Room> rooms) {
        return """
                <section class="card">
                    <h2>分配宿舍</h2>
                    <form method="post" action="/dorms/assign">
                        <label>学号<input name="studentId" placeholder="输入已存在的学号" required></label>
                        """ + renderBuildingSelect(buildings, "buildingId") + """
                        """ + renderRoomSelect(rooms, "roomId") + """
                        <label>床号<input type="number" name="bedNumber" min="1" max="4" required></label>
                        <button type="submit">分配床位</button>
                    </form>
                </section>
                """;
    }

    private String renderChangeForm(List<Building> buildings, List<Room> rooms) {
        return """
                <section class="card">
                    <h2>调换宿舍</h2>
                    <form method="post" action="/dorms/change">
                        <label>学号<input name="studentId" placeholder="输入已入住的学号" required></label>
                        """ + renderBuildingSelect(buildings, "buildingId") + """
                        """ + renderRoomSelect(rooms, "roomId") + """
                        <label>新床号<input type="number" name="bedNumber" min="1" max="4" required></label>
                        <button type="submit">提交调宿</button>
                    </form>
                </section>
                """;
    }

    private String renderStudentLookupForm(String lookupStudentId, StudentDormView dormByStudent) {
        StringBuilder result = new StringBuilder();
        if (!lookupStudentId.isBlank() && dormByStudent != null) {
            result.append("<div class=\"result\">")
                    .append("学号：").append(escapeHtml(dormByStudent.studentId())).append("<br>")
                    .append("姓名：").append(escapeHtml(dormByStudent.studentName())).append("<br>")
                    .append("宿舍楼：").append(escapeHtml(nullToDash(dormByStudent.buildingName()))).append("<br>")
                    .append("房间号：").append(escapeHtml(nullToDash(dormByStudent.roomNumber()))).append("<br>")
                    .append("床号：").append(dormByStudent.bedNumber() == null ? "-" : dormByStudent.bedNumber())
                    .append("</div>");
        }
        return """
                <section class="card">
                    <h2>按学号查宿舍</h2>
                    <form method="get" action="/">
                        <label>学号<input name="lookupStudentId" value="
                """ + escapeHtml(lookupStudentId) + """
                " placeholder="例如 20240001" required></label>
                        <button type="submit">查询住宿信息</button>
                    </form>
                """ + result + """
                </section>
                """;
    }

    private String renderRoomLookupForm(String lookupRoomId, List<StudentDormView> studentsByRoom) {
        StringBuilder result = new StringBuilder();
        if (!lookupRoomId.isBlank()) {
            result.append("<div class=\"result\">");
            if (studentsByRoom.isEmpty()) {
                result.append("当前房间暂无学生入住。");
            } else {
                for (StudentDormView view : studentsByRoom) {
                    result.append(escapeHtml(view.studentName()))
                            .append("（")
                            .append(escapeHtml(view.studentId()))
                            .append("）- 床号 ")
                            .append(view.bedNumber())
                            .append("<br>");
                }
            }
            result.append("</div>");
        }
        return """
                <section class="card">
                    <h2>按房间查学生</h2>
                    <form method="get" action="/">
                        <label>房间 ID<input name="lookupRoomId" value="
                """ + escapeHtml(lookupRoomId) + """
                " placeholder="例如 1" required></label>
                        <button type="submit">查询入住名单</button>
                    </form>
                """ + result + """
                </section>
                """;
    }

    private String renderStudentTable(List<Student> students) {
        StringBuilder rows = new StringBuilder();
        for (Student student : students) {
            rows.append("<tr><td>")
                    .append(escapeHtml(student.studentId()))
                    .append("</td><td>")
                    .append(escapeHtml(student.studentName()))
                    .append("</td><td>")
                    .append(escapeHtml(student.className()))
                    .append("</td><td>")
                    .append(escapeHtml(student.grade()))
                    .append("</td><td>")
                    .append(escapeHtml(student.gender().label()))
                    .append("</td></tr>");
        }
        return """
                <section class="card wide">
                    <h2>学生列表</h2>
                    <table>
                        <thead>
                        <tr><th>学号</th><th>姓名</th><th>班级</th><th>年级</th><th>性别</th></tr>
                        </thead>
                        <tbody>
                """ + rows + """
                        </tbody>
                    </table>
                </section>
                """;
    }

    private String renderBuildingTable(List<Building> buildings) {
        StringBuilder rows = new StringBuilder();
        for (Building building : buildings) {
            rows.append("<tr><td>")
                    .append(building.buildingId())
                    .append("</td><td>")
                    .append(escapeHtml(building.buildingCode()))
                    .append("</td><td>")
                    .append(escapeHtml(building.buildingName()))
                    .append("</td><td>")
                    .append(escapeHtml(building.genderPolicy().label()))
                    .append("</td></tr>");
        }
        return """
                <section class="card wide">
                    <h2>宿舍楼列表</h2>
                    <table>
                        <thead>
                        <tr><th>ID</th><th>编号</th><th>名称</th><th>入住策略</th></tr>
                        </thead>
                        <tbody>
                """ + rows + """
                        </tbody>
                    </table>
                </section>
                """;
    }

    private String renderRoomTable(List<Room> rooms, List<Building> buildings) {
        Map<Long, Building> buildingsById = new java.util.HashMap<>();
        for (Building building : buildings) {
            buildingsById.put(building.buildingId(), building);
        }

        StringBuilder rows = new StringBuilder();
        for (Room room : rooms) {
            Building building = buildingsById.get(room.buildingId());
            rows.append("<tr><td>")
                    .append(room.roomId())
                    .append("</td><td>")
                    .append(escapeHtml(room.roomNumber()))
                    .append("</td><td>")
                    .append(building == null ? "-" : escapeHtml(building.buildingName()))
                    .append("</td><td>")
                    .append(room.floorNumber())
                    .append("</td></tr>");
        }
        return """
                <section class="card wide">
                    <h2>房间列表</h2>
                    <table>
                        <thead>
                        <tr><th>ID</th><th>房间号</th><th>所属楼栋</th><th>楼层</th></tr>
                        </thead>
                        <tbody>
                """ + rows + """
                        </tbody>
                    </table>
                </section>
                """;
    }

    private String renderBuildingSelect(List<Building> buildings, String fieldName) {
        StringBuilder options = new StringBuilder();
        for (Building building : buildings) {
            options.append("<option value=\"")
                    .append(building.buildingId())
                    .append("\">")
                    .append(escapeHtml(building.buildingCode()))
                    .append(" - ")
                    .append(escapeHtml(building.buildingName()))
                    .append("</option>");
        }
        return """
                <label>宿舍楼
                    <select name="
                """ + fieldName + """
                ">
                """ + options + """
                    </select>
                </label>
                """;
    }

    private String renderRoomSelect(List<Room> rooms, String fieldName) {
        StringBuilder options = new StringBuilder();
        for (Room room : rooms) {
            options.append("<option value=\"")
                    .append(room.roomId())
                    .append("\">房间 ID ")
                    .append(room.roomId())
                    .append(" / 房间号 ")
                    .append(escapeHtml(room.roomNumber()))
                    .append("</option>");
        }
        return """
                <label>房间
                    <select name="
                """ + fieldName + """
                ">
                """ + options + """
                    </select>
                </label>
                """;
    }

    private String renderErrorPage(String error) {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head><meta charset="UTF-8"><title>请求失败</title></head>
                <body style="font-family: Microsoft YaHei, sans-serif; padding: 24px;">
                    <h2>请求失败</h2>
                    <p>
                """ + escapeHtml(error) + """
                    </p>
                    <p><a href="/">返回首页</a></p>
                </body>
                </html>
                """;
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
