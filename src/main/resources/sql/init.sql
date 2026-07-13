-- Manual demo data for PostgreSQL and MySQL.
-- This script is repeatable: existing rows are kept and not duplicated.

INSERT INTO buildings (building_code, building_name, gender_policy)
SELECT 'M1', '松园一号楼', 'MALE_ONLY'
WHERE NOT EXISTS (
    SELECT 1 FROM buildings WHERE building_code = 'M1'
);

INSERT INTO buildings (building_code, building_name, gender_policy)
SELECT 'F1', '兰园一号楼', 'FEMALE_ONLY'
WHERE NOT EXISTS (
    SELECT 1 FROM buildings WHERE building_code = 'F1'
);

INSERT INTO buildings (building_code, building_name, gender_policy)
SELECT 'X1', '学苑综合楼', 'MIXED_BY_FLOOR'
WHERE NOT EXISTS (
    SELECT 1 FROM buildings WHERE building_code = 'X1'
);

INSERT INTO rooms (room_number, building_id, floor_number)
SELECT '101', b.building_id, 1
FROM buildings b
WHERE b.building_code = 'M1'
  AND NOT EXISTS (
      SELECT 1 FROM rooms r
      WHERE r.building_id = b.building_id AND r.room_number = '101'
  );

INSERT INTO rooms (room_number, building_id, floor_number)
SELECT '201', b.building_id, 2
FROM buildings b
WHERE b.building_code = 'M1'
  AND NOT EXISTS (
      SELECT 1 FROM rooms r
      WHERE r.building_id = b.building_id AND r.room_number = '201'
  );

INSERT INTO rooms (room_number, building_id, floor_number)
SELECT '101', b.building_id, 1
FROM buildings b
WHERE b.building_code = 'F1'
  AND NOT EXISTS (
      SELECT 1 FROM rooms r
      WHERE r.building_id = b.building_id AND r.room_number = '101'
  );

INSERT INTO rooms (room_number, building_id, floor_number)
SELECT '301', b.building_id, 3
FROM buildings b
WHERE b.building_code = 'X1'
  AND NOT EXISTS (
      SELECT 1 FROM rooms r
      WHERE r.building_id = b.building_id AND r.room_number = '301'
  );

INSERT INTO students (student_id, student_name, class_name, grade, gender)
SELECT '20240001', '张伟', '软件工程1班', '2024', 'MALE'
WHERE NOT EXISTS (SELECT 1 FROM students WHERE student_id = '20240001');

INSERT INTO students (student_id, student_name, class_name, grade, gender)
SELECT '20240002', '李强', '软件工程1班', '2024', 'MALE'
WHERE NOT EXISTS (SELECT 1 FROM students WHERE student_id = '20240002');

INSERT INTO students (student_id, student_name, class_name, grade, gender)
SELECT '20240003', '王芳', '计算机2班', '2024', 'FEMALE'
WHERE NOT EXISTS (SELECT 1 FROM students WHERE student_id = '20240003');

INSERT INTO students (student_id, student_name, class_name, grade, gender)
SELECT '20240004', '陈静', '计算机2班', '2024', 'FEMALE'
WHERE NOT EXISTS (SELECT 1 FROM students WHERE student_id = '20240004');

INSERT INTO students (student_id, student_name, class_name, grade, gender)
SELECT '20240005', '赵磊', '人工智能1班', '2024', 'MALE'
WHERE NOT EXISTS (SELECT 1 FROM students WHERE student_id = '20240005');

INSERT INTO students (student_id, student_name, class_name, grade, gender)
SELECT '20240006', '周敏', '人工智能1班', '2024', 'FEMALE'
WHERE NOT EXISTS (SELECT 1 FROM students WHERE student_id = '20240006');

INSERT INTO dorm_assignments (student_id, building_id, room_id, bed_number)
SELECT '20240001', b.building_id, r.room_id, 1
FROM buildings b
JOIN rooms r ON r.building_id = b.building_id
WHERE b.building_code = 'M1'
  AND r.room_number = '101'
  AND NOT EXISTS (
      SELECT 1 FROM dorm_assignments WHERE student_id = '20240001'
  );

INSERT INTO dorm_assignments (student_id, building_id, room_id, bed_number)
SELECT '20240002', b.building_id, r.room_id, 2
FROM buildings b
JOIN rooms r ON r.building_id = b.building_id
WHERE b.building_code = 'M1'
  AND r.room_number = '101'
  AND NOT EXISTS (
      SELECT 1 FROM dorm_assignments WHERE student_id = '20240002'
  );

INSERT INTO dorm_assignments (student_id, building_id, room_id, bed_number)
SELECT '20240003', b.building_id, r.room_id, 1
FROM buildings b
JOIN rooms r ON r.building_id = b.building_id
WHERE b.building_code = 'F1'
  AND r.room_number = '101'
  AND NOT EXISTS (
      SELECT 1 FROM dorm_assignments WHERE student_id = '20240003'
  );

INSERT INTO dorm_assignments (student_id, building_id, room_id, bed_number)
SELECT '20240004', b.building_id, r.room_id, 1
FROM buildings b
JOIN rooms r ON r.building_id = b.building_id
WHERE b.building_code = 'X1'
  AND r.room_number = '301'
  AND NOT EXISTS (
      SELECT 1 FROM dorm_assignments WHERE student_id = '20240004'
  );
