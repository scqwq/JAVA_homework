# JAVA_homework

学生宿舍管理系统，基于 Java 21 + JDBC + Maven，实现了：

- 学生管理
- 宿舍楼管理
- 宿舍房间管理
- 宿舍分配与调换
- 根据宿舍查找学生
- 根据学生查找宿舍

项目使用接口 + JDBC 实现仓储层多态，当前支持 `PostgreSQL` 和 `MySQL`。

## 控制台中文编码

如果终端使用 UTF-8 编码（如 VS Code 终端、Git Bash、PowerShell 7+ 等），而程序输出出现乱码，可在运行前设置环境变量：

```bash
# Windows (PowerShell)
$env:CONSOLE_ENCODING = "UTF-8"
.\mvnw.cmd exec:java

# Windows (cmd)
set CONSOLE_ENCODING=UTF-8
mvn exec:java

# macOS / Linux / Git Bash
export CONSOLE_ENCODING=UTF-8
./mvnw exec:java
```

在中文 Windows 默认的 GBK 控制台中，通常无需设置此变量。

## Maven 目录结构

- Java 源码位于 `src/main/java`
- SQL 资源位于 `src/main/resources/sql`

## 快速开始

1. 复制 `.env.example` 为 `.env`
2. 配置数据库连接串
3. 如果你的电脑已经安装 Maven，执行：

```bash
mvn clean compile
mvn exec:java
```

4. 如果另一台电脑没有全局安装 Maven，也可以直接用项目自带的 Maven Wrapper：

Windows:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd exec:java
```

macOS / Linux:

```bash
chmod +x mvnw
./mvnw clean compile
./mvnw exec:java
```

程序启动时会自动建表。`mvnw` / `mvnw.cmd` 首次运行时会从 `.mvn/wrapper/maven-wrapper.properties` 指定的地址下载 Maven，因此第一次执行需要联网。

## 部署到 ECS

如果你已经把项目传到 Linux ECS、安装好了 Java 21，并且 PostgreSQL 已创建 `JAVA` 数据库，可以按下面运行：

1. 复制配置文件：

```bash
cp .env.example .env
```

2. 编辑 `.env`，填写数据库连接，例如：

```env
APP_PORT=8080
DATABASE_DSN=jdbc:postgresql://127.0.0.1:5432/JAVA?user=postgres&password=123456
PGDSN=
MYSQL_DSN=
```

3. 一键编译并启动网站：

```bash
bash scripts/run-ecs.sh
```

4. 启动成功后，在浏览器访问：

```text
http://服务器公网IP:8080
```

如果 ECS 开了防火墙或华为云安全组，记得放行 `8080` 端口。

### ECS 上如何测试

先在服务器本机测试：

```bash
curl http://127.0.0.1:8080
```

如果返回 HTML，说明程序已经正常启动。

再从你自己的电脑浏览器访问：

```text
http://ECS公网IP:8080
```

如果本机能通、外网不能通，通常是华为云安全组或服务器防火墙没有放行 `8080`。

## 演示数据

程序首次启动会自动建表，但不会自动导入演示数据。建表后可手动执行
`src/main/resources/sql/init.sql`；脚本可重复执行，不会重复插入相同的演示记录。

PostgreSQL：

```powershell
psql -h 127.0.0.1 -U postgres -d JAVA -f src/main/resources/sql/init.sql
```

MySQL：

```bash
mysql -h 127.0.0.1 -u root -p JAVA < src/main/resources/sql/init.sql
```

## 表结构设计

- `buildings`
  - 主键：`building_id`
  - 唯一键：`building_code`
- `rooms`
  - 主键：`room_id`
  - 外键：`building_id -> buildings.building_id`
  - 唯一键：`(building_id, room_number)`
- `students`
  - 主键：`student_id`
- `dorm_assignments`
  - 主键：`assignment_id`
  - 外键：`student_id -> students.student_id`
  - 外键：`building_id -> buildings.building_id`
  - 外键：`room_id -> rooms.room_id`
  - 唯一键：`student_id`
  - 唯一键：`(room_id, bed_number)`

约束说明：

- 每个房间固定 4 张床，床号范围 `1-4`
- 同一房间同一床位不能重复占用
- 一个学生同时只能有一条住宿分配记录

## 说明

- 宿舍楼性别支持：`男`、`女`、`男女分层`
- `男女分层` 在本实现中表示该楼允许男女入住；如果后续需要精确到“某层仅男/某层仅女”，可以继续在 `rooms` 或楼层规则表上扩展
