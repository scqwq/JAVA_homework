package database;

import java.util.List;

/**
 * 数据库方言枚举。
 * <p>
 * 封装不同数据库在 DDL（建表语句）语法上的差异。
 * 当前支持两种数据库：
 * <ul>
 *   <li>{@link #POSTGRESQL} — PostgreSQL，使用 BIGSERIAL 自增主键</li>
 *   <li>{@link #MYSQL} — MySQL，使用 BIGINT AUTO_INCREMENT 自增主键</li>
 * </ul>
 * 每个枚举常量持有一组建表 SQL 语句，由 {@link DatabaseConnection} 在启动时执行。
 */
public enum DatabaseDialect {

    /**
     * PostgreSQL 方言。
     * <p>
     * 建表 SQL 说明：
     * <ol>
     *   <li><strong>buildings</strong> — 宿舍楼表：楼号（building_code）唯一，性别策略（gender_policy）标识该楼入住性别规则。</li>
     *   <li><strong>rooms</strong> — 房间表：每个房间属于一栋楼（building_id 外键），同一楼内房间号唯一（uq_room_per_building）。</li>
     *   <li><strong>students</strong> — 学生表：学号（student_id）为主键，记录姓名、班级、年级、性别。</li>
     *   <li><strong>dorm_assignments</strong> — 宿舍分配表：
     *     <ul>
     *       <li>每个学生只能有一个分配记录（student_id UNIQUE）</li>
     *       <li>每个房间最多 4 个床位（chk_bed_range CHECK 约束）</li>
     *       <li>同一床位号在同一个房间内唯一（uq_room_bed）</li>
     *       <li>删除学生/楼/房间时级联删除关联分配（ON DELETE CASCADE）</li>
     *     </ul>
     *   </li>
     * </ol>
     */
    POSTGRESQL(List.of( // POSTGRESQL = 一个自带 4 段 PostgreSQL 建表 SQL 的方言配置
            """
            CREATE TABLE IF NOT EXISTS buildings (
                building_id BIGSERIAL PRIMARY KEY,
                building_code VARCHAR(50) NOT NULL UNIQUE,
                building_name VARCHAR(100) NOT NULL,
                gender_policy VARCHAR(20) NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS rooms (
                room_id BIGSERIAL PRIMARY KEY,
                room_number VARCHAR(50) NOT NULL,
                building_id BIGINT NOT NULL REFERENCES buildings(building_id) ON DELETE CASCADE,
                floor_number INT NOT NULL,
                CONSTRAINT uq_room_per_building UNIQUE (building_id, room_number)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS students (
                student_id VARCHAR(50) PRIMARY KEY,
                student_name VARCHAR(100) NOT NULL,
                class_name VARCHAR(100) NOT NULL,
                grade VARCHAR(20) NOT NULL,
                gender VARCHAR(20) NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS dorm_assignments (
                assignment_id BIGSERIAL PRIMARY KEY,
                student_id VARCHAR(50) NOT NULL UNIQUE REFERENCES students(student_id) ON DELETE CASCADE,
                building_id BIGINT NOT NULL REFERENCES buildings(building_id) ON DELETE CASCADE,
                room_id BIGINT NOT NULL REFERENCES rooms(room_id) ON DELETE CASCADE,
                bed_number INT NOT NULL,
                CONSTRAINT chk_bed_range CHECK (bed_number BETWEEN 1 AND 4),
                CONSTRAINT uq_room_bed UNIQUE (room_id, bed_number)
            )
            """
    )),

    /**
     * MySQL 方言。
     * <p>
     * 表结构与 PostgreSQL 版本一致，仅在以下语法上做适配：
     * <ul>
     *   <li>自增主键使用 BIGINT AUTO_INCREMENT 替代 PostgreSQL 的 BIGSERIAL</li>
     *   <li>外键约束单独以 CONSTRAINT 子句声明，而非在列定义中直接写 REFERENCES</li>
     * </ul>
     */
    MYSQL(List.of(
            """
            CREATE TABLE IF NOT EXISTS buildings (
                building_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                building_code VARCHAR(50) NOT NULL UNIQUE,
                building_name VARCHAR(100) NOT NULL,
                gender_policy VARCHAR(20) NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS rooms (
                room_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                room_number VARCHAR(50) NOT NULL,
                building_id BIGINT NOT NULL,
                floor_number INT NOT NULL,
                CONSTRAINT fk_room_building FOREIGN KEY (building_id) REFERENCES buildings(building_id) ON DELETE CASCADE,
                CONSTRAINT uq_room_per_building UNIQUE (building_id, room_number)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS students (
                student_id VARCHAR(50) PRIMARY KEY,
                student_name VARCHAR(100) NOT NULL,
                class_name VARCHAR(100) NOT NULL,
                grade VARCHAR(20) NOT NULL,
                gender VARCHAR(20) NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS dorm_assignments (
                assignment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                student_id VARCHAR(50) NOT NULL UNIQUE,
                building_id BIGINT NOT NULL,
                room_id BIGINT NOT NULL,
                bed_number INT NOT NULL,
                CONSTRAINT fk_assignment_student FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
                CONSTRAINT fk_assignment_building FOREIGN KEY (building_id) REFERENCES buildings(building_id) ON DELETE CASCADE,
                CONSTRAINT fk_assignment_room FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
                CONSTRAINT chk_bed_range CHECK (bed_number BETWEEN 1 AND 4),
                CONSTRAINT uq_room_bed UNIQUE (room_id, bed_number)
            )
            """
    ));

    /*
    POSTGRESQL 是一个 DatabaseDialect 枚举常量。当它被创建时，
    List.of(...) 返回的 List<String> 被传入构造器，
    构造器里 this.schemaStatements = schemaStatements 把这个列表存进了枚举常量的内部字段。
    所以 POSTGRESQL 内部"藏"着一个包含 4 段 SQL 字符串的列表，
    通过 .schemaStatements() 方法可以取出来使用。
     */

    private final List<String> schemaStatements;
    DatabaseDialect(List<String> schemaStatements) { //构造函数
        this.schemaStatements = schemaStatements;
    }

    // return 该方言的建表 SQL 语句列表 
    public List<String> schemaStatements() {
        return schemaStatements;
    }

    /**
     * 根据 DSN 字符串自动识别数据库类型，返回对应的方言枚举。
     * <p>
     * 判断依据：DSN 是否以 {@code jdbc:postgresql:} 或 {@code jdbc:mysql:} 开头。
     *
     * @param dsn 数据库连接字符串（如 {@code jdbc:postgresql://localhost:5432/dorm}）
     * @return 对应的 {@link DatabaseDialect}
     * @throws IllegalArgumentException 遇到不支持的 DSN 类型时抛出
     */
    public static DatabaseDialect fromDsn(String dsn) {
        String normalized = dsn.toLowerCase(); //用于将字符串中的所有英文字母转成小写，是String类中封装好的方法
        if (normalized.startsWith("jdbc:postgresql:")) { //Startw大小写敏感
            return POSTGRESQL;
        }
        // st是JAVA库封装好的方法，逐个字符比对，检查字符串的开头是否和 () 里的内容完全一致
        if (normalized.startsWith("jdbc:mysql:")) {
            return MYSQL;
        }
        throw new IllegalArgumentException("暂不支持的 DSN: " + dsn);
    }
}
