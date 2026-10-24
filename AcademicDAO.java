package com.kec.academic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AcademicDAO {

    // One enrollment row joined with course, student and marks
    private static class Record {
        String roll, studentName, courseCode, courseTitle, courseType;
        double credits;
        int semester, classesHeld, classesAttended;
        boolean condonation, eseAbsent;
        Double cia1, cia2, assignment, quiz, lab, ese;

        double attendancePercentage() {
            return classesHeld == 0 ? 100.0 : (classesAttended * 100.0) / classesHeld;
        }

        GradeCalculator.Result result() {
            return GradeCalculator.evaluate(cia1, cia2, assignment, quiz, lab, ese,
                    courseType, attendancePercentage(), condonation, eseAbsent);
        }
    }

    private static final String RECORD_SELECT =
            "SELECT e.roll_no, s.student_name, e.course_code, c.course_title, "
            + "c.credits, c.course_type, e.semester, e.classes_held, "
            + "e.classes_attended, e.medical_condonation_approved, "
            + "m.cia1, m.cia2, m.assignment_mark, m.quiz_mark, m.lab_mark, "
            + "m.ese_mark, COALESCE(m.ese_absent, FALSE) AS ese_absent "
            + "FROM enrollments e "
            + "JOIN students s ON s.roll_no = e.roll_no "
            + "JOIN courses c ON c.course_code = e.course_code "
            + "LEFT JOIN assessment_marks m ON m.enrollment_id = e.enrollment_id ";

    // ---------------------------------------------------------------- helpers

    private static Double getNullableDouble(ResultSet rs, String column) throws SQLException {
        double v = rs.getDouble(column);
        return rs.wasNull() ? null : v;
    }

    private static Record readRecord(ResultSet rs) throws SQLException {
        Record r = new Record();
        r.roll = rs.getString("roll_no");
        r.studentName = rs.getString("student_name");
        r.courseCode = rs.getString("course_code");
        r.courseTitle = rs.getString("course_title");
        r.credits = rs.getDouble("credits");
        r.courseType = rs.getString("course_type");
        r.semester = rs.getInt("semester");
        r.classesHeld = rs.getInt("classes_held");
        r.classesAttended = rs.getInt("classes_attended");
        r.condonation = rs.getBoolean("medical_condonation_approved");
        r.cia1 = getNullableDouble(rs, "cia1");
        r.cia2 = getNullableDouble(rs, "cia2");
        r.assignment = getNullableDouble(rs, "assignment_mark");
        r.quiz = getNullableDouble(rs, "quiz_mark");
        r.lab = getNullableDouble(rs, "lab_mark");
        r.ese = getNullableDouble(rs, "ese_mark");
        r.eseAbsent = rs.getBoolean("ese_absent");
        return r;
    }

    private static void checkMark(String label, Double mark) {
        if (mark != null && (mark < 0 || mark > 100)) {
            throw new IllegalArgumentException(label + " must be between 0 and 100.");
        }
    }

    private static void setNullableDouble(PreparedStatement ps, int index, Double v)
            throws SQLException {
        if (v == null) {
            ps.setNull(index, Types.DECIMAL);
        } else {
            ps.setDouble(index, v);
        }
    }

    /** Returns the enrollment id, or throws if the student is not enrolled. */
    private static long findEnrollmentId(Connection con, String roll, String courseCode,
                                         int semester) throws SQLException {
        String sql = "SELECT enrollment_id FROM enrollments "
                + "WHERE roll_no = ? AND course_code = ? AND semester = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, roll.trim().toUpperCase());
            ps.setString(2, courseCode.trim().toUpperCase());
            ps.setInt(3, semester);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new IllegalArgumentException("Student " + roll.toUpperCase()
                + " is not enrolled in " + courseCode.toUpperCase()
                + " for semester " + semester + ".");
    }

    private static void checkSemester(int semester) {
        if (semester < 1 || semester > 8) {
            throw new IllegalArgumentException("Semester must be between 1 and 8.");
        }
    }

    // ---------------------------------------------------------------- courses

    public void addCourse(String code, String title, double credits, String courseType)
            throws SQLException {

        if (code == null || code.trim().isEmpty() || title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Course code and title are required.");
        }
        if (credits <= 0) {
            throw new IllegalArgumentException("Credits must be greater than 0.");
        }
        String type = courseType.trim().toUpperCase();
        if (!type.equals("THEORY") && !type.equals("PRACTICAL") && !type.equals("EMBEDDED")) {
            throw new IllegalArgumentException(
                    "Course type must be THEORY, PRACTICAL or EMBEDDED.");
        }

        String sql = "INSERT INTO courses (course_code, course_title, credits, course_type) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, code.trim().toUpperCase());
            ps.setString(2, title.trim());
            ps.setDouble(3, credits);
            ps.setString(4, type);
            ps.executeUpdate();
            System.out.println("Course added successfully!");
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            throw new SQLException("Course " + code.toUpperCase() + " already exists.", e);
        }
    }

    // ------------------------------------------------------------ enrollment

    public void enrollStudent(String roll, String courseCode, int semester)
            throws SQLException {

        checkSemester(semester);
        String sql = "INSERT INTO enrollments (roll_no, course_code, semester) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, roll.trim().toUpperCase());
            ps.setString(2, courseCode.trim().toUpperCase());
            ps.setInt(3, semester);
            ps.executeUpdate();
            System.out.println("Student enrolled successfully!");
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            throw new SQLException("Could not enroll: student or course does not exist, "
                    + "or the student is already enrolled in this course for the semester.", e);
        }
    }

    // ------------------------------------------------------------ attendance

    public void updateAttendance(String roll, String courseCode, int semester,
                                 int classesHeld, int classesAttended,
                                 boolean medicalCondonationApproved) throws SQLException {

        checkSemester(semester);
        if (classesHeld < 0 || classesAttended < 0) {
            throw new IllegalArgumentException("Class counts cannot be negative.");
        }
        if (classesAttended > classesHeld) {
            throw new IllegalArgumentException("Classes attended cannot exceed classes held.");
        }

        String sql = "UPDATE enrollments SET classes_held = ?, classes_attended = ?, "
                + "medical_condonation_approved = ? WHERE enrollment_id = ?";
        try (Connection con = DBConnection.getConnection()) {
            long id = findEnrollmentId(con, roll, courseCode, semester);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, classesHeld);
                ps.setInt(2, classesAttended);
                ps.setBoolean(3, medicalCondonationApproved);
                ps.setLong(4, id);
                ps.executeUpdate();
            }
            double pct = classesHeld == 0 ? 100.0 : classesAttended * 100.0 / classesHeld;
            System.out.printf("Attendance updated: %.2f%%%n", pct);
        }
    }

    // ----------------------------------------------------------------- marks

    public void saveCiaMarks(String roll, String courseCode, int semester,
                             Double cia1, Double cia2, Double assignment,
                             Double quiz, Double lab) throws SQLException {

        checkSemester(semester);
        checkMark("CIA 1", cia1);
        checkMark("CIA 2", cia2);
        checkMark("Assignment", assignment);
        checkMark("Quiz", quiz);
        checkMark("Lab", lab);

        String sql = "INSERT INTO assessment_marks "
                + "(enrollment_id, cia1, cia2, assignment_mark, quiz_mark, lab_mark) "
                + "VALUES (?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE cia1 = VALUES(cia1), cia2 = VALUES(cia2), "
                + "assignment_mark = VALUES(assignment_mark), quiz_mark = VALUES(quiz_mark), "
                + "lab_mark = VALUES(lab_mark)";

        try (Connection con = DBConnection.getConnection()) {
            long id = findEnrollmentId(con, roll, courseCode, semester);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setLong(1, id);
                setNullableDouble(ps, 2, cia1);
                setNullableDouble(ps, 3, cia2);
                setNullableDouble(ps, 4, assignment);
                setNullableDouble(ps, 5, quiz);
                setNullableDouble(ps, 6, lab);
                ps.executeUpdate();
            }
            System.out.println("CIA marks saved.");
        }
    }

    public void saveEseMarks(String roll, String courseCode, int semester,
                             Double ese, boolean absent) throws SQLException {

        checkSemester(semester);
        if (absent) {
            ese = null;
        } else if (ese == null) {
            throw new IllegalArgumentException("ESE mark is required unless the student is absent.");
        }
        checkMark("ESE mark", ese);

        String sql = "INSERT INTO assessment_marks (enrollment_id, ese_mark, ese_absent) "
                + "VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE ese_mark = VALUES(ese_mark), "
                + "ese_absent = VALUES(ese_absent)";

        try (Connection con = DBConnection.getConnection()) {
            long id = findEnrollmentId(con, roll, courseCode, semester);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setLong(1, id);
                setNullableDouble(ps, 2, ese);
                ps.setBoolean(3, absent);
                ps.executeUpdate();
            }
            System.out.println("ESE marks saved.");
        }
    }

    // ------------------------------------------------------------- reports

    public void generateMarkSheet(String roll, int semester) throws SQLException {

        checkSemester(semester);
        String sql = RECORD_SELECT
                + "WHERE e.roll_no = ? AND e.semester = ? ORDER BY e.course_code";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, roll.trim().toUpperCase());
            ps.setInt(2, semester);

            try (ResultSet rs = ps.executeQuery()) {
                List<Record> records = new ArrayList<>();
                while (rs.next()) {
                    records.add(readRecord(rs));
                }

                if (records.isEmpty()) {
                    System.out.println("No enrollments found for " + roll.toUpperCase()
                            + " in semester " + semester + ".");
                    return;
                }

                System.out.println("\n================ MARK SHEET ================");
                System.out.println("Roll No : " + records.get(0).roll);
                System.out.println("Name    : " + records.get(0).studentName);
                System.out.println("Semester: " + semester);
                System.out.println("--------------------------------------------");
                System.out.printf("%-10s %-24s %4s %6s %6s %7s %5s %3s%n",
                        "Course", "Title", "Cr", "CIA", "Total", "Att%", "Grade", "GP");

                double creditSum = 0;
                double pointSum = 0;
                boolean pendingItems = false;

                for (Record r : records) {
                    GradeCalculator.Result res = r.result();
                    String title = r.courseTitle.length() > 24
                            ? r.courseTitle.substring(0, 24) : r.courseTitle;
                    System.out.printf("%-10s %-24s %4.1f %6.2f %6.2f %6.2f%% %5s %3d%n",
                            r.courseCode, title, r.credits, res.cia, res.total,
                            r.attendancePercentage(), res.grade, res.gradePoint);

                    if (res.counted) {
                        creditSum += r.credits;
                        pointSum += r.credits * res.gradePoint;
                    } else {
                        pendingItems = true;
                    }
                }

                System.out.println("--------------------------------------------");
                if (creditSum > 0) {
                    System.out.printf("GPA: %.2f%n", pointSum / creditSum);
                } else {
                    System.out.println("GPA: not available yet");
                }
                if (pendingItems) {
                    System.out.println("Note: PENDING / INCOMPLETE courses are not in the GPA.");
                }
                System.out.println("============================================");
            }
        }
    }

    public void attendanceDefaulterReport(int semester) throws SQLException {

        checkSemester(semester);
        String sql = RECORD_SELECT + "WHERE e.semester = ? "
                + "AND e.classes_held > 0 "
                + "AND (e.classes_attended * 100.0 / e.classes_held) < 75 "
                + "ORDER BY e.roll_no, e.course_code";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, semester);

            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n=== ATTENDANCE DEFAULTERS (below 75%), Semester "
                        + semester + " ===");
                System.out.printf("%-14s %-22s %-10s %7s %-12s%n",
                        "Roll No", "Name", "Course", "Att%", "Status");

                boolean any = false;
                while (rs.next()) {
                    any = true;
                    Record r = readRecord(rs);
                    double pct = r.attendancePercentage();
                    String status;
                    if (pct < 65) {
                        status = "SA";
                    } else if (r.condonation) {
                        status = "CONDONED";
                    } else {
                        status = "PENDING";
                    }
                    String name = r.studentName.length() > 22
                            ? r.studentName.substring(0, 22) : r.studentName;
                    System.out.printf("%-14s %-22s %-10s %6.2f%% %-12s%n",
                            r.roll, name, r.courseCode, pct, status);
                }
                if (!any) {
                    System.out.println("No defaulters found.");
                }
            }
        }
    }

    public void classPerformanceSummary(String courseCode, int semester) throws SQLException {

        checkSemester(semester);
        String sql = RECORD_SELECT
                + "WHERE e.course_code = ? AND e.semester = ? ORDER BY e.roll_no";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, courseCode.trim().toUpperCase());
            ps.setInt(2, semester);

            try (ResultSet rs = ps.executeQuery()) {
                List<Record> records = new ArrayList<>();
                while (rs.next()) {
                    records.add(readRecord(rs));
                }

                if (records.isEmpty()) {
                    System.out.println("No students enrolled in " + courseCode.toUpperCase()
                            + " for semester " + semester + ".");
                    return;
                }

                Map<String, Integer> distribution = new LinkedHashMap<>();
                for (String g : new String[] {"O", "A+", "A", "B+", "B", "RA", "SA", "AB",
                        "PENDING", "INCOMPLETE"}) {
                    distribution.put(g, 0);
                }

                int passed = 0;
                int evaluated = 0;   // students with a final result
                double totalSum = 0;
                double highest = Double.NEGATIVE_INFINITY;
                double lowest = Double.POSITIVE_INFINITY;

                for (Record r : records) {
                    GradeCalculator.Result res = r.result();
                    distribution.merge(res.grade, 1, Integer::sum);

                    if (res.counted) {
                        evaluated++;
                        if (res.gradePoint > 0) {
                            passed++;
                        }
                    }
                    boolean marksComplete = !res.grade.equals("INCOMPLETE")
                            && !res.grade.equals("AB");
                    if (marksComplete && !res.grade.equals("PENDING") && !res.grade.equals("SA")) {
                        totalSum += res.total;
                        highest = Math.max(highest, res.total);
                        lowest = Math.min(lowest, res.total);
                    }
                }

                int scored = 0;
                for (String g : new String[] {"O", "A+", "A", "B+", "B", "RA"}) {
                    scored += distribution.get(g);
                }

                Record first = records.get(0);
                System.out.println("\n===== CLASS PERFORMANCE SUMMARY =====");
                System.out.println("Course  : " + first.courseCode + " - " + first.courseTitle);
                System.out.println("Semester: " + semester);
                System.out.println("Enrolled: " + records.size());
                System.out.println("Final results: " + evaluated);
                System.out.println("Passed  : " + passed);
                if (evaluated > 0) {
                    System.out.printf("Pass %%  : %.2f%%%n", passed * 100.0 / evaluated);
                }
                if (scored > 0) {
                    System.out.printf("Average : %.2f%n", totalSum / scored);
                    System.out.printf("Highest : %.2f%n", highest);
                    System.out.printf("Lowest  : %.2f%n", lowest);
                }
                System.out.println("Grade distribution:");
                for (Map.Entry<String, Integer> e : distribution.entrySet()) {
                    if (e.getValue() > 0) {
                        System.out.println("  " + e.getKey() + " : " + e.getValue());
                    }
                }
                System.out.println("=====================================");
            }
        }
    }
}