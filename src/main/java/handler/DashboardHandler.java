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
import java.util.ArrayList;
import java.util.HashMap;
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
        Map<String, String> query = queryParams(exchange);
        List<Building> buildings = List.of();
        List<Room> rooms = List.of();
        List<Student> students = List.of();
        StudentDormView dormByStudent = null;
        List<StudentDormView> studentsByRoom = List.of();
        Room roomByLocation = null;
        try {
            ensureMethod(exchange, "GET");
            buildings = buildingService.listBuildings();
            rooms = roomService.listRooms();
            students = studentService.listStudents();

            if (!query.getOrDefault("lookupStudentId", "").isBlank()) {
                dormByStudent = dormService.findDormByStudent(query.get("lookupStudentId").trim());
            }

            String lookupRoomId = query.getOrDefault("lookupRoomId", "").trim();
            if (!lookupRoomId.isBlank()) {
                studentsByRoom = dormService.findStudentsByRoom(Long.parseLong(lookupRoomId));
            }

            String lookupBuildingId = query.getOrDefault("lookupBuildingId", "").trim();
            String lookupFloorNumber = query.getOrDefault("lookupFloorNumber", "").trim();
            String lookupRoomNumber = query.getOrDefault("lookupRoomNumber", "").trim();
            if (!lookupBuildingId.isBlank() || !lookupFloorNumber.isBlank() || !lookupRoomNumber.isBlank()) {
                roomByLocation = roomService.getRoomByLocation(
                        Long.parseLong(lookupBuildingId),
                        Integer.parseInt(lookupFloorNumber),
                        lookupRoomNumber
                );
                studentsByRoom = dormService.findStudentsByRoom(roomByLocation.roomId());
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
                    lookupRoomId,
                    roomByLocation,
                    lookupBuildingId,
                    lookupFloorNumber,
                    lookupRoomNumber,
                    query.getOrDefault("changeStudentId", ""),
                    query.getOrDefault("changeBuildingId", ""),
                    query.getOrDefault("changeRoomId", ""),
                    query.getOrDefault("changeBedNumber", "")
            ));
        } catch (IllegalArgumentException exception) {
            if (buildings.isEmpty() && rooms.isEmpty() && students.isEmpty()) {
                try {
                    buildings = buildingService.listBuildings();
                    rooms = roomService.listRooms();
                    students = studentService.listStudents();
                } catch (SQLException sqlException) {
                    sendHtml(exchange, 500, renderErrorPage("数据库操作失败: " + sqlException.getMessage()));
                    return;
                }
            }
            sendHtml(exchange, 400, renderPage(
                    query.getOrDefault("message", ""),
                    exception.getMessage(),
                    buildings,
                    rooms,
                    students,
                    dormByStudent,
                    studentsByRoom,
                    query.getOrDefault("lookupStudentId", ""),
                    query.getOrDefault("lookupRoomId", ""),
                    roomByLocation,
                    query.getOrDefault("lookupBuildingId", ""),
                    query.getOrDefault("lookupFloorNumber", ""),
                    query.getOrDefault("lookupRoomNumber", ""),
                    query.getOrDefault("changeStudentId", ""),
                    query.getOrDefault("changeBuildingId", ""),
                    query.getOrDefault("changeRoomId", ""),
                    query.getOrDefault("changeBedNumber", "")
            ));
        } catch (SQLException exception) {
            sendHtml(exchange, 500, renderPage(
                    query.getOrDefault("message", ""),
                    "数据库操作失败: " + exception.getMessage(),
                    buildings,
                    rooms,
                    students,
                    dormByStudent,
                    studentsByRoom,
                    query.getOrDefault("lookupStudentId", ""),
                    query.getOrDefault("lookupRoomId", ""),
                    roomByLocation,
                    query.getOrDefault("lookupBuildingId", ""),
                    query.getOrDefault("lookupFloorNumber", ""),
                    query.getOrDefault("lookupRoomNumber", ""),
                    query.getOrDefault("changeStudentId", ""),
                    query.getOrDefault("changeBuildingId", ""),
                    query.getOrDefault("changeRoomId", ""),
                    query.getOrDefault("changeBedNumber", "")
            ));
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
            String lookupRoomId,
            Room roomByLocation,
            String lookupBuildingId,
            String lookupFloorNumber,
            String lookupRoomNumber,
            String changeStudentId,
            String changeBuildingId,
            String changeRoomId,
            String changeBedNumber
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
        html.append(renderChangeForm(
                buildings,
                rooms,
                changeStudentId,
                changeBuildingId,
                changeRoomId,
                changeBedNumber
        ));
        html.append(renderStudentLookupForm(lookupStudentId, dormByStudent));
        html.append(renderRoomLookupForm(
                buildings,
                lookupRoomId,
                studentsByRoom,
                roomByLocation,
                lookupBuildingId,
                lookupFloorNumber,
                lookupRoomNumber
        ));
        html.append(renderStudentTable(students));
        html.append(renderBuildingTable(buildings));
        html.append(renderRoomTable(rooms, buildings));
        html.append(renderFormScript());
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
        if (buildings.isEmpty()) {
            return """
                    <section class="card">
                        <h2>新增房间</h2>
                        <div class="result">请先创建宿舍楼，再为楼栋添加房间。</div>
                    </section>
                    """;
        }

        StringBuilder html = new StringBuilder();
        html.append("""
                <section class="card">
                    <h2>新增房间</h2>
                    <form method="post" action="/rooms/create">
                        <label>房间号<input name="roomNumber" placeholder="例如 101" required></label>
                        <label>所属宿舍楼
                            <select name="buildingId" required>
                """);
        appendBuildingOptions(html, buildings, "请选择宿舍楼");
        html.append("""
                            </select>
                        </label>
                        <label>楼层<input type="number" name="floorNumber" min="1" placeholder="例如 1" required></label>
                        <button type="submit">创建房间</button>
                    </form>
                </section>
                """);
        return html.toString();
    }

    private String renderAssignForm(List<Building> buildings, List<Room> rooms) {
        return renderDormForm(
                "分配宿舍",
                "/dorms/assign",
                "assign-form",
                "输入已存在的学号",
                "分配床位",
                "当前没有宿舍楼，请先创建宿舍楼和房间，再进行分配。",
                buildings,
                rooms,
                "assign-dorm",
                "",
                "",
                "",
                ""
        );
    }

    private String renderChangeForm(
            List<Building> buildings,
            List<Room> rooms,
            String studentId,
            String buildingId,
            String roomId,
            String bedNumber
    ) {
        return renderDormForm(
                "调换宿舍",
                "/dorms/change",
                "change-form",
                "输入已入住的学号",
                "提交调宿",
                "当前没有可调换的宿舍资源，请先创建宿舍楼和房间。",
                buildings,
                rooms,
                "change-dorm",
                studentId,
                buildingId,
                roomId,
                bedNumber
        );
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
                <section class="card" id="student-lookup">
                    <h2>按学号查宿舍</h2>
                    <form method="get" action="/#student-lookup">
                        <label>学号<input name="lookupStudentId" value="
                """ + escapeHtml(lookupStudentId) + """
                " placeholder="例如 20240001" required></label>
                        <button type="submit">查询住宿信息</button>
                    </form>
                """ + result + """
                </section>
                """;
    }

    private String renderRoomLookupForm(
            List<Building> buildings,
            String lookupRoomId,
            List<StudentDormView> studentsByRoom,
            Room roomByLocation,
            String lookupBuildingId,
            String lookupFloorNumber,
            String lookupRoomNumber
    ) {
        StringBuilder result = new StringBuilder();
        if (!lookupRoomId.isBlank() || roomByLocation != null) {
            result.append("<div class=\"result\">");
            if (roomByLocation != null) {
                result.append("当前查询房间：房间 ID ").append(roomByLocation.roomId())
                        .append(" / 楼层 ").append(roomByLocation.floorNumber())
                        .append(" / 房间号 ").append(escapeHtml(roomByLocation.roomNumber()))
                        .append("<br>");
            }
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

        StringBuilder html = new StringBuilder();
        html.append("""
                <section class="card" id="room-lookup">
                    <h2>按房间查学生</h2>
                    <form method="get" action="/#room-lookup">
                """);
        html.append("        <label>房间 ID<input name=\"lookupRoomId\" value=\"")
                .append(escapeHtml(lookupRoomId))
                .append("\" placeholder=\"例如 1\"></label>\n");
        html.append("""
                        <button type="submit">按房间 ID 查询</button>
                    </form>
                """);

        html.append("""
                    <form method="get" action="/#room-lookup">
                        <label>所属宿舍楼
                            <select name="lookupBuildingId" required>
                """);
        appendBuildingOptionsWithSelected(html, buildings, "请选择宿舍楼", lookupBuildingId);
        html.append("""
                            </select>
                        </label>
                """);
        html.append("        <label>楼层<input type=\"number\" name=\"lookupFloorNumber\" min=\"1\" value=\"")
                .append(escapeHtml(lookupFloorNumber))
                .append("\" placeholder=\"例如 3\" required></label>\n");
        html.append("        <label>房间号<input name=\"lookupRoomNumber\" value=\"")
                .append(escapeHtml(lookupRoomNumber))
                .append("\" placeholder=\"例如 301\" required></label>\n");
        html.append("""
                        <button type="submit">按楼层和房间号查询</button>
                    </form>
                """);

        html.append(result).append("""
                </section>
                """);
        return html.toString();
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
        Map<Long, Building> buildingsById = new HashMap<>();
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

    private String renderDormForm(
            String title,
            String action,
            String formClass,
            String studentPlaceholder,
            String buttonLabel,
            String emptyMessage,
            List<Building> buildings,
            List<Room> rooms,
            String sectionId,
            String selectedStudentId,
            String selectedBuildingId,
            String selectedRoomId,
            String selectedBedNumber
    ) {
        if (buildings.isEmpty()) {
            return "<section class=\"card\" id=\"" + escapeHtml(sectionId) + "\"><h2>" + escapeHtml(title)
                    + "</h2><div class=\"result\">" + escapeHtml(emptyMessage) + "</div></section>";
        }

        Map<Long, List<Room>> roomsByBuilding = groupRoomsByBuilding(rooms);
        StringBuilder html = new StringBuilder();
        html.append("<section class=\"card\" id=\"").append(escapeHtml(sectionId)).append("\">");
        html.append("<h2>").append(escapeHtml(title)).append("</h2>");
        html.append("<form method=\"post\" action=\"").append(action).append("\" class=\"dorm-form ")
                .append(formClass).append("\" data-selected-room-id=\"").append(escapeHtml(selectedRoomId)).append("\">");
        html.append("<label>学号<input name=\"studentId\" placeholder=\"").append(escapeHtml(studentPlaceholder))
                .append("\" value=\"").append(escapeHtml(selectedStudentId)).append("\" required></label>");
        html.append("<label>宿舍楼<select name=\"buildingId\" class=\"building-select\" required>");
        appendBuildingOptionsWithSelected(html, buildings, "请选择宿舍楼", selectedBuildingId);
        html.append("</select></label>");
        html.append("<label>房间");
        html.append("<select name=\"roomId\" class=\"room-select\" required disabled>");
        html.append("<option value=\"\">请先选择宿舍楼</option>");
        html.append("</select></label>");
        html.append("<label>床号<input type=\"number\" name=\"bedNumber\" min=\"1\" max=\"4\" value=\"")
                .append(escapeHtml(selectedBedNumber)).append("\" required></label>");
        html.append("<button type=\"submit\">").append(escapeHtml(buttonLabel)).append("</button>");
        html.append("</form>");

        if (rooms.isEmpty()) {
            html.append("<div class=\"result\">当前还没有房间，请先给宿舍楼添加房间。</div>");
        } else {
            html.append("<div class=\"result\">");
            html.append("先选宿舍楼，再选该楼栋下的房间。");
            for (Building building : buildings) {
                int roomCount = roomsByBuilding.getOrDefault(building.buildingId(), List.of()).size();
                html.append("<br>").append(escapeHtml(building.buildingName()))
                        .append("：").append(roomCount).append(" 个房间");
            }
            html.append("</div>");
        }

        html.append("</section>");
        return html.toString();
    }

    private void appendBuildingOptions(StringBuilder html, List<Building> buildings, String placeholder) {
        html.append("<option value=\"\">").append(escapeHtml(placeholder)).append("</option>");
        for (Building building : buildings) {
            html.append("<option value=\"").append(building.buildingId()).append("\">")
                    .append(escapeHtml(building.buildingCode()))
                    .append(" - ")
                    .append(escapeHtml(building.buildingName()))
                    .append("</option>");
        }
    }

    private void appendBuildingOptionsWithSelected(
            StringBuilder html,
            List<Building> buildings,
            String placeholder,
            String selectedBuildingId
    ) {
        html.append("<option value=\"\">").append(escapeHtml(placeholder)).append("</option>");
        for (Building building : buildings) {
            html.append("<option value=\"").append(building.buildingId()).append("\"");
            if (String.valueOf(building.buildingId()).equals(selectedBuildingId)) {
                html.append(" selected");
            }
            html.append(">")
                    .append(escapeHtml(building.buildingCode()))
                    .append(" - ")
                    .append(escapeHtml(building.buildingName()))
                    .append("</option>");
        }
    }

    private Map<Long, List<Room>> groupRoomsByBuilding(List<Room> rooms) {
        Map<Long, List<Room>> roomsByBuilding = new HashMap<>();
        for (Room room : rooms) {
            roomsByBuilding.computeIfAbsent(room.buildingId(), ignored -> new ArrayList<>()).add(room);
        }
        return roomsByBuilding;
    }

    private String renderFormScript() {
        return """
                <script>
                    const roomMap = {
                """ + buildRoomMapJson() + """
                    };

                    if (window.location.hash) {
                        const target = document.querySelector(window.location.hash);
                        if (target) {
                            target.scrollIntoView({ behavior: "smooth", block: "start" });
                        }
                    }

                    document.querySelectorAll(".dorm-form").forEach((form) => {
                        const buildingSelect = form.querySelector(".building-select");
                        const roomSelect = form.querySelector(".room-select");
                        const selectedRoomId = form.dataset.selectedRoomId || "";

                        const syncRooms = () => {
                            const buildingId = buildingSelect.value;
                            const rooms = roomMap[buildingId] || [];
                            roomSelect.innerHTML = "";

                            if (!buildingId) {
                                roomSelect.disabled = true;
                                roomSelect.innerHTML = '<option value="">请先选择宿舍楼</option>';
                                return;
                            }

                            if (rooms.length === 0) {
                                roomSelect.disabled = true;
                                roomSelect.innerHTML = '<option value="">该宿舍楼下暂无房间</option>';
                                return;
                            }

                            roomSelect.disabled = false;
                            roomSelect.innerHTML = '<option value="">请选择房间</option>';
                            rooms.forEach((room) => {
                                const option = document.createElement("option");
                                option.value = room.id;
                                option.textContent = room.label;
                                if (selectedRoomId && selectedRoomId === room.id) {
                                    option.selected = true;
                                }
                                roomSelect.appendChild(option);
                            });
                        };

                        buildingSelect.addEventListener("change", syncRooms);
                        syncRooms();
                    });
                </script>
                """;
    }

    private String buildRoomMapJson() {
        try {
            List<Room> rooms = roomService.listRooms();
            Map<Long, List<Room>> roomsByBuilding = groupRoomsByBuilding(rooms);
            StringBuilder json = new StringBuilder();
            boolean firstBuilding = true;
            for (Map.Entry<Long, List<Room>> entry : roomsByBuilding.entrySet()) {
                if (!firstBuilding) {
                    json.append(",");
                }
                firstBuilding = false;
                json.append("\"").append(entry.getKey()).append("\":[");
                boolean firstRoom = true;
                for (Room room : entry.getValue()) {
                    if (!firstRoom) {
                        json.append(",");
                    }
                    firstRoom = false;
                    json.append("{\"id\":\"").append(room.roomId()).append("\",\"label\":\"房间 ID ")
                            .append(room.roomId()).append(" / 房间号 ")
                            .append(escapeJs(room.roomNumber())).append("\"}");
                }
                json.append("]");
            }
            return json.toString();
        } catch (SQLException exception) {
            return "";
        }
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

    private String escapeJs(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
