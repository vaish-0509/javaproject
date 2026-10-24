package com.kec.academic;

/**
 * Pure grading logic (no database access), so it can be tested on its own.
 *
 * Rules used:
 *  - CIA = average of the CIA components that have been entered (out of 100).
 *  - Theory 40 CIA + 60 ESE, Practical 60 + 40, Embedded 50 + 50.
 *  - ESE must be at least 45 (out of 100) and the overall total at least 50 to pass.
 *  - Attendance below 65%            -> SA (shortage of attendance)
 *  - Attendance 65% to below 75%     -> PENDING unless medical condonation is approved
 *  - ESE absent                      -> AB
 *  - Otherwise grade from total: O(90+) A+(80+) A(70+) B+(60+) B(50+), else RA.
 */
public class GradeCalculator {

    public static final double ESE_MIN_MARK = 45.0;
    public static final double OVERALL_MIN = 50.0;

    public static class Result {
        public final double cia;          // CIA average out of 100
        public final double total;        // weighted total out of 100
        public final String grade;
        public final int gradePoint;
        public final boolean counted;     // true when the grade is final (counts for GPA)

        Result(double cia, double total, String grade, int gradePoint, boolean counted) {
            this.cia = cia;
            this.total = total;
            this.grade = grade;
            this.gradePoint = gradePoint;
            this.counted = counted;
        }
    }

    public static double ciaAverage(Double cia1, Double cia2, Double assignment,
                                    Double quiz, Double lab) {
        double sum = 0;
        int count = 0;
        for (Double d : new Double[] {cia1, cia2, assignment, quiz, lab}) {
            if (d != null) {
                sum += d;
                count++;
            }
        }
        return count == 0 ? 0.0 : sum / count;
    }

    public static double weightedTotal(double cia, double ese, String courseType) {
        switch (courseType.toUpperCase()) {
            case "THEORY":
                return cia * 0.40 + ese * 0.60;
            case "PRACTICAL":
                return cia * 0.60 + ese * 0.40;
            case "EMBEDDED":
                return cia * 0.50 + ese * 0.50;
            default:
                throw new IllegalArgumentException("Invalid course type: " + courseType);
        }
    }

    public static Result evaluate(Double cia1, Double cia2, Double assignment,
                                  Double quiz, Double lab, Double ese,
                                  String courseType, double attendancePercentage,
                                  boolean medicalCondonationApproved, boolean eseAbsent) {

        boolean anyCia = cia1 != null || cia2 != null || assignment != null
                || quiz != null || lab != null;
        double cia = ciaAverage(cia1, cia2, assignment, quiz, lab);
        double eseMark = (ese == null) ? 0.0 : ese;
        double total = weightedTotal(cia, eseMark, courseType);

        // Attendance rules come first
        if (attendancePercentage < 65) {
            return new Result(cia, total, "SA", 0, true);
        }
        if (attendancePercentage < 75 && !medicalCondonationApproved) {
            return new Result(cia, total, "PENDING", 0, false);
        }
        if (eseAbsent) {
            return new Result(cia, total, "AB", 0, true);
        }
        if (!anyCia || ese == null) {
            return new Result(cia, total, "INCOMPLETE", 0, false);
        }

        boolean passed = eseMark >= ESE_MIN_MARK && total >= OVERALL_MIN;
        if (!passed) {
            return new Result(cia, total, "RA", 0, true);
        }

        if (total >= 90) return new Result(cia, total, "O", 10, true);
        if (total >= 80) return new Result(cia, total, "A+", 9, true);
        if (total >= 70) return new Result(cia, total, "A", 8, true);
        if (total >= 60) return new Result(cia, total, "B+", 7, true);
        return new Result(cia, total, "B", 6, true);
    }
}