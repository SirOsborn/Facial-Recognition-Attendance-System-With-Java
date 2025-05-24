package com.attendance;

public class AttendanceRecord {
    private String studentId;
    private String name;
    private String classId;
    private String date;
    private String status;

    public AttendanceRecord(String studentId, String name, String classId, String date, String status) {
        this.studentId = studentId;
        this.name = name;
        this.classId = classId;
        this.date = date;
        this.status = status;
    }

    // Getters and setters
    public String getStudentId() { return studentId; }
    public String getName() { return name; }
    public String getClassId() { return classId; }
    public String getDate() { return date; }
    public String getStatus() { return status; }
}
