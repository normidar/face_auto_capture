package com.example.face_auto_capture;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;
import android.os.Handler;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;


import com.google.common.util.concurrent.ListenableFuture;

import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

/**
 * CameraManager
 */
public class CameraManager {
    /**
     * Activity
     */
    private Activity activity;

    /**
     * Variable to hold Handler object for Image Capture
     */
    private Handler imageCaptureHandler;

    private boolean isBindingCamera = false;

    /**
     * ImageAnalysis is to get faceID
     */
    private ImageAnalysis imageAnalysis;

    PreviewView previewView;
    ListenableFuture<ProcessCameraProvider> providerListenableFuture;


    public CameraManager(Activity activity, PreviewView processProvider,
                         AnalyzerCallback analyzerCallback) {
        this.activity = activity;
        this.previewView = processProvider;
        this.mAnalyzerCallback = analyzerCallback;
    }

    void toBind(){
        providerListenableFuture = ProcessCameraProvider.getInstance(activity);
        providerListenableFuture.addListener(new Runnable() {
            @Override
            public void run() {
                bind();
            }
        }, ContextCompat.getMainExecutor(activity));
    }
    
    private void bind() {
        // bind to camera
        try {
            // select camera
            CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
            // faceID用インスタンス
            imageAnalysis = new ImageAnalysis.Builder()
                    .setTargetResolution(new Size(480, 360)) // ID獲得ため最小限のサイズ
                    .build();
            imageAnalysis.setAnalyzer(
                    new Executor() {
                        @Override
                        public void execute(Runnable runnable) {
                            runnable.run();
                        }
                    },
                    analyzer
            );
            // Set up the view finder use case to display camera preview
            Preview preview = new Preview.Builder().build();
            // bind
            ProcessCameraProvider cameraBinder = providerListenableFuture.get();
            cameraBinder.unbindAll();
            cameraBinder.bindToLifecycle(
                    (LifecycleOwner) activity, cameraSelector, imageAnalysis,preview
            );

            preview.setSurfaceProvider(previewView.getSurfaceProvider());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * analyze the image to get face ID
     * 画像を分析して顔のIDを獲得
     */
    ImageAnalysis.Analyzer analyzer = new ImageAnalysis.Analyzer() {
        @SuppressLint({"UnsafeExperimentalUsageError", "UnsafeOptInUsageError"})
        @Override
        public void analyze(@NonNull ImageProxy image) {
            if (mAnalyzerCallback != null) {
                mAnalyzerCallback.result(image.getImage(), image.getImageInfo().getRotationDegrees(), image);
            }
        }
    };

    /**
     * Variable to hold object of FaceAuthenticationCallback
     */
    private CameraCallback mCameraCallback;

    public interface CameraCallback {
        /**
         * Return the result of Camera Capture
         *
         * @param bitmapImage Bitmap
         */
        void result(Bitmap bitmapImage);
        void onError();
    }

    private AnalyzerCallback mAnalyzerCallback;

    public interface AnalyzerCallback {
        /**
         * return the image to analyzer
         *
         * @param closeable return the ImageProxy to close it after compute
         */
        void result(Image image, int rotationDegrees, AutoCloseable closeable);
    }

    /**
     * Capture Image
     * この関数が呼ばれる時にBitmapを作って戻しましょう
     * authIntervalは毎回の間隔
     */
    public void startCapture(final long authInterval) {
        // ここもスキップの時も呼ばれる
        if (!isBindingCamera) {
            // runloop
            imageCaptureHandler = new Handler();
            imageCaptureHandler.removeCallbacksAndMessages(null);
        }
        runCaptureLoop(authInterval);
    }

    /**
     * run capture loop
     */
    private void runCaptureLoop(final long authInterval) {
        imageCaptureHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
//                takePicture();
            }
        }, authInterval);
    }
    /**
     * 画像を回転させる
     *
     * @param bitmap Original Bitmap
     * @param degree Orientation
     * @return rotated Image
     */
    private Bitmap getRotatedImage(Bitmap bitmap, int degree) {
        Matrix matrix = new Matrix();
        matrix.preScale(-1, 1); // 写真の左右を反転させる
        matrix.preRotate(degree); // 写真を回転させる
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix,
                true);
    }
}
