package com.attendance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class DatabaseManager {
    private Connection conn;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/attendance_system?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "KiloTgun1979";

    public DatabaseManager() {
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (Exception e) {
            System.err.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean saveStudent(String studentId, String name, INDArray faceVector) {
        try {
            // First check if student exists
            PreparedStatement checkStmt = conn.prepareStatement("SELECT student_id FROM Students WHERE student_id = ?");
            checkStmt.setString(1, studentId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                System.out.println("Error: Student ID " + studentId + " already exists.");
                return false;
            }
            
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO Students (student_id, name, face_vector) VALUES (?, ?, ?)");
            stmt.setString(1, studentId);
            stmt.setString(2, name);
            stmt.setBytes(3, serializeVector(faceVector));
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("Error saving student: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public String matchFaceVector(INDArray inputVector, double threshold) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT student_id, face_vector FROM Students");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String studentId = rs.getString("student_id");
                byte[] vectorBytes = rs.getBytes("face_vector");
                INDArray storedVector = deserializeVector(vectorBytes);
                double similarity = new FaceRecognizer().calculateSimilarity(inputVector, storedVector);
                if (similarity >= threshold) {
                    return studentId;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveAttendance(String studentId, String classId, LocalDate date, String status) {
        try {
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO Attendance (student_id, class_id, date, status) VALUES (?, ?, ?, ?)");
            stmt.setString(1, studentId);
            stmt.setString(2, classId);
            stmt.setDate(3, java.sql.Date.valueOf(date));
            stmt.setString(4, status);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateAttendance(String studentId, String classId, LocalDate date, String status) {
        try {
            PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE Attendance SET status = ? WHERE student_id = ? AND class_id = ? AND date = ?");
            stmt.setString(1, status);
            stmt.setString(2, studentId);
            stmt.setString(3, classId);
            stmt.setDate(4, java.sql.Date.valueOf(date));
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<AttendanceRecord> getAttendanceRecords() {
        List<AttendanceRecord> records = new ArrayList<>();
        try {
            String sql = "SELECT a.student_id, s.name, a.class_id, a.date, a.status " +
                         "FROM Attendance a JOIN Students s ON a.student_id = s.student_id";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String studentId = rs.getString("student_id");
                String name = rs.getString("name");
                String classId = rs.getString("class_id");
                String date = rs.getDate("date").toString();
                String status = rs.getString("status");
                records.add(new AttendanceRecord(studentId, name, classId, date, status));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return records;
    }

    private byte[] serializeVector(INDArray vector) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            Nd4j.write(vector, dos);
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    private INDArray deserializeVector(byte[] bytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(bais)) {
            return Nd4j.read(dis);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
