package com.attendance;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Modality;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.Java2DFrameConverter;
import javafx.embed.swing.SwingFXUtils;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

public class RegisterStudentDialog {
    private final Stage dialog;
    private final ImageView imageView;
    private final CameraManager cameraManager;
    private final AtomicBoolean running;
    private Mat capturedFrame;
    private boolean success = false;

    public RegisterStudentDialog(Stage parentStage, String studentId, String name) {
        dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Register Student: " + name);

        cameraManager = new CameraManager();
        imageView = new ImageView();
        imageView.setFitWidth(640);
        imageView.setFitHeight(480);
        running = new AtomicBoolean(true);

        Button captureButton = new Button("Capture (Space)");
        Button cancelButton = new Button("Cancel (Esc)");
        Label instructionLabel = new Label("Please look at the camera and press Capture when ready");

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(captureButton, cancelButton);

        VBox root = new VBox(10);
        root.getChildren().addAll(instructionLabel, imageView, buttonBox);

        Scene scene = new Scene(root);
        dialog.setScene(scene);

        // Handle window close
        dialog.setOnCloseRequest(e -> {
            running.set(false);
            cameraManager.stopCamera();
        });

        // Handle buttons
        captureButton.setOnAction(e -> {
            capturedFrame = cameraManager.captureFrame();
            if (capturedFrame != null && !capturedFrame.empty()) {
                success = true;
                running.set(false);
                dialog.close();
            }
        });

        cancelButton.setOnAction(e -> {
            success = false;
            running.set(false);
            dialog.close();
        });

        // Start camera feed
        startCameraFeed();
    }

    private void startCameraFeed() {
        Thread cameraThread = new Thread(() -> {
            OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
            Java2DFrameConverter converterToAWT = new Java2DFrameConverter();

            while (running.get()) {
                Mat frame = cameraManager.captureFrame();
                if (frame != null && !frame.empty()) {
                    BufferedImage bufferedImage = converterToAWT.convert(converterToMat.convert(frame));
                    if (bufferedImage != null) {
                        Platform.runLater(() -> {
                            Image image = SwingFXUtils.toFXImage(bufferedImage, null);
                            imageView.setImage(image);
                        });
                    }
                }
                try {
                    Thread.sleep(33); // ~30 fps
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    public Mat showAndWait() {
        dialog.showAndWait();
        return success ? capturedFrame : null;
    }
}
