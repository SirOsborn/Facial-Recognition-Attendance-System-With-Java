CREATE DATABASE attendance_system;
USE attendance_system;

CREATE TABLE Students (
                          student_id VARCHAR(50) PRIMARY KEY,
                          name VARCHAR(100) NOT NULL,
                          face_vector BLOB NOT NULL
);

CREATE TABLE Classes (
                         class_id VARCHAR(50) PRIMARY KEY,
                         class_name VARCHAR(100) NOT NULL,
                         teacher_id VARCHAR(50) NOT NULL
);

CREATE TABLE Attendance (
                            attendance_id INT AUTO_INCREMENT PRIMARY KEY,
                            student_id VARCHAR(50),
                            class_id VARCHAR(50),
                            date DATE,
                            status ENUM('pending', 'confirmed', 'absent') DEFAULT 'pending',
                            FOREIGN KEY (student_id) REFERENCES Students(student_id),
                            FOREIGN KEY (class_id) REFERENCES Classes(class_id)
);