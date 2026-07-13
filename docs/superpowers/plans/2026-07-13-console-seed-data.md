# Console Seed Data and Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add repeatable PostgreSQL/MySQL-compatible demo data and prove core console workflows against a disposable PostgreSQL database.

**Architecture:** `init.sql` remains a manual import file and identifies related rows with business keys rather than generated IDs. A PowerShell script creates a uniquely named test database, starts the application to create its schema, imports seed data twice, sends console input, checks the result, then drops the database in `finally`.

**Tech Stack:** Java 21, Maven 3.9.9, PostgreSQL 18 (`psql`), PowerShell.

## Global Constraints

- Do not import demo data automatically at application startup.
- Use SQL accepted by both PostgreSQL and MySQL 8.
- Do not write generated primary-key literals into seed SQL.
- Never stage `.env`, `target/`, or `.claude/`.
- Never leave the temporary test database behind.

---

### Task 1: Repeatable Demo Data

**Files:**

- Modify: `src/main/resources/sql/init.sql`
- Test: disposable PostgreSQL database via `psql`

**Interfaces:**

- Consumes: `buildings`, `rooms`, `students`, and `dorm_assignments` created by `DatabaseDialect`.
- Produces: buildings `M1`, `F1`, `X1`; rooms `M1-101`, `M1-201`, `F1-101`, `X1-301`; students `20240001` through `20240006`; four assignments.

- [ ] **Step 1: Establish the failing data check**

  Run `psql -h 127.0.0.1 -U postgres -d <temporary_database> -tAc "select count(*) from students;"` after schema creation.

  Expected: `0`, because the current script is empty.

- [ ] **Step 2: Write portable idempotent SQL**

  Use this exact insertion pattern for every independent row. It works in PostgreSQL and MySQL and permits re-execution:

  ```sql
  INSERT INTO students (student_id, student_name, class_name, grade, gender)
  SELECT '20240001', '张伟', '软件工程1班', '2024', 'MALE'
  WHERE NOT EXISTS (
      SELECT 1 FROM students WHERE student_id = '20240001'
  );
  ```

  Seed the following values:

  ```text
  buildings: M1/松园一号楼/男, F1/兰园一号楼/女, X1/学苑综合楼/男女分层
  rooms: M1/101/1, M1/201/2, F1/101/1, X1/301/3
  students: 20240001/张伟/软件工程1班/2024/MALE
            20240002/李强/软件工程1班/2024/MALE
            20240003/王芳/计算机2班/2024/FEMALE
            20240004/陈静/计算机2班/2024/FEMALE
            20240005/赵磊/人工智能1班/2024/MALE
            20240006/周敏/人工智能1班/2024/FEMALE
  assignments: 20240001/M1/101/1, 20240002/M1/101/2,
               20240003/F1/101/1, 20240004/X1/301/1
  ```

  Insert rooms with `SELECT building_id FROM buildings WHERE building_code = '<code>'`; insert assignments with joins from building code and room number. Put `NOT EXISTS` checks on `(building_id, room_number)` for rooms and `student_id` for assignments.

- [ ] **Step 3: Verify the SQL is repeatable**

  Run the script twice and check counts:

  ```powershell
  psql -h 127.0.0.1 -U postgres -d <temporary_database> -f src/main/resources/sql/init.sql
  psql -h 127.0.0.1 -U postgres -d <temporary_database> -f src/main/resources/sql/init.sql
  psql -h 127.0.0.1 -U postgres -d <temporary_database> -tAc "select (select count(*) from buildings) || ',' || (select count(*) from rooms) || ',' || (select count(*) from students) || ',' || (select count(*) from dorm_assignments);"
  ```

  Expected: `3,4,6,4`.

- [ ] **Step 4: Commit the independently verified seed script**

  ```powershell
  git add src/main/resources/sql/init.sql
  git commit -m "feat: add repeatable demo seed data"
  ```

### Task 2: Disposable Console Workflow Check

**Files:**

- Create: `scripts/verify-console.ps1`
- Test: `scripts/verify-console.ps1`

**Interfaces:**

- Consumes: user-level `JAVA_HOME`, `MAVEN_HOME`, `psql`, and `src/main/resources/sql/init.sql`.
- Produces: process exit code `0` plus `Console verification passed.` only after all assertions pass.

- [ ] **Step 1: Create the failing test harness**

  Create a script whose first runnable version proves the check is active:

  ```powershell
  $ErrorActionPreference = 'Stop'
  throw 'Console verification has not been implemented'
  ```

- [ ] **Step 2: Confirm the test is red**

  Run `powershell -ExecutionPolicy Bypass -File scripts/verify-console.ps1`.

  Expected: exit code `1` and `Console verification has not been implemented`.

- [ ] **Step 3: Implement isolated setup and cleanup**

  Replace the failure with a script that assigns
  `$database = "java_homework_console_$([guid]::NewGuid().ToString('N'))"`,
  runs `CREATE DATABASE $database` through `psql` connected to `postgres`, and
  sets `DATABASE_DSN` to
  `jdbc:postgresql://127.0.0.1:5432/$database?user=postgres&password=123456`.
  In the `try` block, pipe `0` to `mvn -q exec:java` so the application creates
  the schema and exits, then invoke `psql -f src/main/resources/sql/init.sql`
  twice. In `finally`, connect `psql` to `postgres` and execute
  `DROP DATABASE IF EXISTS $database WITH (FORCE)`.

  Resolve `psql` from `$env:PG_BIN\psql.exe` when supplied, otherwise `G:\develop\PostgreSQL\bin\psql.exe`. Resolve `mvn.cmd` and `java.exe` using the user-level `MAVEN_HOME` and `JAVA_HOME`. Throw when any child process returns a non-zero exit code.

- [ ] **Step 4: Test navigation and seeded lookup paths**

  Feed this exact input to `mvn -q exec:java` after importing `init.sql` twice:

  ```text
  abc
  2
  2

  0
  1
  3
  20240005

  0
  4
  3
  <M1-101 room id>

  0
  0
  ```

  Resolve `<M1-101 room id>` by `psql -tAc "select r.room_id from rooms r join buildings b on b.building_id=r.building_id where b.building_code='M1' and r.room_number='101';"`. Assert the output contains `请输入整数。`, `楼号=M1`, `该学生暂未分配宿舍。`, `学号=20240001`, and `已退出系统。`.

- [ ] **Step 5: Test four rejected assignment cases**

  Use database queries to resolve the M1, F1, M1-101, and F1-101 IDs. Append inputs for these cases and assert the exact messages:

  ```text
  20240001 + M1 + M1-101 + bed 3 => 该学生已分配宿舍，请使用调宿功能
  20240005 + M1 + M1-101 + bed 1 => 该床位已被占用
  20240006 + M1 + M1-101 + bed 4 => 该宿舍楼不允许当前学生性别入住
  20240005 + M1 + F1-101 + bed 3 => 房间不属于该宿舍楼
  ```

  Put an empty input after each message for `waitForEnter`, then return to the previous menu. The test must also query counts and require `3,4,6,4`, proving rejected commands did not alter seed rows.

- [ ] **Step 6: Make the test green or fix only a reproduced behavior defect**

  Run `powershell -ExecutionPolicy Bypass -File scripts/verify-console.ps1`.

  If an assertion fails, add the smallest correction in the owning class: menu-input behavior in `src/main/java/ui/ConsoleUtils.java`; domain validation in `src/main/java/service/DormService.java`. Re-run until the script returns `0`; do not change public prompts or service signatures.

- [ ] **Step 7: Commit the test and any source correction**

  ```powershell
  git add scripts/verify-console.ps1 src/main/java/ui/ConsoleUtils.java src/main/java/service/DormService.java
  git commit -m "test: verify seeded console workflows"
  ```

  Stage only Java files actually changed.

### Task 3: Document Manual Data Import

**Files:**

- Modify: `README.md`
- Test: import the script twice with the documented PostgreSQL command

**Interfaces:**

- Consumes: `src/main/resources/sql/init.sql`.
- Produces: commands that make clear seed data is manually imported and repeatable.

- [ ] **Step 1: Add the documentation section**

  Add this Markdown after the Maven quick-start section:

  ```markdown
  ## 演示数据

  程序首次启动会自动建表，但不会自动导入演示数据。建表后可手动执行 `src/main/resources/sql/init.sql`；脚本可重复执行，不会重复插入相同的演示记录。

  PostgreSQL：

  ```powershell
  psql -h 127.0.0.1 -U postgres -d JAVA -f src/main/resources/sql/init.sql
  ```

  MySQL：

  ```bash
  mysql -h 127.0.0.1 -u root -p JAVA < src/main/resources/sql/init.sql
  ```
  ```

- [ ] **Step 2: Verify the documented PostgreSQL command**

  Execute it twice on the disposable database and confirm `3,4,6,4` with the Task 1 count query.

- [ ] **Step 3: Commit the documentation**

  ```powershell
  git add README.md
  git commit -m "docs: explain manual demo data import"
  ```

### Task 4: Final Verification

**Files:**

- Verify: `src/main/resources/sql/init.sql`, `scripts/verify-console.ps1`, `README.md`

- [ ] **Step 1: Run the full console verification**

  ```powershell
  powershell -ExecutionPolicy Bypass -File scripts/verify-console.ps1
  ```

  Expected: `Console verification passed.` and exit code `0`.

- [ ] **Step 2: Verify the project build**

  ```powershell
  mvn test
  ```

  Expected: `BUILD SUCCESS`; the repository currently has no Java test sources, so Maven reports `No tests to run`.

- [ ] **Step 3: Inspect the final scope**

  ```powershell
  git diff --check origin/taqminum-branch...HEAD
  git status -sb
  ```

  Expected: no whitespace errors; unrelated existing `target/` changes and `.claude/` remain unstaged.
