package com.kec.academic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StudentDAO {

    public void addStudent(
            String roll,
            String name,
            String department,
            int semester) throws SQLException {

        if (semester < 1 || semester > 8) {
            throw new IllegalArgumentException(
                "Semester must be between 1 and 8."
            );
        }

        String sql =
            "INSERT INTO students " +
            "(roll_no, student_name, department, current_semester) " +
            "VALUES (?, ?, ?, ?)";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, roll.toUpperCase());
            ps.setString(2, name);
            ps.setString(3, department.toUpperCase());
            ps.setInt(4, semester);

            ps.executeUpdate();

            System.out.println("Student registered successfully!");
        }
    }

    public void displayStudents() throws SQLException {

        String sql = "SELECT * FROM students ORDER BY roll_no";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {

            System.out.println("\nSTUDENT DETAILS");

            while (rs.next()) {

                System.out.println(
                    "Roll Number: " + rs.getString("roll_no")
                );

                System.out.println(
                    "Name: " + rs.getString("student_name")
                );

                System.out.println(
                    "Department: " + rs.getString("department")
                );

                System.out.println(
                    "Semester: " + rs.getInt("current_semester")
                );

                System.out.println("-------------------------");
            }
        }
    }
}