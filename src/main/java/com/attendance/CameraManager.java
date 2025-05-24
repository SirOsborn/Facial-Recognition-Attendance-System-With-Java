package com.attendance;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_highgui.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class CameraManager {
    private VideoCapture capture;
    private boolean isWindowOpen = false;

    public CameraManager() {
        capture = new VideoCapture(0); // Default webcam
        if (!capture.isOpened()) {
            System.err.println("Error: Could not open camera");
        }
    }    public Mat captureFrame() {
        Mat frame = new Mat();
        if (capture.isOpened()) {
            capture.read(frame);
        }
        return frame;
    }

    public void stopCamera() {
        if (isWindowOpen) {
            destroyWindow("Camera Preview");
        }
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
    }
}
