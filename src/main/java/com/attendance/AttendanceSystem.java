package com.attendance;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.nd4j.linalg.api.ndarray.INDArray;
import javafx.embed.swing.SwingFXUtils;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttendanceSystem extends Application {
    private static final Logger logger = LoggerFactory.getLogger(AttendanceSystem.class);
    private CameraManager cameraManager;
    private FaceRecognizer faceRecognizer;
    private DatabaseManager dbManager;
    private ImageView cameraView;
    private ScheduledExecutorService cameraTimer;
    private TextField studentIdField;
    private TextField nameField;
    private TextField classIdField;
    private Label statusLabel;    private OpenCVFrameConverter.ToMat matConverter;
    private Java2DFrameConverter converter;

    public AttendanceSystem() {
        matConverter = new OpenCVFrameConverter.ToMat();
        converter = new Java2DFrameConverter();
    }

    @Override
    public void start(Stage primaryStage) {
        this.cameraManager = new CameraManager();
        this.faceRecognizer = new FaceRecognizer();
        this.dbManager = new DatabaseManager();

        primaryStage.setTitle("Face Recognition Attendance System");

        // Create the main layout
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));

        // Create camera preview
        cameraView = new ImageView();
        cameraView.setFitWidth(640);
        cameraView.setFitHeight(480);
        cameraView.setPreserveRatio(true);

        // Create input fields
        VBox inputFields = createInputFields();
        
        // Create buttons
        HBox buttonBox = createButtons();

        // Status label
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-font-size: 14px;");

        // Layout assembly
        VBox rightPanel = new VBox(10);
        rightPanel.getChildren().addAll(inputFields, buttonBox, statusLabel);
        rightPanel.setPadding(new Insets(10));
        rightPanel.setAlignment(Pos.TOP_CENTER);

        mainLayout.setCenter(cameraView);
        mainLayout.setRight(rightPanel);

        Scene scene = new Scene(mainLayout);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start camera feed
        startCameraFeed();

        primaryStage.setOnCloseRequest(e -> {
            stopCameraFeed();
            Platform.exit();
            System.exit(0);
        });
    }

    private VBox createInputFields() {
        VBox inputBox = new VBox(10);
        inputBox.setAlignment(Pos.TOP_LEFT);
        inputBox.setPrefWidth(300);

        studentIdField = new TextField();
        studentIdField.setPromptText("Student ID");

        nameField = new TextField();
        nameField.setPromptText("Student Name");

        classIdField = new TextField();
        classIdField.setPromptText("Class ID");

        inputBox.getChildren().addAll(
            new Label("Student ID:"), studentIdField,
            new Label("Student Name:"), nameField,
            new Label("Class ID:"), classIdField
        );

        return inputBox;
    }

    private HBox createButtons() {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button registerButton = new Button("Register Student");
        Button markEntryButton = new Button("Mark Entry");
        Button markExitButton = new Button("Mark Exit");

        registerButton.setOnAction(e -> registerStudent());
        markEntryButton.setOnAction(e -> markEntry());
        markExitButton.setOnAction(e -> markExit());

        buttonBox.getChildren().addAll(registerButton, markEntryButton, markExitButton);
        return buttonBox;
    }

    private void startCameraFeed() {
        cameraTimer = Executors.newSingleThreadScheduledExecutor();
        cameraTimer.scheduleAtFixedRate(() -> {
            Mat frame = cameraManager.captureFrame();
            if (frame != null && !frame.empty()) {
                updateCameraView(frame);
            }
        }, 0, 33, TimeUnit.MILLISECONDS);
    }

    private void stopCameraFeed() {
        if (cameraTimer != null) {
            cameraTimer.shutdown();
            try {
                cameraTimer.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("Error stopping camera feed", e);
            }
        }
        cameraManager.stopCamera();
    }    private void updateCameraView(Mat frame) {
        try {
            Frame javaFrame = matConverter.convert(frame);
            BufferedImage image = converter.getBufferedImage(javaFrame);
            Platform.runLater(() -> cameraView.setImage(SwingFXUtils.toFXImage(image, null)));
        } catch (Exception e) {
            logger.error("Error updating camera view", e);
        }
    }

    private void registerStudent() {
        String studentId = studentIdField.getText().trim();
        String name = nameField.getText().trim();

        if (studentId.isEmpty() || name.isEmpty()) {
            updateStatus("Please enter both Student ID and Name");
            return;
        }

        Mat frame = cameraManager.captureFrame();
        if (frame != null) {
            INDArray faceVector = faceRecognizer.generateFaceVector(frame);
            if (faceVector != null) {
                if (dbManager.saveStudent(studentId, name, faceVector)) {
                    updateStatus("Student registered: " + name + " (" + studentId + ")");
                    clearFields();
                } else {
                    updateStatus("Error registering student. ID may already exist.");
                }
            } else {
                updateStatus("No face detected. Please try again.");
            }
        }
    }

    private void markEntry() {
        String studentId = studentIdField.getText().trim();
        String classId = classIdField.getText().trim();

        if (studentId.isEmpty() || classId.isEmpty()) {
            updateStatus("Please enter both Student ID and Class ID");
            return;
        }

        Mat frame = cameraManager.captureFrame();
        INDArray faceVector = faceRecognizer.generateFaceVector(frame);
        if (faceVector != null) {
            String matchedId = dbManager.matchFaceVector(faceVector, 0.8);
            if (matchedId != null && matchedId.equals(studentId)) {
                dbManager.saveAttendance(studentId, classId, LocalDate.now(), "pending");
                updateStatus("Entry marked for student: " + studentId);
                clearFields();
            } else {
                updateStatus("Face not recognized or mismatch.");
            }
        }
    }

    private void markExit() {
        String studentId = studentIdField.getText().trim();
        String classId = classIdField.getText().trim();

        if (studentId.isEmpty() || classId.isEmpty()) {
            updateStatus("Please enter both Student ID and Class ID");
            return;
        }

        Mat frame = cameraManager.captureFrame();
        INDArray faceVector = faceRecognizer.generateFaceVector(frame);
        if (faceVector != null) {
            String matchedId = dbManager.matchFaceVector(faceVector, 0.8);
            if (matchedId != null && matchedId.equals(studentId)) {
                dbManager.updateAttendance(studentId, classId, LocalDate.now(), "confirmed");
                updateStatus("Attendance confirmed for student: " + studentId);
                clearFields();
            } else {
                dbManager.updateAttendance(studentId, classId, LocalDate.now(), "absent");
                updateStatus("Attendance marked absent for student: " + studentId);
            }
        }
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    private void clearFields() {
        Platform.runLater(() -> {
            studentIdField.clear();
            nameField.clear();
            classIdField.clear();
        });
    }

    @Override
    public void stop() {
        stopCameraFeed();
        if (converter != null) {
            converter.close();
        }
        if (matConverter != null) {
            matConverter.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}