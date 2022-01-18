package com.example.face_auto_capture;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.lang.reflect.Array;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    CameraManager cameraManager;
    FaceDetectorOptions faceDetectorOptions = new FaceDetectorOptions.Builder()
            // 性能
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
    // 特徴を取るかどうか
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
    // 顔の外形
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
    // 顔状態を取るかどうか
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
    // 顔のどれほど小さいまでとるか
                .setMinFaceSize(0.3f)
    // 顔の追跡をオン、トラッキング
                .enableTracking()
                .build();


     long waitCount = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FaceDetector detector = FaceDetection.getClient(faceDetectorOptions);

        // 権限確認
        boolean isPermitted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if(!isPermitted){
            ActivityCompat.requestPermissions(this, new String[]{(Manifest.permission.CAMERA)},10);
        }

        PreviewView preview = (PreviewView) findViewById(R.id.previewView);

        cameraManager = new CameraManager(this, preview,
                (image, rotationDegrees, closeable) -> {
                    if(image != null) {
                        InputImage inputImage = InputImage.fromMediaImage(image,rotationDegrees);
                        detector.process(inputImage)
                                .addOnSuccessListener(
                                        (faces) ->{
                                            waitCount++;
                                            if(faces.size() > 0 && waitCount > 250){
                                                Face face = faces.get(0);
                                                float angularX = face.getHeadEulerAngleX();
                                                float angularY = face.getHeadEulerAngleY();
                                                checkAngular(angularX, angularY);
                                            }
                                            try {
                                                closeable.close();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                );
                    }
                });
        cameraManager.toBind();
    }
    final int ANGULAR_THRESHOLD = 10;

    enum Step{
        FRONT, UP, RIGHT, DOWN, LEFT, END
    }
    Step nowStep = Step.FRONT;
    void checkAngular(float angularX,float angularY){
        Log.d("ang","angX:" + angularX);
        Log.d("ang","angY:" + angularY);
        switch(nowStep) {
            case FRONT:
                if(Math.abs(angularX) <= ANGULAR_THRESHOLD && Math.abs(angularY) <= ANGULAR_THRESHOLD){
                    // 振動
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(30);
                    nowStep = Step.UP;
                    resetNotice();
                }
                break;
            case UP:
                if(angularX > ANGULAR_THRESHOLD && angularX < ANGULAR_THRESHOLD*2
                        && Math.abs(angularY) <= ANGULAR_THRESHOLD){
                    // 振動
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(30);
                    nowStep = Step.RIGHT;
                    resetNotice();
                }
                break;
            case RIGHT:
                if(Math.abs(angularX) <= ANGULAR_THRESHOLD  && angularY < -ANGULAR_THRESHOLD
                        && angularY > -ANGULAR_THRESHOLD*2){
                    // 振動
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(30);
                    nowStep = Step.DOWN;
                    resetNotice();
                }
                break;
            case DOWN:
                if(angularX < -ANGULAR_THRESHOLD && angularX > -ANGULAR_THRESHOLD*2
                        && Math.abs(angularY) <= ANGULAR_THRESHOLD){
                    // 振動
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(30);
                    nowStep = Step.LEFT;
                    resetNotice();
                }
                break;
            case LEFT:
                if(Math.abs(angularX) <= ANGULAR_THRESHOLD  && angularY > ANGULAR_THRESHOLD
                        && angularY < ANGULAR_THRESHOLD*2){
                    // 振動
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(30);
                    nowStep = Step.END;
                    resetNotice();
                }
                break;
        }
    }

    void resetNotice(){
        TextView textView = (TextView) findViewById(R.id.hint);
        switch(nowStep){
            case FRONT:
                textView.setText("顔を正面に向けてください");
                break;
            case UP:
                textView.setText("少し上に向けてください");
                break;
            case RIGHT:
                textView.setText("少し右に向けてください");
                break;
            case DOWN:
                textView.setText("少し下に向けてください");
                break;
            case LEFT:
                textView.setText("少し左に向けてください");
                break;
            case END:
                textView.setText("これで撮影終了します");
                break;
        }
    }

}