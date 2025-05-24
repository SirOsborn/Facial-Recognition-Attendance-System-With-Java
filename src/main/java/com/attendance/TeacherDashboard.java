package com.attendance;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class TeacherDashboard extends Application {
    private DatabaseManager dbManager;

    public TeacherDashboard() {
        this.dbManager = new DatabaseManager();
    }

    @Override
    public void start(Stage primaryStage) {
        TableView<AttendanceRecord> table = new TableView<>();
        ObservableList<AttendanceRecord> data = FXCollections.observableArrayList(dbManager.getAttendanceRecords());

        TableColumn<AttendanceRecord, String> studentIdCol = new TableColumn<>("Student ID");
        studentIdCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));

        TableColumn<AttendanceRecord, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<AttendanceRecord, String> classIdCol = new TableColumn<>("Class ID");
        classIdCol.setCellValueFactory(new PropertyValueFactory<>("classId"));

        TableColumn<AttendanceRecord, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));

        TableColumn<AttendanceRecord, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(studentIdCol, nameCol, classIdCol, dateCol, statusCol);
        table.setItems(data);

        VBox vbox = new VBox(table);
        Scene scene = new Scene(vbox, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Teacher Attendance Dashboard");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}