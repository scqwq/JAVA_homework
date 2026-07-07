package database;

import java.util.List;

public enum DatabaseDialect {
    POSTGRESQL(List.of(
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

    private final List<String> schemaStatements;

    DatabaseDialect(List<String> schemaStatements) {
        this.schemaStatements = schemaStatements;
    }

    public List<String> schemaStatements() {
        return schemaStatements;
    }

    public static DatabaseDialect fromDsn(String dsn) {
        String normalized = dsn.toLowerCase();
        if (normalized.startsWith("jdbc:postgresql:")) {
            return POSTGRESQL;
        }
        if (normalized.startsWith("jdbc:mysql:")) {
            return MYSQL;
        }
        throw new IllegalArgumentException("暂不支持的 DSN: " + dsn);
    }
}

