CREATE DATABASE IF NOT EXISTS kec_academic;

USE kec_academic;

-- 1. Student Details
CREATE TABLE IF NOT EXISTS students (
    roll_no VARCHAR(20) PRIMARY KEY,
    student_name VARCHAR(100) NOT NULL,
    department VARCHAR(20) NOT NULL,
    current_semester TINYINT UNSIGNED NOT NULL,
    CHECK (current_semester BETWEEN 1 AND 8)
);

-- 2. Course Details
CREATE TABLE IF NOT EXISTS courses (
    course_code VARCHAR(20) PRIMARY KEY,
    course_title VARCHAR(150) NOT NULL,
    credits DECIMAL(4,1) NOT NULL,
    course_type ENUM(
        'THEORY',
        'PRACTICAL',
        'EMBEDDED'
    ) NOT NULL,
    CHECK (credits > 0)
);

-- 3. Student Course Enrollment and Attendance
CREATE TABLE IF NOT EXISTS enrollments (
    enrollment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    roll_no VARCHAR(20) NOT NULL,
    course_code VARCHAR(20) NOT NULL,
    semester TINYINT UNSIGNED NOT NULL,
    classes_held INT UNSIGNED NOT NULL DEFAULT 0,
    classes_attended INT UNSIGNED NOT NULL DEFAULT 0,
    medical_condonation_approved BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_student
        FOREIGN KEY (roll_no)
        REFERENCES students(roll_no),

    CONSTRAINT fk_course
        FOREIGN KEY (course_code)
        REFERENCES courses(course_code),

    CONSTRAINT unique_enrollment
        UNIQUE (roll_no, course_code, semester),

    CHECK (semester BETWEEN 1 AND 8),
    CHECK (classes_attended <= classes_held)
);

-- 4. Assessment Marks
CREATE TABLE IF NOT EXISTS assessment_marks (
    assessment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id BIGINT NOT NULL UNIQUE,

    cia1 DECIMAL(5,2),
    cia2 DECIMAL(5,2),
    assignment_mark DECIMAL(5,2),
    quiz_mark DECIMAL(5,2),
    lab_mark DECIMAL(5,2),
    ese_mark DECIMAL(5,2),

    ese_absent BOOLEAN NOT NULL DEFAULT FALSE,

    FOREIGN KEY (enrollment_id)
        REFERENCES enrollments(enrollment_id)
        ON DELETE CASCADE,

    CHECK (cia1 IS NULL OR cia1 BETWEEN 0 AND 100),
    CHECK (cia2 IS NULL OR cia2 BETWEEN 0 AND 100),
    CHECK (assignment_mark IS NULL OR assignment_mark BETWEEN 0 AND 100),
    CHECK (quiz_mark IS NULL OR quiz_mark BETWEEN 0 AND 100),
    CHECK (lab_mark IS NULL OR lab_mark BETWEEN 0 AND 100),
    CHECK (ese_mark IS NULL OR ese_mark BETWEEN 0 AND 100)
);

SHOW TABLES;