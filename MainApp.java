package com.kec.academic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

public class MainApp {

    static Scanner sc = new Scanner(System.in);

    static StudentDAO studentDAO = new StudentDAO();
    static AcademicDAO academicDAO = new AcademicDAO();

    public static void main(String[] args) {

        try (Connection con = DBConnection.getConnection()) {
            System.out.println("KEC R2024 Academic Evaluation System");
        } catch (SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
            return;
        }

        int choice;

        do {
            System.out.println("\n========== MAIN MENU ==========");
            System.out.println("1. Register Student");
            System.out.println("2. Display Students");
            System.out.println("3. Add Course");
            System.out.println("4. Enroll Student");
            System.out.println("5. Update Attendance");
            System.out.println("6. Enter CIA Marks");
            System.out.println("7. Enter ESE Marks");
            System.out.println("8. Generate Mark Sheet");
            System.out.println("9. Attendance Defaulter Report");
            System.out.println("10. Class Performance Summary");
            System.out.println("0. Exit");

            choice = readInt("Enter your choice: ");

            try {
                switch (choice) {
                    case 1:
                        registerStudent();
                        break;
                    case 2:
                        studentDAO.displayStudents();
                        break;
                    case 3:
                        addCourse();
                        break;
                    case 4:
                        enrollStudent();
                        break;
                    case 5:
                        updateAttendance();
                        break;
                    case 6:
                        enterCiaMarks();
                        break;
                    case 7:
                        enterEseMarks();
                        break;
                    case 8:
                        generateMarkSheet();
                        break;
                    case 9:
                        academicDAO.attendanceDefaulterReport(readInt("Semester (1-8): "));
                        break;
                    case 10:
                        classSummary();
                        break;
                    case 0:
                        System.out.println("Application closed.");
                        break;
                    default:
                        System.out.println("Invalid choice.");
                }
            } catch (SQLException | IllegalArgumentException e) {
                System.out.println("Operation failed: " + e.getMessage());
            }

        } while (choice != 0);

        sc.close();
    }

    // ------------------------------------------------------- input helpers

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return sc.hasNextLine() ? sc.nextLine().trim() : "";
    }

    private static int readInt(String prompt) {
        while (true) {
            String line = readLine(prompt);
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            String line = readLine(prompt);
            try {
                return Double.parseDouble(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    /** Blank input means "not entered yet" and returns null. */
    private static Double readOptionalDouble(String prompt) {
        while (true) {
            String line = readLine(prompt + " (blank to skip): ");
            if (line.isEmpty()) {
                return null;
            }
            try {
                return Double.parseDouble(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number or leave blank.");
            }
        }
    }

    private static boolean readYesNo(String prompt) {
        while (true) {
            String line = readLine(prompt + " (y/n): ").toLowerCase();
            if (line.equals("y") || line.equals("yes")) {
                return true;
            }
            if (line.equals("n") || line.equals("no")) {
                return false;
            }
            System.out.println("Please answer y or n.");
        }
    }

    // ------------------------------------------------------------ actions

    private static void registerStudent() throws SQLException {
        String roll = readLine("Roll Number: ");
        String name = readLine("Student Name: ");
        String department = readLine("Department: ");
        int semester = readInt("Semester: ");
        studentDAO.addStudent(roll, name, department, semester);
    }

    private static void addCourse() throws SQLException {
        String code = readLine("Course Code: ");
        String title = readLine("Course Title: ");
        double credits = readDouble("Credits: ");
        String type = readLine("Course Type (THEORY / PRACTICAL / EMBEDDED): ");
        academicDAO.addCourse(code, title, credits, type);
    }

    private static void enrollStudent() throws SQLException {
        String roll = readLine("Roll Number: ");
        String code = readLine("Course Code: ");
        int semester = readInt("Semester: ");
        academicDAO.enrollStudent(roll, code, semester);
    }

    private static void updateAttendance() throws SQLException {
        String roll = readLine("Roll Number: ");
        String code = readLine("Course Code: ");
        int semester = readInt("Semester: ");
        int held = readInt("Classes held: ");
        int attended = readInt("Classes attended: ");
        boolean condonation = readYesNo("Medical condonation approved?");
        academicDAO.updateAttendance(roll, code, semester, held, attended, condonation);
    }

    private static void enterCiaMarks() throws SQLException {
        String roll = readLine("Roll Number: ");
        String code = readLine("Course Code: ");
        int semester = readInt("Semester: ");
        Double cia1 = readOptionalDouble("CIA 1 (0-100)");
        Double cia2 = readOptionalDouble("CIA 2 (0-100)");
        Double assignment = readOptionalDouble("Assignment (0-100)");
        Double quiz = readOptionalDouble("Quiz (0-100)");
        Double lab = readOptionalDouble("Lab (0-100)");
        academicDAO.saveCiaMarks(roll, code, semester, cia1, cia2, assignment, quiz, lab);
    }

    private static void enterEseMarks() throws SQLException {
        String roll = readLine("Roll Number: ");
        String code = readLine("Course Code: ");
        int semester = readInt("Semester: ");
        boolean absent = readYesNo("Was the student absent for ESE?");
        Double ese = absent ? null : Double.valueOf(readDouble("ESE mark (0-100): "));
        academicDAO.saveEseMarks(roll, code, semester, ese, absent);
    }

    private static void generateMarkSheet() throws SQLException {
        String roll = readLine("Roll Number: ");
        int semester = readInt("Semester: ");
        academicDAO.generateMarkSheet(roll, semester);
    }

    private static void classSummary() throws SQLException {
        String code = readLine("Course Code: ");
        int semester = readInt("Semester: ");
        academicDAO.classPerformanceSummary(code, semester);
    }
}