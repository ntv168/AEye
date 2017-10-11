package com.example.sam.aeye.camera;

/**
 * Created by Sam on 6/20/2017.
 */

public class CameraConfig {
    private static volatile CameraConfig singletonCamera = null;
    private byte[] faceImage;
    private int faceDetected;

    private CameraConfig() {}

    public static CameraConfig getInstance() {
        if (singletonCamera == null) {
            synchronized (CameraConfig.class){
                if (singletonCamera == null) {
                    singletonCamera = new CameraConfig();

                }
            }
        }
        return singletonCamera;
    }


    public void setFaceImage(byte[] faceImage) {
        this.faceImage = faceImage;
    }
    public byte[] getFaceImage() {
        return faceImage;
    }
    public int getFaceDetected() {
        return faceDetected;
    }
    public void setFaceDetected(int faceDetected) {
        this.faceDetected = faceDetected;
    }
}
