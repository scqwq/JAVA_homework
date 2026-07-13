package handler;

import com.sun.net.httpserver.HttpExchange;
import model.Building;
import model.BuildingOccupancy;
import model.OccupancyOverview;
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
import java.util.Set;

public class DashboardHandler extends BaseHandler {
    private final BuildingService buildingService;
    private final RoomService roomService;
    private final StudentService studentService;
    private final DormService dormService;

    private static final Set<String> VALID_TABS = Set.of(
            "dashboard", "students", "buildings", "rooms", "dorms", "queries"
    );

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
            OccupancyOverview occupancy = dormService.getOccupancyOverview();

            String activeTab = query.getOrDefault("tab", "dashboard");
            if (!VALID_TABS.contains(activeTab)) {
                activeTab = "dashboard";
            }

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
                    activeTab,
                    query.getOrDefault("message", ""),
                    query.getOrDefault("error", ""),
                    buildings,
                    rooms,
                    students,
                    occupancy,
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
            String activeTab,
            String message,
            String error,
            List<Building> buildings,
            List<Room> rooms,
            List<Student> students,
            OccupancyOverview occupancy,
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
                            --bg: #f4f6f8;
                            --surface: #ffffff;
                            --ink: #1f2937;
                            --subtle: #6b7280;
                            --line: #e5e7eb;
                            --primary: #2563eb;
                            --primary-soft: #dbeafe;
                            --accent: #7c3aed;
                            --accent-soft: #ede9fe;
                            --success: #059669;
                            --success-soft: #d1fae5;
                            --danger: #dc2626;
                            --danger-soft: #fee2e2;
                            --radius: 16px;
                            --shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
                        }
                        * { box-sizing: border-box; }
                        body {
                            margin: 0;
                            font-family: "Microsoft YaHei", "PingFang SC", -apple-system, BlinkMacSystemFont, sans-serif;
                            color: var(--ink);
                            background: var(--bg);
                            line-height: 1.5;
                        }
                        header {
                            position: sticky;
                            top: 0;
                            z-index: 100;
                            background: rgba(255, 255, 255, 0.92);
                            backdrop-filter: blur(10px);
                            border-bottom: 1px solid var(--line);
                            box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
                        }
                        .header-inner {
                            max-width: 1400px;
                            margin: 0 auto;
                            padding: 18px 24px;
                            display: flex;
                            flex-wrap: wrap;
                            align-items: center;
                            justify-content: space-between;
                            gap: 16px;
                        }
                        h1 {
                            margin: 0;
                            font-size: 22px;
                            display: flex;
                            align-items: center;
                            gap: 10px;
                        }
                        .subtitle {
                            margin: 4px 0 0;
                            font-size: 13px;
                            color: var(--subtle);
                        }
                        nav {
                            display: flex;
                            flex-wrap: wrap;
                            gap: 8px;
                        }
                        nav a {
                            padding: 8px 16px;
                            border-radius: 999px;
                            text-decoration: none;
                            font-size: 14px;
                            font-weight: 500;
                            color: var(--subtle);
                            transition: all 0.15s ease;
                        }
                        nav a:hover {
                            background: var(--primary-soft);
                            color: var(--primary);
                        }
                        nav a.active {
                            background: var(--primary);
                            color: #fff;
                            box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
                        }
                        main {
                            max-width: 1400px;
                            margin: 0 auto;
                            padding: 24px;
                        }
                        .notice, .error {
                            padding: 14px 18px;
                            border-radius: var(--radius);
                            margin-bottom: 20px;
                            font-size: 14px;
                            display: flex;
                            align-items: center;
                            gap: 10px;
                        }
                        .notice {
                            background: var(--success-soft);
                            color: var(--success);
                            border: 1px solid #a7f3d0;
                        }
                        .error {
                            background: var(--danger-soft);
                            color: var(--danger);
                            border: 1px solid #fecaca;
                        }
                        .tab-content {
                            display: none;
                            animation: fadeIn 0.25s ease;
                        }
                        .tab-content.active {
                            display: block;
                        }
                        @keyframes fadeIn {
                            from { opacity: 0; transform: translateY(6px); }
                            to { opacity: 1; transform: translateY(0); }
                        }
                        .stats {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
                            gap: 16px;
                            margin-bottom: 24px;
                        }
                        .stat-card {
                            background: var(--surface);
                            border-radius: var(--radius);
                            padding: 20px;
                            box-shadow: var(--shadow);
                            border: 1px solid var(--line);
                        }
                        .stat-card h3 {
                            margin: 0 0 6px;
                            font-size: 13px;
                            color: var(--subtle);
                            font-weight: 500;
                        }
                        .stat-card .number {
                            font-size: 32px;
                            font-weight: 700;
                            color: var(--primary);
                        }
                        .occupancy-summary {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
                            gap: 16px;
                            margin-bottom: 20px;
                        }
                        .summary-item {
                            display: flex;
                            flex-direction: column;
                            gap: 4px;
                        }
                        .summary-label {
                            font-size: 13px;
                            color: var(--subtle);
                        }
                        .summary-value {
                            font-size: 24px;
                            font-weight: 700;
                            color: var(--ink);
                        }
                        .summary-value.primary { color: var(--primary); }
                        .summary-value.accent { color: var(--accent); }
                        .progress-bar {
                            height: 14px;
                            background: #e5e7eb;
                            border-radius: 999px;
                            overflow: hidden;
                        }
                        .progress-bar.slim {
                            height: 8px;
                            margin-top: 8px;
                        }
                        .progress-fill {
                            height: 100%;
                            background: linear-gradient(90deg, var(--primary), #60a5fa);
                            border-radius: 999px;
                            transition: width 0.4s ease;
                        }
                        .occupancy-row {
                            padding: 14px 0;
                            border-bottom: 1px solid var(--line);
                        }
                        .occupancy-row:last-child {
                            border-bottom: none;
                            padding-bottom: 0;
                        }
                        .occupancy-info {
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            gap: 12px;
                        }
                        .occupancy-name {
                            font-weight: 600;
                        }
                        .occupancy-detail {
                            font-size: 13px;
                            color: var(--subtle);
                        }
                        .occupancy-rate {
                            font-size: 18px;
                            font-weight: 700;
                            color: var(--primary);
                            text-align: right;
                            margin-bottom: 4px;
                        }
                        .card {
                            background: var(--surface);
                            border-radius: var(--radius);
                            padding: 24px;
                            box-shadow: var(--shadow);
                            border: 1px solid var(--line);
                            margin-bottom: 20px;
                        }
                        .card h2 {
                            margin: 0 0 18px;
                            font-size: 18px;
                            display: flex;
                            align-items: center;
                            gap: 8px;
                        }
                        .two-col {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
                            gap: 20px;
                        }
                        .form-grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                            gap: 16px;
                        }
                        .form-group {
                            display: flex;
                            flex-direction: column;
                            gap: 6px;
                        }
                        .form-group.full {
                            grid-column: 1 / -1;
                        }
                        label {
                            font-size: 13px;
                            color: var(--subtle);
                            font-weight: 500;
                        }
                        input, select {
                            padding: 10px 12px;
                            border-radius: 10px;
                            border: 1px solid var(--line);
                            font-size: 14px;
                            background: #fff;
                            outline: none;
                            transition: border 0.15s;
                        }
                        input:focus, select:focus {
                            border-color: var(--primary);
                            box-shadow: 0 0 0 3px var(--primary-soft);
                        }
                        button {
                            padding: 10px 18px;
                            border: none;
                            border-radius: 10px;
                            background: var(--primary);
                            color: #fff;
                            font-size: 14px;
                            font-weight: 600;
                            cursor: pointer;
                            transition: all 0.15s;
                        }
                        button:hover {
                            filter: brightness(1.05);
                            box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
                        }
                        button.secondary {
                            background: var(--accent);
                        }
                        button.secondary:hover {
                            box-shadow: 0 4px 12px rgba(124, 58, 237, 0.25);
                        }
                        .actions {
                            display: flex;
                            justify-content: flex-end;
                            margin-top: 8px;
                        }
                        table {
                            width: 100%;
                            border-collapse: collapse;
                            font-size: 14px;
                        }
                        th, td {
                            padding: 12px;
                            text-align: left;
                            border-bottom: 1px solid var(--line);
                        }
                        th {
                            background: #f9fafb;
                            color: var(--subtle);
                            font-weight: 600;
                            font-size: 13px;
                            position: sticky;
                            top: 0;
                        }
                        tr:hover td {
                            background: #f9fafb;
                        }
                        .pill {
                            display: inline-block;
                            padding: 3px 10px;
                            border-radius: 999px;
                            background: var(--primary-soft);
                            color: var(--primary);
                            font-size: 12px;
                            font-weight: 600;
                        }
                        .pill.accent {
                            background: var(--accent-soft);
                            color: var(--accent);
                        }
                        .result {
                            margin-top: 16px;
                            padding: 16px;
                            border-radius: var(--radius);
                            background: #f9fafb;
                            border: 1px dashed var(--line);
                            font-size: 14px;
                        }
                        .empty {
                            color: var(--subtle);
                            font-size: 14px;
                            padding: 16px;
                            text-align: center;
                        }
                        .quick-links {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
                            gap: 12px;
                        }
                        .quick-links a {
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            gap: 8px;
                            padding: 14px;
                            border-radius: 12px;
                            background: var(--surface);
                            border: 1px solid var(--line);
                            text-decoration: none;
                            color: var(--ink);
                            font-weight: 500;
                            transition: all 0.15s;
                        }
                        .quick-links a:hover {
                            border-color: var(--primary);
                            color: var(--primary);
                            background: var(--primary-soft);
                        }
                        .scroll-x {
                            overflow-x: auto;
                            border-radius: var(--radius);
                            border: 1px solid var(--line);
                        }
                        @media (max-width: 640px) {
                            .header-inner { flex-direction: column; align-items: flex-start; }
                            nav { width: 100%; }
                            nav a { flex: 1; text-align: center; }
                        }
                    </style>
                </head>
                <body>
                <header>
                    <div class="header-inner">
                        <div>
                            <h1>🏠 学生宿舍管理系统</h1>
                            <p class="subtitle">浏览器端管理学生、楼栋、房间与住宿分配</p>
                        </div>
                        <nav>
                """);

        html.append(navLink("dashboard", "🏠 首页", activeTab))
                .append(navLink("students", "🎓 学生", activeTab))
                .append(navLink("buildings", "🏢 楼栋", activeTab))
                .append(navLink("rooms", "🚪 房间", activeTab))
                .append(navLink("dorms", "🛏️ 分配/调宿", activeTab))
                .append(navLink("queries", "🔍 查询", activeTab));

        html.append("""
                        </nav>
                    </div>
                </header>
                <main>
                """);

        if (!message.isBlank()) {
            html.append("<div class=\"notice\">✅ ").append(escapeHtml(message)).append("</div>");
        }
        if (!error.isBlank()) {
            html.append("<div class=\"error\">❌ ").append(escapeHtml(error)).append("</div>");
        }

        // Dashboard
        html.append("<section id=\"dashboard\" class=\"tab-content").append(activeClass("dashboard", activeTab)).append("\">");
        html.append(renderDashboard(students.size(), buildings.size(), rooms.size(), occupancy));
        html.append("</section>");

        // Students
        html.append("<section id=\"students\" class=\"tab-content").append(activeClass("students", activeTab)).append("\">");
        html.append(renderStudentSection(students));
        html.append("</section>");

        // Buildings
        html.append("<section id=\"buildings\" class=\"tab-content").append(activeClass("buildings", activeTab)).append("\">");
        html.append(renderBuildingSection(buildings));
        html.append("</section>");

        // Rooms
        html.append("<section id=\"rooms\" class=\"tab-content").append(activeClass("rooms", activeTab)).append("\">");
        html.append(renderRoomSection(buildings, rooms));
        html.append("</section>");

        // Dorms
        html.append("<section id=\"dorms\" class=\"tab-content").append(activeClass("dorms", activeTab)).append("\">");
        html.append(renderDormSection(buildings, rooms));
        html.append("</section>");

        // Queries
        html.append("<section id=\"queries\" class=\"tab-content").append(activeClass("queries", activeTab)).append("\">");
        html.append(renderQuerySection(lookupStudentId, dormByStudent, lookupRoomId, studentsByRoom));
        html.append("</section>");

        html.append("""
                </main>
                </body>
                </html>
                """);
        return html.toString();
    }

    private String activeClass(String tab, String activeTab) {
        return tab.equals(activeTab) ? " active" : "";
    }

    private String navLink(String tab, String label, String activeTab) {
        String active = tab.equals(activeTab) ? " class=\"active\"" : "";
        return "<a" + active + " href=\"/?tab=" + tab + "\">" + label + "</a>";
    }

    private String renderDashboard(int studentCount, int buildingCount, int roomCount, OccupancyOverview occupancy) {
        long vacantBeds = occupancy.totalBeds() - occupancy.occupiedBeds();
        double overallRate = Math.min(100.0, occupancy.occupancyRate());
        return """
                <div class="stats">
                    <div class="stat-card">
                        <h3>学生总数</h3>
                        <div class="number">""" + studentCount + """
                        </div>
                    </div>
                    <div class="stat-card">
                        <h3>宿舍楼栋</h3>
                        <div class="number">""" + buildingCount + """
                        </div>
                    </div>
                    <div class="stat-card">
                        <h3>房间总数</h3>
                        <div class="number">""" + roomCount + """
                        </div>
                    </div>
                </div>
                """ + renderOccupancyPanel(occupancy.totalBeds(), occupancy.occupiedBeds(), vacantBeds, overallRate)
                + renderBuildingOccupancy(occupancy.buildingStats()) + """
                <div class="card">
                    <h2>⚡ 快速入口</h2>
                    <div class="quick-links">
                        <a href="/?tab=students">🎓 管理学生</a>
                        <a href="/?tab=buildings">🏢 管理楼栋</a>
                        <a href="/?tab=rooms">🚪 管理房间</a>
                        <a href="/?tab=dorms">🛏️ 分配宿舍</a>
                        <a href="/?tab=queries">🔍 住宿查询</a>
                    </div>
                </div>
                """;
    }

    private String renderOccupancyPanel(long totalBeds, long occupiedBeds, long vacantBeds, double occupancyRate) {
        return "<div class=\"card\">"
                + "<h2>📊 入住率概览</h2>"
                + "<div class=\"occupancy-summary\">"
                + "<div class=\"summary-item\"><span class=\"summary-label\">总床位</span><span class=\"summary-value\">" + totalBeds + "</span></div>"
                + "<div class=\"summary-item\"><span class=\"summary-label\">已入住</span><span class=\"summary-value primary\">" + occupiedBeds + "</span></div>"
                + "<div class=\"summary-item\"><span class=\"summary-label\">空置床位</span><span class=\"summary-value\">" + vacantBeds + "</span></div>"
                + "<div class=\"summary-item\"><span class=\"summary-label\">整体入住率</span><span class=\"summary-value accent\">" + occupancyRate + "%</span></div>"
                + "</div>"
                + "<div class=\"progress-bar\"><div class=\"progress-fill\" style=\"width: " + occupancyRate + "%\"></div></div>"
                + "</div>";
    }

    private String renderBuildingOccupancy(List<BuildingOccupancy> stats) {
        if (stats.isEmpty()) {
            return "<div class=\"card\"><h2>🏢 各楼栋入住情况</h2><div class=\"empty\">暂无楼栋数据</div></div>";
        }
        StringBuilder rows = new StringBuilder();
        rows.append("<div class=\"card\"><h2>🏢 各楼栋入住情况</h2>");
        for (BuildingOccupancy stat : stats) {
            double rate = Math.min(100.0, stat.occupancyRate());
            rows.append("<div class=\"occupancy-row\">"
                    + "<div class=\"occupancy-info\">"
                    + "<span class=\"occupancy-name\">").append(escapeHtml(stat.buildingName())).append("</span>"
                    + "<span class=\"occupancy-detail\">").append(stat.occupiedBeds()).append(" / ").append(stat.totalBeds()).append(" 床 · ").append(stat.totalRooms()).append(" 间</span>"
                    + "</div>"
                    + "<div class=\"occupancy-rate\">").append(stat.occupancyRate()).append("%</div>"
                    + "<div class=\"progress-bar slim\"><div class=\"progress-fill\" style=\"width: ").append(rate).append("%\"></div></div>"
                    + "</div>");
        }
        rows.append("</div>");
        return rows.toString();
    }

    private String renderStudentSection(List<Student> students) {
        return """
                <div class="two-col">
                    <div class="card">
                        <h2>🎓 新增学生</h2>
                        <form method="post" action="/students/create">
                            <div class="form-grid">
                                <div class="form-group">
                                    <label>学号</label>
                                    <input name="studentId" placeholder="例如 20240001" required>
                                </div>
                                <div class="form-group">
                                    <label>姓名</label>
                                    <input name="name" placeholder="例如 张三" required>
                                </div>
                                <div class="form-group">
                                    <label>班级</label>
                                    <input name="className" placeholder="例如 计科 1 班" required>
                                </div>
                                <div class="form-group">
                                    <label>年级</label>
                                    <input name="grade" placeholder="例如 2024" required>
                                </div>
                                <div class="form-group">
                                    <label>性别</label>
                                    <select name="gender">
                                        <option value="男">男</option>
                                        <option value="女">女</option>
                                    </select>
                                </div>
                            </div>
                            <div class="actions">
                                <button type="submit">创建学生</button>
                            </div>
                        </form>
                    </div>
                    <div class="card">
                        <h2>📋 学生列表</h2>
                        """ + renderStudentTable(students) + """
                    </div>
                </div>
                """;
    }

    private String renderBuildingSection(List<Building> buildings) {
        return """
                <div class="two-col">
                    <div class="card">
                        <h2>🏢 新增宿舍楼</h2>
                        <form method="post" action="/buildings/create">
                            <div class="form-grid">
                                <div class="form-group">
                                    <label>楼栋编号</label>
                                    <input name="code" placeholder="例如 A1" required>
                                </div>
                                <div class="form-group">
                                    <label>楼栋名称</label>
                                    <input name="name" placeholder="例如 一舍" required>
                                </div>
                                <div class="form-group">
                                    <label>入住策略</label>
                                    <select name="genderPolicy">
                                        <option value="男">男生楼</option>
                                        <option value="女">女生楼</option>
                                        <option value="男女分层">男女分层</option>
                                    </select>
                                </div>
                            </div>
                            <div class="actions">
                                <button type="submit">创建宿舍楼</button>
                            </div>
                        </form>
                    </div>
                    <div class="card">
                        <h2>📋 宿舍楼列表</h2>
                        """ + renderBuildingTable(buildings) + """
                    </div>
                </div>
                """;
    }

    private String renderRoomSection(List<Building> buildings, List<Room> rooms) {
        return """
                <div class="two-col">
                    <div class="card">
                        <h2>🚪 新增房间</h2>
                        <form method="post" action="/rooms/create">
                            <div class="form-grid">
                                <div class="form-group">
                                    <label>房间号</label>
                                    <input name="roomNumber" placeholder="例如 101" required>
                                </div>
                                <div class="form-group">
                                    <label>所属宿舍楼</label>
                                    <select name="buildingId">
                                        """ + renderBuildingOptions(buildings) + """
                                    </select>
                                </div>
                                <div class="form-group">
                                    <label>楼层</label>
                                    <input type="number" name="floorNumber" min="1" placeholder="例如 1" required>
                                </div>
                            </div>
                            <div class="actions">
                                <button type="submit">创建房间</button>
                            </div>
                        </form>
                    </div>
                    <div class="card">
                        <h2>📋 房间列表</h2>
                        """ + renderRoomTable(rooms, buildings) + """
                    </div>
                </div>
                """;
    }

    private String renderDormSection(List<Building> buildings, List<Room> rooms) {
        return """
                <div class="two-col">
                    <div class="card">
                        <h2>🛏️ 分配宿舍</h2>
                        <form method="post" action="/dorms/assign">
                            <div class="form-grid">
                                <div class="form-group">
                                    <label>学号</label>
                                    <input name="studentId" placeholder="输入已存在的学号" required>
                                </div>
                                """ + renderBuildingSelect(buildings, "buildingId") + """
                                """ + renderRoomSelect(rooms, "roomId") + """
                                <div class="form-group">
                                    <label>床号</label>
                                    <input type="number" name="bedNumber" min="1" max="4" required>
                                </div>
                            </div>
                            <div class="actions">
                                <button type="submit">分配床位</button>
                            </div>
                        </form>
                    </div>
                    <div class="card">
                        <h2>🔄 调换宿舍</h2>
                        <form method="post" action="/dorms/change">
                            <div class="form-grid">
                                <div class="form-group">
                                    <label>学号</label>
                                    <input name="studentId" placeholder="输入已入住的学号" required>
                                </div>
                                """ + renderBuildingSelect(buildings, "buildingId") + """
                                """ + renderRoomSelect(rooms, "roomId") + """
                                <div class="form-group">
                                    <label>新床号</label>
                                    <input type="number" name="bedNumber" min="1" max="4" required>
                                </div>
                            </div>
                            <div class="actions">
                                <button type="submit" class="secondary">提交调宿</button>
                            </div>
                        </form>
                    </div>
                </div>
                """;
    }

    private String renderQuerySection(
            String lookupStudentId,
            StudentDormView dormByStudent,
            String lookupRoomId,
            List<StudentDormView> studentsByRoom
    ) {
        return """
                <div class="two-col">
                    <div class="card">
                        <h2>🔍 按学号查宿舍</h2>
                        <form method="get" action="/?tab=queries">
                            <div class="form-grid">
                                <div class="form-group">
                                    <label>学号</label>
                                    <input name="lookupStudentId" value=""" + "\"" + escapeHtml(lookupStudentId) + "\"" + """
                                           placeholder="例如 20240001" required>
                                </div>
                            </div>
                            <div class="actions">
                                <button type="submit">查询住宿信息</button>
                            </div>
                        </form>
                        """ + renderStudentDormResult(dormByStudent) + """
                    </div>
                    <div class="card">
                        <h2>🔍 按房间查学生</h2>
                        <form method="get" action="/?tab=queries">
                            <div class="form-grid">
                                <div class="form-group">
                                    <label>房间 ID</label>
                                    <input name="lookupRoomId" value=""" + "\"" + escapeHtml(lookupRoomId) + "\"" + """
                                           placeholder="例如 1" required>
                                </div>
                            </div>
                            <div class="actions">
                                <button type="submit">查询入住名单</button>
                            </div>
                        </form>
                        """ + renderRoomStudentsResult(studentsByRoom) + """
                    </div>
                </div>
                """;
    }

    private String renderStudentDormResult(StudentDormView dormByStudent) {
        if (dormByStudent == null) {
            return "";
        }
        return """
                <div class="result">
                    <strong>查询结果</strong><br><br>
                    学号：""" + escapeHtml(dormByStudent.studentId()) + """
                    <br>
                    姓名：""" + escapeHtml(dormByStudent.studentName()) + """
                    <br>
                    宿舍楼：""" + escapeHtml(nullToDash(dormByStudent.buildingName())) + """
                    <br>
                    房间号：""" + escapeHtml(nullToDash(dormByStudent.roomNumber())) + """
                    <br>
                    床号：""" + (dormByStudent.bedNumber() == null ? "-" : dormByStudent.bedNumber()) + """
                </div>
                """;
    }

    private String renderRoomStudentsResult(List<StudentDormView> studentsByRoom) {
        if (studentsByRoom == null || studentsByRoom.isEmpty()) {
            return "";
        }
        StringBuilder rows = new StringBuilder();
        for (StudentDormView view : studentsByRoom) {
            rows.append(escapeHtml(view.studentName()))
                    .append("（").append(escapeHtml(view.studentId())).append("）")
                    .append(" - 床号 ").append(view.bedNumber()).append("<br>");
        }
        return "<div class=\"result\"><strong>查询结果</strong><br><br>" + rows + "</div>";
    }

    private String renderStudentTable(List<Student> students) {
        if (students.isEmpty()) {
            return "<div class=\"empty\">暂无学生记录</div>";
        }
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
                <div class="scroll-x">
                    <table>
                        <thead>
                        <tr><th>学号</th><th>姓名</th><th>班级</th><th>年级</th><th>性别</th></tr>
                        </thead>
                        <tbody>
                """ + rows + """
                        </tbody>
                    </table>
                </div>
                """;
    }

    private String renderBuildingTable(List<Building> buildings) {
        if (buildings.isEmpty()) {
            return "<div class=\"empty\">暂无宿舍楼记录</div>";
        }
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
                <div class="scroll-x">
                    <table>
                        <thead>
                        <tr><th>ID</th><th>编号</th><th>名称</th><th>入住策略</th></tr>
                        </thead>
                        <tbody>
                """ + rows + """
                        </tbody>
                    </table>
                </div>
                """;
    }

    private String renderRoomTable(List<Room> rooms, List<Building> buildings) {
        Map<Long, Building> buildingsById = new java.util.HashMap<>();
        for (Building building : buildings) {
            buildingsById.put(building.buildingId(), building);
        }
        if (rooms.isEmpty()) {
            return "<div class=\"empty\">暂无房间记录</div>";
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
                <div class="scroll-x">
                    <table>
                        <thead>
                        <tr><th>ID</th><th>房间号</th><th>所属楼栋</th><th>楼层</th></tr>
                        </thead>
                        <tbody>
                """ + rows + """
                        </tbody>
                    </table>
                </div>
                """;
    }

    private String renderBuildingOptions(List<Building> buildings) {
        StringBuilder options = new StringBuilder();
        for (Building building : buildings) {
            options.append("<option value=\"\"")
                    .append(building.buildingId())
                    .append("\">")
                    .append(escapeHtml(building.buildingCode()))
                    .append(" - ")
                    .append(escapeHtml(building.buildingName()))
                    .append("</option>");
        }
        return options.toString();
    }

    private String renderBuildingSelect(List<Building> buildings, String fieldName) {
        StringBuilder options = new StringBuilder();
        for (Building building : buildings) {
            options.append("<option value=\"\"")
                    .append(building.buildingId())
                    .append("\">")
                    .append(escapeHtml(building.buildingCode()))
                    .append(" - ")
                    .append(escapeHtml(building.buildingName()))
                    .append("</option>");
        }
        return """
                <div class="form-group">
                    <label>宿舍楼</label>
                    <select name='""" + fieldName + """
                        >
                """ + options + """
                    </select>
                </div>
                """;
    }

    private String renderRoomSelect(List<Room> rooms, String fieldName) {
        StringBuilder options = new StringBuilder();
        for (Room room : rooms) {
            options.append("<option value=\"\"")
                    .append(room.roomId())
                    .append("\">房间 ID ")
                    .append(room.roomId())
                    .append(" / 房间号 ")
                    .append(escapeHtml(room.roomNumber()))
                    .append("</option>");
        }
        return """
                <div class="form-group">
                    <label>房间</label>
                    <select name='""" + fieldName + """
                        >
                """ + options + """
                    </select>
                </div>
                """;
    }

    private String renderErrorPage(String error) {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>请求失败</title>
                    <style>
                        body {
                            margin: 0;
                            font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
                            background: #f4f6f8;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            min-height: 100vh;
                        }
                        .box {
                            background: #fff;
                            border-radius: 20px;
                            padding: 40px;
                            box-shadow: 0 8px 30px rgba(0,0,0,0.08);
                            max-width: 480px;
                            text-align: center;
                        }
                        h2 { margin: 0 0 16px; color: #dc2626; }
                        p { color: #4b5563; margin-bottom: 24px; }
                        a {
                            display: inline-block;
                            padding: 10px 20px;
                            background: #2563eb;
                            color: #fff;
                            text-decoration: none;
                            border-radius: 10px;
                            font-weight: 600;
                        }
                        a:hover { filter: brightness(1.05); }
                    </style>
                </head>
                <body>
                    <div class="box">
                        <h2>❌ 请求失败</h2>
                        <p>""" + escapeHtml(error) + """
                        </p>
                        <a href="/">返回首页</a>
                    </div>
                </body>
                </html>
                """;
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
