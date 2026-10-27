<<<<<<< HEAD
<<<<<<< HEAD
# KEC Academic Management System (Java JDBC + MariaDB)

## Features
- Student listing, registration, update and deletion
- Course listing and course creation
- Student course enrollment
- Attendance update, medical approval flag, attendance eligibility report
- CIA-1, CIA-2, assignment, quiz, lab and ESE marks entry/update
- Student semester mark sheet, grade and SGPA calculation
- Department-wise student/enrollment/attendance/marks summary
- Parameterized SQL, input validation, JDBC try-with-resources

## Requirements
- JDK 17 or newer (source uses switch expressions)
- MariaDB Server
- Included MariaDB Connector/J `lib/mariadb-java-client-3.5.11.jar`

## Setup
1. Start MariaDB.
2. Import `kec_schema.sql` using MariaDB client or a GUI such as HeidiSQL/phpMyAdmin:
   ```sql
   SOURCE D:/Java-JDPC-Academic-Management-System/kec_schema.sql;
   ```
   Or open the SQL file in your database tool and execute it.
   The schema uses `CREATE TABLE IF NOT EXISTS` and `INSERT IGNORE`; it does not drop tables.
3. Default connection is `jdbc:mariadb://localhost:3306/kec_academic_system`, user `root`, empty password.
   If your MariaDB credentials differ, set environment variables before running:
   ```powershell
   $env:KEC_DB_URL="jdbc:mariadb://localhost:3306/kec_academic_system"
   $env:KEC_DB_USER="root"
   $env:KEC_DB_PASSWORD="your_password"
   ```
4. Open PowerShell in the project folder and compile:
   ```powershell
   javac -cp "lib/*" -d out src\*.java
   ```
5. Run:
   ```powershell
   java -cp "out;lib/*" MainApp
   ```

## Recommended testing sequence
1. Run option 1: verify sample students.
2. Option 5: verify sample courses.
3. Option 10: verify attendance percentages and statuses.
4. Option 2: add a test student (use a unique roll number).
5. Option 3: update the test student.
6. Option 6: add a course for an existing department code.
7. Option 7: enroll the test student; note that enrollment ID is auto-generated.
8. Option 8: set held/attended counts; test 75%+, 65–74.99%, and below 65%.
9. Option 9: enter marks for the enrollment.
10. Option 11: display the student and semester mark sheet.
11. Option 12: verify department summary.
12. Option 4: delete the test student when finished.

## Important academic note
The assignment PDF specifies the features but does not provide a complete authoritative grade table or formula in the visible source excerpt. For demonstration, this implementation uses a sample formula:
- Internal score = average of CIA-1/CIA-2 weighted 40%, assignment 20%, quiz 10%, lab evaluation 30%.
- Course total = internal score weighted 40% + ESE weighted 60%.
- Grades: O >=90, A+ >=80, A >=70, B+ >=60, B >=50, C >=40, else F.
- Attendance: >=75% eligible; 65–74.99% requires medical approval; below 65% is SA/not eligible.
These are configurable demo assumptions, not a claim about official KEC regulations. Confirm the exact weights and grade boundaries with your department before final submission.
The program displays the enrollment ID in option 10; use it for attendance and marks entry.
=======
# Java-JDPC-Mini-Project
>>>>>>> 3e6c4a971620fc981c4a5420ee908d437460be55
=======
# Java-JDPC-Mini-Project
>>>>>>> 205e5406ff39975bc0981cca55dd3486291c938d
