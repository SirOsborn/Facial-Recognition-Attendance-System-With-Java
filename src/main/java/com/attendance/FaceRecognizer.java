package com.attendance;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.*;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;

public class FaceRecognizer {
    private static final Logger logger = LoggerFactory.getLogger(FaceRecognizer.class);
    private CascadeClassifier faceDetector;
    private static final int FACE_WIDTH = 96;
    private static final int FACE_HEIGHT = 96;
    
    static {
        try {            // Initialize ND4J backend
            org.nd4j.linalg.factory.Nd4jBackend.load();
            org.nd4j.linalg.factory.Nd4j.getExecutioner().enableVerboseMode(false);
            logger.info("ND4J backend initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize ND4J backend: {}", e.getMessage(), e);
        }
    }
    
    public FaceRecognizer() {
        faceDetector = new CascadeClassifier();
        try {
            // First try loading from resources
            java.net.URL url = getClass().getResource("/haarcascade_frontalface_default.xml");
            if (url == null) {
                logger.error("Could not find haarcascade_frontalface_default.xml in resources");
                return;
            }
            
            // Create a temporary file
            java.io.File temp = java.io.File.createTempFile("cascade", ".xml");
            temp.deleteOnExit();
            
            // Copy from jar to temp file
            java.nio.file.Files.copy(
                url.openStream(), 
                temp.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            
            // Load from temp file
            if (!faceDetector.load(temp.getAbsolutePath())) {
                System.err.println("Error: Could not load cascade classifier");
                throw new RuntimeException("Failed to load cascade classifier");
            }
            
            System.out.println("Successfully loaded cascade classifier");
        } catch (Exception e) {
            System.err.println("Error loading cascade file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public INDArray generateFaceVector(Mat frame) {
        // Convert the frame to grayscale
        Mat gray = new Mat();
        cvtColor(frame, gray, COLOR_BGR2GRAY);
        
        // Detect faces
        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(gray, faces);
        
        // If no face is detected or multiple faces are detected, return null
        if (faces.empty() || faces.size() > 1) {
            return null;
        }
        
        // Get the detected face
        Rect face = faces.get(0);
        
        // Extract and preprocess the face region
        Mat faceRegion = new Mat(gray, face);
        Mat resizedFace = new Mat();
        resize(faceRegion, resizedFace, new Size(FACE_WIDTH, FACE_HEIGHT));
        
        // Convert to float values between 0 and 1
        Mat floatFace = new Mat();
        resizedFace.convertTo(floatFace, CV_32F);
        FloatIndexer indexer = floatFace.createIndexer();
        
        // Convert to INDArray (128-dimensional vector)
        float[] pixels = new float[FACE_WIDTH * FACE_HEIGHT];
        for (int i = 0; i < FACE_HEIGHT; i++) {
            for (int j = 0; j < FACE_WIDTH; j++) {
                pixels[i * FACE_WIDTH + j] = indexer.get(i, j) / 255.0f;
            }
        }
        indexer.release();
        
        // Create a 128-dimensional feature vector (simplified for testing)
        INDArray vector = Nd4j.create(128);
        for (int i = 0; i < 128; i++) {
            vector.putScalar(i, pixels[i % pixels.length]);
        }
        
        return vector;
    }

    public double calculateSimilarity(INDArray vector1, INDArray vector2) {
        // Cosine similarity implementation
        if (vector1 == null || vector2 == null) return 0.0;
        double dot = Nd4j.getBlasWrapper().dot(vector1, vector2);
        double norm1 = vector1.norm2Number().doubleValue();
        double norm2 = vector2.norm2Number().doubleValue();
        if (norm1 == 0 || norm2 == 0) return 0.0;
        return dot / (norm1 * norm2);
    }
}
